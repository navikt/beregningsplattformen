package no.nav.beregning.task

import mu.KotlinLogging
import no.nav.beregning.task.arkiv.ArkiverFerdige
import no.nav.beregning.task.mdc.NoExtraLogInfoProvider
import no.nav.beregning.task.mdc.TaskLogInfoProvider
import no.nav.beregning.task.trace.TaskTracer
import org.jdbi.v3.core.Jdbi
import org.slf4j.MDC
import java.io.Closeable
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.sql.DataSource
import kotlin.concurrent.thread

private val log = KotlinLogging.logger {}

/**
 * 🔴 RØD SONE — hjertet i utbox-garantien og krasj-semantikken. Les sjekklisten i leveransen.
 *
 * Batch-plukkende motor: én poller henter [batchStorrelse] modne tasks i én kort transaksjon,
 * committer plukkingen (status PLUKKET), og legger dem på en intern kø. En worker-pool med
 * [antallWorkere] tråder utfører hver task i SIN EGEN transaksjon (FERDIG eller FEILET).
 *
 * Fordi plukkingen committes, holdes ingen DB-lås under utførelsen — samtidighet er frikoblet fra
 * antall DB-tilkoblinger. Prisen er at Postgres ikke lenger frigjør en task automatisk ved krasj:
 * en [reaper] kjører periodisk og setter tasks som har stått PLUKKET lenger enn [reaperTimeout]
 * tilbake til KLAR.
 *
 * Dimensjonering: pollingen bruker 1 tilkobling kort, hver worker 1 tilkobling mens den kjører,
 * reaperen 1 kort. Pool bør være >= antallWorkere + 1.
 */
