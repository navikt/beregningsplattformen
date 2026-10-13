package no.nav.utbetaling.beregning.oppdrag.xml

import java.time.LocalDate

internal data class SimuleringsPeriode300(
    val datoSimulerFom: LocalDate? = null,
    val datoSimulerTom: LocalDate? = null,
)
