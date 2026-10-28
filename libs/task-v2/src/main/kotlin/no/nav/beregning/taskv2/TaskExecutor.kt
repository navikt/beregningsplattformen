package no.nav.beregning.taskv2

import mu.KotlinLogging
import no.nav.beregning.taskv2.domene.MDC_CALL_ID
import no.nav.beregning.taskv2.domene.Status
import no.nav.beregning.taskv2.domene.Task
import no.nav.beregning.taskv2.error.RekjorSenereException
import no.nav.beregning.taskv2.trace.TaskTracer
import org.slf4j.MDC
import java.io.Closeable
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

private val log = KotlinLogging.logger {}

/**
 * 🔴 RØD SONE — polling, optimistisk plukking og utførelse. Portet fra prosesserings
 * TaskStepExecutorService + TaskWorker.
 *
 * Flyt per runde: hent en side klare tasks → forsøk plukk med optimistisk lås (tap = hopp videre)
 * → send plukkede til worker-pool → doActualWork kjører steget og ferdigstiller.
 *
 * Vedlikehold kjøres kun på leder (Nais leader-elector).
 */
class TaskExecutor(
    private val taskService: TaskService,
    private val register: TaskStegRegister,
    private val maintenance: TaskMaintenance,
    private val leaderElection: LeaderElection,
    private val maxAntallPerRunde: Int = 10,
    private val antallWorkere: Int = 4,
    private val pollInterval: Duration = Duration.ofSeconds(1),
    private val tomKoBackoff: Duration = Duration.ofSeconds(5),
    private val vedlikeholdInterval: Duration = Duration.ofMinutes(5),
) : Closeable {

    private val workers =
        ThreadPoolExecutor(
            antallWorkere, antallWorkere, 0L, TimeUnit.MILLISECONDS,
            LinkedBlockingQueue(maxAntallPerRunde),
            ThreadPoolExecutor.CallerRunsPolicy(),
        )
    private val scheduler = Executors.newScheduledThreadPool(2)

    @Volatile
    private var kjorer = false

    fun start() {
        kjorer = true
        Thread({ pollLoop() }, "taskv2-poller").apply { isDaemon = true }.start()
        scheduler.scheduleWithFixedDelay(
            ::kjorVedlikehold, vedlikeholdInterval.toMillis(), vedlikeholdInterval.toMillis(), TimeUnit.MILLISECONDS,
        )
        log.info { "TaskExecutor startet: $antallWorkere workere" }
    }

    private fun pollLoop() {
        while (kjorer) {
            val antall =
                try {
                    plukkOgKjorRunde()
                } catch (e: Exception) {
                    log.error(e) { "Uventet feil i plukkeløkken" }
                    0
                }
            Thread.sleep((if (antall > 0) pollInterval else tomKoBackoff).toMillis())
        }
    }

    /**
     * Hent en side klare tasks og forsøk å plukke hver med optimistisk lås. Taper vi kappløpet mot
     * en annen pod, kaster plukk OptimistiskLasException og vi hopper videre. Returnerer antall vi
     * faktisk plukket.
     */
    private fun plukkOgKjorRunde(): Int {
        val klare = taskService.finnKlareForProsessering(maxAntallPerRunde)
        var plukket = 0
        for (task in klare) {
            val vart =
                try {
                    taskService.plukk(task)
                } catch (e: OptimistiskLasException) {
                    continue // en annen pod tok den
                }
            plukket++
            workers.execute { doActualWork(vart) }
        }
        return plukket
    }

    /**
     * Utfør én plukket task. Setter BEHANDLER, kjører steget, ferdigstiller. RekjorSenereException
     * utsetter uten ordinært feilforsøk; andre exceptions registreres som feil med backoff.
     *
     * At-least-once: en krasj etter at steget har gjort en ekstern sideeffekt, men før FERDIG, gir
     * ny kjøring. doTask MÅ være idempotent.
     */
    private fun doActualWork(task: Task) {
        MDC.put(MDC_CALL_ID, task.callId)
        try {
            val behandles =
                try {
                    taskService.behandle(task)
                } catch (e: OptimistiskLasException) {
                    return // en annen pod overtok
                }

            val steg = register.finnSteg(task.type)
            TaskTracer.span(behandles) { steg.doTask(behandles) }
            taskService.ferdigstill(behandles)
            log.info { "Task ${task.id} (${task.type}) ferdig" }
        } catch (e: RekjorSenereException) {
            rekjorSenere(task, e)
        } catch (e: Exception) {
            registrerFeil(task, e)
        } finally {
            MDC.remove(MDC_CALL_ID)
        }
    }

    private fun rekjorSenere(task: Task, e: RekjorSenereException) {
        log.info { "Rekjører task=${task.id} senere, triggerTid=${e.triggerTid}" }
        try {
            val gjeldende = taskService.finnById(task.id)
            taskService.klarTilPlukk(gjeldende, melding = e.arsak, nyTriggerTid = e.triggerTid)
        } catch (ex: OptimistiskLasException) {
            log.debug { "Kunne ikke sette rekjør-senere pga. samtidig endring: ${ex.message}" }
        }
    }

    private fun registrerFeil(task: Task, e: Exception) {
        val beskrivelse = register.finnBeskrivelse(task.type)
        val tidligereAntallFeil = taskService.antallGangerPlukket(task.id)
        // Logg aldri payload. Kun exception-type og melding.
        val feilmelding = "${e.javaClass.simpleName}: ${e.message}"
        val nyTriggerTid = LocalDateTime.now().plusSeconds(beskrivelse.triggerTidVedFeilISekunder)
        try {
            val gjeldende = taskService.finnById(task.id)
            taskService.feilet(
                task = gjeldende,
                feilmelding = feilmelding,
                tidligereAntallFeil = tidligereAntallFeil,
                maxAntallFeil = beskrivelse.maxAntallFeil,
                settTilManuellOppfolgning = beskrivelse.settTilManuellOppfolgning,
                nyTriggerTid = nyTriggerTid,
            )
            log.warn { "Task ${task.id} (${task.type}) feilet, forsøk $tidligereAntallFeil registrert" }
        } catch (ex: OptimistiskLasException) {
            log.debug { "Kunne ikke registrere feil pga. samtidig endring: ${ex.message}" }
        }
    }

    private fun kjorVedlikehold() {
        if (!leaderElection.erLeder()) return
        try {
            maintenance.retryFeilende()
            maintenance.settPermanentPlukketTilKlar()
            maintenance.slettFerdige()
        } catch (e: Exception) {
            log.error(e) { "Vedlikehold feilet" }
        }
    }

    fun stop(timeout: Duration = Duration.ofSeconds(10)) {
        kjorer = false
        workers.shutdown()
        scheduler.shutdown()
        if (!workers.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS)) workers.shutdownNow()
        scheduler.shutdownNow()
        log.info { "TaskExecutor stoppet" }
    }

    override fun close() = stop()
}
