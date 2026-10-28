package no.nav.beregning.taskv2

import mu.KotlinLogging
import no.nav.beregning.taskv2.domene.TaskLogg
import java.time.Duration
import java.time.LocalDateTime

private val log = KotlinLogging.logger {}

/**
 * Vedlikeholdsoppgaver, portet fra prosesserings TaskMaintenanceService. Kjøres kun på leder
 * (se [TaskExecutor]). Prosessering sletter ferdige tasks (arkiverer ikke), så det gjør vi også.
 */
class TaskMaintenance(
    private val taskService: TaskService,
    private val jdbi: org.jdbi.v3.core.Jdbi,
    private val slettEtter: Duration = Duration.ofDays(14),
    private val slettBatch: Int = 10_000,
    private val permanentPlukketGrense: Duration = Duration.ofHours(1),
) {
    /** FEILET → KLAR_TIL_PLUKK, ny trigger_tid nå. */
    fun retryFeilende() {
        val feilede = taskService.finnFeilede()
        log.info { "Rekjører ${feilede.size} feilede tasks" }
        feilede.forEach { tryOvergang { taskService.klarTilPlukk(it, nyTriggerTid = LocalDateTime.now()) } }
    }

    /** PLUKKET for lenge (krasjet pod) → KLAR_TIL_PLUKK. Motstykket til v1s reaper. */
    fun settPermanentPlukketTilKlar() {
        val grense = LocalDateTime.now().minus(permanentPlukketGrense)
        val hengende = taskService.finnPlukketFor(grense)
        if (hengende.isNotEmpty()) log.warn { "Setter ${hengende.size} hengende PLUKKET tilbake til klar" }
        hengende.forEach { tryOvergang { taskService.klarTilPlukk(it) } }
    }

    /** Slett FERDIG eldre enn [slettEtter], inkl. task_logg. */
    fun slettFerdige() {
        val grense = LocalDateTime.now().minus(slettEtter)
        while (true) {
            val antall =
                jdbi.inTransaction<Int, Exception> { handle ->
                    val ids = TaskRepository(handle).finnFerdigeEldreEnn(grense, slettBatch)
                    if (ids.isEmpty()) return@inTransaction 0
                    TaskLoggRepository(handle).slettForTasks(ids)
                    TaskRepository(handle).slett(ids)
                    ids.size
                }
            log.info { "Slettet $antall ferdige tasks eldre enn $grense" }
            if (antall == 0) return
        }
    }

    // Vedlikehold er beste-innsats; en optimistisk konflikt på én task skal ikke stoppe resten.
    private fun tryOvergang(block: () -> Unit) {
        try {
            block()
        } catch (e: OptimistiskLasException) {
            log.debug { "Hoppet over task pga. samtidig endring: ${e.message}" }
        }
    }
}
