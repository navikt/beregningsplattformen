package no.nav.utbetaling.beregning.oppdrag.xml

import java.time.LocalDate

internal data class Valuta190(
    val typeValuta: String,
    val valuta: String,
    val datoValutaFom: LocalDate,
    val feilreg: String? = null,
)
