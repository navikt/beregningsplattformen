package no.nav.beregning.task

import no.nav.beregning.task.cron.CronExpression
import java.time.Duration

/**
 * Hva en task gjør når den kjøres. Mottar payload via [TaskInput]. Motor: `JobbUtfører`.
 */
fun interface Task {
    fun utfor(input: TaskInput)
}

/**
 * Deklarasjonen av en task-type: hvordan den konstrueres og dens policy for retry/prioritet/cron.
 * Motor: `JobbSpesifikasjon`.
 */
interface TaskType {
    val type: String
    val navn: String
    val beskrivelse: String

    /** Antall ganger tasken prøves før den settes til FEILET permanent. */
    val retries: Int get() = 3

    /** Standardprioritet for typen. Kan overstyres per [TaskInput]. */
    val prioritet: Int get() = Prioritet.NORMAL

    /** Backoff mellom forsøk ved feil. */
    val retryBackoff: Duration get() = Duration.ofMinutes(5)

    /** Ved fullføring reskedulerer tasken seg selv etter dette mønsteret. */
    val cron: CronExpression? get() = null

    fun konstruer(): Task
}
