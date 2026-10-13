package no.nav.utbetaling.beregning.oppdrag.xml

import java.math.BigDecimal
import java.time.LocalDate

internal data class BelopsGrense130(
    val typeGrense: String,
    val belopGrense: BigDecimal,
    val datoGrenseFom: LocalDate,
    val datoGrenseTom: LocalDate? = null,
    val feilreg: String? = null,
)
