package no.nav.beregning.taskv2

import no.nav.beregning.taskv2.domene.Loggtype
import no.nav.beregning.taskv2.domene.Status
import no.nav.beregning.taskv2.domene.Task
import no.nav.beregning.taskv2.domene.TaskLogg
import org.jdbi.v3.core.Jdbi
import java.time.LocalDateTime

/**
 * 🔴 RØD SONE — statusmaskinen. Hver overgang oppdaterer task.status med optimistisk versjonssjekk
 * OG skriver en task_logg-rad, i samme transaksjon. Versjonskonflikt bobler opp som
 * [OptimistiskLasException] og håndteres av kalleren (plukk: hopp videre; behandling: avbryt).
 *
 * Portet fra prosesserings TaskService, men transaksjonene er eksplisitte JDBI-blokker i stedet for
 * Spring @Transactional, og versjonssjekken er manuell.
 */
class TaskService(private val jdbi: Jdbi) {

    fun opprett(task: Task): Task =
        jdbi.inTransaction<Task, Exception> { handle ->
            val lagret = TaskRepository(handle).opprett(task)
            TaskLoggRepository(handle).lagre(TaskLogg(taskId = lagret.id, type = Loggtype.UBEHANDLET))
            lagret
        }

    fun finnById(id: Long): Task =
        jdbi.inTransaction<Task, Exception> { TaskRepository(it).finnById(id) }
            ?: error("Task med id=$id ikke funnet")

    fun finnKlareForProsessering(antall: Int): List<Task> =
        jdbi.inTransaction<List<Task>, Exception> { TaskRepository(it).finnKlareForProsessering(antall) }

    fun finnFeilede(): List<Task> =
        jdbi.inTransaction<List<Task>, Exception> { TaskRepository(it).finnMedStatus(Status.FEILET) }

    fun finnPlukketFor(tid: LocalDateTime): List<Task> =
        jdbi.inTransaction<List<Task>, Exception> { TaskRepository(it).finnPlukketFor(tid) }

    /**
     * Forsøk å plukke en task (KLAR→PLUKKET). Kaster [OptimistiskLasException] hvis en annen pod
     * rakk den først; kalleren fanger og hopper videre.
     */
    fun plukk(task: Task): Task =
        overgang(task, Status.PLUKKET, Loggtype.PLUKKET)

    fun behandle(task: Task): Task =
        overgang(task, Status.BEHANDLER, Loggtype.BEHANDLER)

    fun ferdigstill(task: Task): Task =
        overgang(task, Status.FERDIG, Loggtype.FERDIG)

    fun klarTilPlukk(task: Task, melding: String? = null, nyTriggerTid: LocalDateTime? = null): Task =
        overgang(task, Status.KLAR_TIL_PLUKK, Loggtype.KLAR_TIL_PLUKK, melding, nyTriggerTid)

    /**
     * Marker en feilet task. Avgjør ny status ut fra antall tidligere feil og policy: KLAR_TIL_PLUKK
     * for nytt forsøk, ellers FEILET eller MANUELL_OPPFOLGING etter maks.
     */
    fun feilet(
        task: Task,
        feilmelding: String?,
        tidligereAntallFeil: Int,
        maxAntallFeil: Int,
        settTilManuellOppfolgning: Boolean,
        nyTriggerTid: LocalDateTime,
    ): Task {
        val nyStatus =
            when {
                tidligereAntallFeil < maxAntallFeil -> Status.KLAR_TIL_PLUKK
                settTilManuellOppfolgning -> Status.MANUELL_OPPFOLGING
                else -> Status.FEILET
            }
        val loggtype =
            when (nyStatus) {
                Status.KLAR_TIL_PLUKK -> Loggtype.KLAR_TIL_PLUKK
                Status.MANUELL_OPPFOLGING -> Loggtype.MANUELL_OPPFOLGING
                else -> Loggtype.FEILET
            }
        return overgang(task, nyStatus, loggtype, feilmelding, nyTriggerTid)
    }

    fun antallGangerPlukket(taskId: Long): Int =
        jdbi.inTransaction<Int, Exception> { TaskLoggRepository(it).antallAvType(taskId, Loggtype.PLUKKET) }

    private fun overgang(
        task: Task,
        nyStatus: Status,
        loggtype: Loggtype,
        melding: String? = null,
        nyTriggerTid: LocalDateTime? = null,
    ): Task =
        jdbi.inTransaction<Task, Exception> { handle ->
            val oppdatert = TaskRepository(handle).oppdaterMedVersjon(task, nyStatus, nyTriggerTid)
            TaskLoggRepository(handle).lagre(TaskLogg(taskId = task.id, type = loggtype, melding = melding))
            oppdatert
        }
}
