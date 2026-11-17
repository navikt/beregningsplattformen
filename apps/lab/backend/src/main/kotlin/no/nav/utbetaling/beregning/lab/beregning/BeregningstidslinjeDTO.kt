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
enum class LinjetypeDTO {
    YTELSE,
    SKATT,
    TREKK,
}

@Serializable
data class LinjeDTO(
    val beregningId: Int,
    val delperiodeId: Int,
    val linjeId: Int,
    val type: LinjetypeDTO,
    @Contextual val fom: LocalDate,
    @Contextual val tom: LocalDate,
)

@Serializable
data class BeregningstidslinjeDTO(
    val linjer: List<LinjeDTO>,
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

            val linjer =
                beregninger.flatMapIndexed { beregningIndex, beregning ->
                    beregning.delberegning.flatMapIndexed { delberegningIndex, delberegning ->
                        delberegning.linjer.mapIndexed { linjeIndex, delberegningslinje ->
                            LinjeDTO(
                                beregningId = beregningIndex + 1,
                                delperiodeId = delberegningIndex + 1,
                                linjeId = linjeIndex + 1,
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
                                fom = delberegning.datoIntervall.fom,
                                tom = delberegning.datoIntervall.tom ?: error("Delberegning skal ha lukket periode"),
                            )
                        }
                    }
                }
            return BeregningstidslinjeDTO(linjer)
        }
    }
}
