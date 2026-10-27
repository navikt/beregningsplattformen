package no.nav.sokos.dare.poc.oppdrag25

import java.math.BigDecimal
import java.time.LocalDate

data class BelopsGrense130(
    val typeGrense: String,
    val belopGrense: BigDecimal,
    val datoGrenseFom: LocalDate,
    val datoGrenseTom: LocalDate? = null,
    val feilreg: String? = null,
)
