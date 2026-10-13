package no.nav.utbetaling.beregning.oppdrag.xml

import java.time.LocalDate

internal data class OppdragsEnhet120(
    val typeEnhet: String,
    val enhet: String? = null,
    val datoEnhetFom: LocalDate,
)
