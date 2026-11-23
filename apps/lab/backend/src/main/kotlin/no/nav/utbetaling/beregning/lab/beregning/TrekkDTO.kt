package no.nav.utbetaling.beregning.lab.beregning

import java.math.BigDecimal
import java.time.LocalDate

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class TrekkDTO(
    val trekkType: String,
    val beregningsType: String,
    val skatteReduserende: Boolean,
    @Contextual
    val saldo: BigDecimal? = null,
    @Contextual
    val gyldigTom: LocalDate? = null,
    val trekkPerioder: List<TrekkPeriodeDTO>,
)

@Serializable
data class TrekkPeriodeDTO(
    @Contextual
    val sats: BigDecimal,
    val satsType: String,
    @Contextual
    val fom: LocalDate,
    @Contextual
    val tom: LocalDate,
)
