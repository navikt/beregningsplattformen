package no.nav.beregning.taskv2

import java.lang.annotation.Inherited

/**
 * Deklarerer en task-type og dens policy. Portet fra prosessering. Leses via refleksjon av
 * [TaskStegRegister] (ingen Spring component scan).
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Inherited
annotation class TaskStepBeskrivelse(
    val taskStepType: String,
    val beskrivelse: String,
    /** Antall forsøk før tasken settes FEILET. */
    val maxAntallFeil: Int = 3,
    /** Hvor lenge man venter ved feil, i sekunder. */
    val triggerTidVedFeilISekunder: Long = 15,
    /** Sett til MANUELL_OPPFOLGING i stedet for FEILET etter maxAntallFeil. */
    val settTilManuellOppfolgning: Boolean = false,
)
