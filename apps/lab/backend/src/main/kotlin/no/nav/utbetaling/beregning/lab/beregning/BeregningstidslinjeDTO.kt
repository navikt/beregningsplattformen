package no.nav.utbetaling.beregning.lab.beregning

import java.math.BigDecimal
import java.time.LocalDate

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

import no.nav.sokos.dare.poc.beregning.DelberegningType
import no.nav.sokos.dare.poc.beregning.fordeler.SkatteKontekst
import no.nav.utbetaling.beregning.oppdrag.Oppdragstidslinje
import no.nav.utbetaling.beregning.tid.Periode
import no.nav.utbetaling.beregningsflyt.Beregningsflyt
import no.nav.utbetaling.beregningsflyt.Beregningsgrunnlag

@Serializable
data class BeregningDTO(
    val id: Int,
    val delberegninger: List<DelberegningDTO>,
)

@Serializable
data class DelberegningDTO(
    val id: Int,
    @Contextual val fom: LocalDate,
    @Contextual val tom: LocalDate,
    val linjer: List<LinjeDTO>,
)

@Serializable
enum class LinjetypeDTO {
    YTELSE,
    SKATT,
    TREKK,
}

@Serializable
data class LinjeDTO(
    val id: Int,
    val type: LinjetypeDTO,
)

@Serializable
data class BeregningstidslinjeDTO(
    val beregninger: List<BeregningDTO>,
) {
    companion object {
        fun fra(
            oppdragstidslinje: Oppdragstidslinje,
            fom: LocalDate?,
            tom: LocalDate?,
        ): BeregningstidslinjeDTO {
            val beregningFom = fom ?: oppdragstidslinje.firstOrNull()?.first?.fom ?: error("Oppdragstidslinje er tom, da må beregningsperiode settes")
            val beregningTom =
                tom ?: oppdragstidslinje
                    .lastOrNull()
                    ?.first
                    ?.let { it as? Periode.LukketPeriode }
                    ?.tom ?: error("Oppdragstidslinje er tom/ åpen ende, da må beregningsperiode settes")

            val grunnlag =
                Beregningsgrunnlag(
                    beregningsperiode = Periode.LukketPeriode(beregningFom, beregningTom),
                    oppdragstidslinje = oppdragstidslinje,
                    skatteKontekst = SkatteKontekst(null, emptyMap()),
                    trekkVedtak = emptyList(),
                    overføres = LocalDate.now(),
                    relaterteBeregninger = emptyList(),
                    tilgjengeligFrikortbeløp = BigDecimal.ZERO,
                )

            val (_, beregninger) = Beregningsflyt().beregn(grunnlag)

            val beregningerDTO =
                beregninger.mapIndexed { beregningIndex, beregning ->
                    BeregningDTO(
                        id = beregningIndex + 1,
                        delberegninger =
                            beregning.delberegning.mapIndexed { delberegningIndex, delberegning ->
                                DelberegningDTO(
                                    id = delberegningIndex + 1,
                                    fom = delberegning.datoIntervall.fom,
                                    tom = delberegning.datoIntervall.tom ?: error("Delberegning skal ha lukket periode"),
                                    linjer =
                                        delberegning.linjer.mapIndexed { linjeIndex, delberegningslinje ->
                                            LinjeDTO(
                                                id = linjeIndex + 1,
                                                type =
                                                    when (delberegningslinje.type) {
                                                        DelberegningType.YTELSE -> LinjetypeDTO.YTELSE
                                                        DelberegningType.SKATT -> LinjetypeDTO.SKATT
                                                        DelberegningType.TREKK -> LinjetypeDTO.TREKK
                                                        DelberegningType.SUM,
                                                        DelberegningType.TIDLIGERE_UTBETALT,
                                                        DelberegningType.FRIKORT_BRUK,
                                                        -> error("Hva er linjetype ${delberegningslinje.type.name} ?")
                                                    },
                                            )
                                        },
                                )
                            },
                    )
                }
            return BeregningstidslinjeDTO(beregningerDTO)
        }
    }
}