class TaskRunner(
    dataSource: DataSource,
    private val registry: TaskRegistry,
    private val antallWorkere: Int = 8,
    private val batchStorrelse: Int = 20,
    private val pollInterval: Duration = Duration.ofSeconds(2),
    private val tomKoBackoff: Duration = Duration.ofSeconds(10),
    private val reaperTimeout: Duration = Duration.ofMinutes(30),
    private val reaperInterval: Duration = Duration.ofMinutes(5),
    private val logInfoProvider: TaskLogInfoProvider = NoExtraLogInfoProvider,
    private val arkiverEldreEnn: Duration = Duration.ofDays(7),
    private val arkivInterval: Duration = Duration.ofHours(1),
) : Closeable {

    private val jdbi: Jdbi = Jdbi.create(dataSource)
    private val arkiverer = ArkiverFerdige(jdbi, arkiverEldreEnn)

    // Begrenset kø med CallerRunsPolicy: fylles køen opp, bremses polleren naturlig (backpressure),
    // så vi aldri plukker flere tasks enn workerne rekker å kjøre.
    private val workers =
        ThreadPoolExecutor(
            antallWorkere, antallWorkere, 0L, TimeUnit.MILLISECONDS,
            LinkedBlockingQueue(batchStorrelse),
            ThreadPoolExecutor.CallerRunsPolicy(),
        )
    private val reaperExecutor = Executors.newScheduledThreadPool(2)

    @Volatile
    private var kjorer = false

    fun start() {
        kjorer = true
        thread(name = "task-poller", isDaemon = true) { pollLoop() }
        reaperExecutor.scheduleWithFixedDelay(
            ::kjorReaper, reaperInterval.toMillis(), reaperInterval.toMillis(), TimeUnit.MILLISECONDS,
        )
        reaperExecutor.scheduleWithFixedDelay(
            ::kjorArkivering, arkivInterval.toMillis(), arkivInterval.toMillis(), TimeUnit.MILLISECONDS,
        )
        log.info { "TaskRunner startet: $antallWorkere workere, batch=$batchStorrelse" }
    }

    private fun pollLoop() {
        while (kjorer) {
            val antallPlukket =
                try {
                    plukkOgKjorBatch()
                } catch (e: Exception) {
                    log.error(e) { "Uventet feil i plukkeløkken" }
                    0
                }
            val ventetid = if (antallPlukket > 0) pollInterval else tomKoBackoff
            Thread.sleep(ventetid.toMillis())
        }
    }

    /**
     * Plukk en batch i én kort transaksjon (committes ved retur fra useTransaction), og send hver
     * task til worker-poolen. Returnerer antall plukket.
     */
    private fun plukkOgKjorBatch(): Int {
        val batch =
            jdbi.inTransaction<List<TaskRecord>, Exception> { handle ->
                TaskRepository(handle).plukkBatch(batchStorrelse)
            }
        batch.forEach { record -> workers.execute { kjorTask(record) } }
        return batch.size
    }

    /**
     * Utfør én plukket task i sin egen transaksjon. Lykkes den: FERDIG. Kaster den: feilen
     * registreres (antall_forsok++, FEILET, backoff) i en egen transaksjon.
     *
     * At-least-once: en krasj etter at utfor() har gjort en ekstern sideeffekt, men før FERDIG
     * er committet, fører til at tasken kjøres på nytt. Dette gjelder uansett plukkestrategi, så
     * Task.utfor MÅ være idempotent. Med reaper er gjenkjøringsvinduet opptil reaperTimeout; uten
     * reaper var det nær umiddelbart. Kravet er det samme, kun deteksjonstiden er lengre.
     */
    private fun kjorTask(record: TaskRecord) {
        val type = registry.parse(record.type)
        val mdcFelter = logInfoProvider.hentInformasjon(record.toInput())?.felterMedVerdi ?: emptyMap()
        mdcFelter.forEach { (k, v) -> MDC.put(k, v) }
        try {
            val task = type.konstruer()
            TaskTracer.span(record) {
                jdbi.useTransaction<Exception> { handle ->
                    task.utfor(record.toInput())
                    val repo = TaskRepository(handle)
                    val cron = type.cron
                    if (cron != null) {
                        // Cron-task: marker ferdig og planlegg neste kjøring av samme type.
                        val neste = cron.nextLocalDateTimeAfter(LocalDateTime.now())
                        repo.reskedulerCron(record, neste)
                    } else {
                        repo.markerFerdig(record.id)
                    }
                }
            }
            log.info { "Task ${record.id} (${record.type}) ferdig" }
        } catch (e: Exception) {
            registrerFeil(record, e)
        } finally {
            mdcFelter.keys.forEach { MDC.remove(it) }
        }
    }

    private fun registrerFeil(record: TaskRecord, e: Exception) {
        val type = registry.parse(record.type)
        // Logg/lagre aldri payload. Kun exception-type og melding.
        val feilmelding = "${e.javaClass.simpleName}: ${e.message}"
        try {
            jdbi.useTransaction<Exception> { handle ->
                TaskRepository(handle).markerFeilet(record.id, type.retries, type.retryBackoff, feilmelding)
            }
            log.warn { "Task ${record.id} (${record.type}) feilet, forsøk registrert" }
        } catch (feilException: Exception) {
            log.error(feilException) { "Klarte ikke registrere feil for task ${record.id}" }
        }
    }

    private fun kjorReaper() {
        try {
            val antall = jdbi.inTransaction<Int, Exception> { TaskRepository(it).resettHengende(reaperTimeout) }
            if (antall > 0) log.warn { "Reaper resatte $antall hengende PLUKKET-tasks" }
        } catch (e: Exception) {
            log.error(e) { "Reaper feilet" }
        }
    }

    private fun kjorArkivering() {
        try {
            arkiverer.arkiver()
        } catch (e: Exception) {
            log.error(e) { "Arkivering feilet" }
        }
    }

    fun stop(timeout: Duration = Duration.ofSeconds(10)) {
        kjorer = false
        workers.shutdown()
        reaperExecutor.shutdown()
        if (!workers.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
            workers.shutdownNow()
        }
        reaperExecutor.shutdownNow()
        log.info { "TaskRunner stoppet" }
    }

    override fun close() = stop()
}
