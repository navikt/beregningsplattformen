package no.nav.utbetaling.beregning.oppdrag.xml

import java.time.LocalDate

internal data class SimuleringsPerioder(
    val periodeFom: LocalDate,
    val periodeTom: LocalDate,
    val simuleringsStoppNivaa: List<no.nav.utbetaling.beregning.oppdrag.xml.SimuleringsStoppNivaa> = emptyList(),
)
