package no.nav.sokos.dare.poc.oppdrag25

import java.time.LocalDate

data class Valuta190(
    val typeValuta: String,
    val valuta: String,
    val datoValutaFom: LocalDate,
    val feilreg: String? = null,
)
