package no.nav.beregning.taskv2.error

import java.time.LocalDateTime

/**
 * Kast fra doTask for å utsette tasken til [triggerTid] uten å telle et ordinært feilforsøk.
 * Portet fra prosessering.
 */
data class RekjorSenereException(
    val arsak: String,
    val triggerTid: LocalDateTime,
) : RuntimeException("Rekjører senere - triggerTid=$triggerTid")

/** Kastes internt når en task har nådd maks antall rekjøringer. */
data class MaxAntallRekjoringerException(
    val maxAntallRekjoring: Int,
) : RuntimeException("Nådd max antall rekjøring - $maxAntallRekjoring")
