package no.nav.sokos.dare.poc.oppdrag25

import java.time.LocalDate

data class LinjeEnhet160(
    val typeEnhet: String,
    val enhet: String? = null,
    val datoEnhetFom: LocalDate,
)
