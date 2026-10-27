package no.nav.sokos.dare.poc.oppdrag25

import java.time.LocalDate

data class OppdragsEnhet120(
    val typeEnhet: String,
    val enhet: String? = null,
    val datoEnhetFom: LocalDate,
)
