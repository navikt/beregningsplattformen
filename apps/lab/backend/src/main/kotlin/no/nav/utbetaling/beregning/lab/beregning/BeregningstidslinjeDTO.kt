package no.nav.utbetaling.beregning.lab.beregning

import java.time.LocalDate

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

import no.nav.sokos.dare.poc.beregning.Beregning
import no.nav.sokos.dare.poc.beregning.DelberegningType
import no.nav.sokos.dare.poc.beregning.dataklasser.Satstype

@Serializable
data class BeregningDTO(
    val id: Int,
    val gjelder: String,
    val utbetalesTil: String,
    val kidnummer: String?,
    val status: String,
    val delberegninger: List<DelberegningDTO>,
)

@Serializable
data class DelberegningDTO(
    val id: Int,
    @Contextual val fom: LocalDate,
    @Contextual val tom: LocalDate,
    val linjer: List<LinjeDTO>,
    val fagomraade: String,
    val beloep: String,
    // TODO: fagsystemId
)

@Serializable
enum class LinjetypeDTO {
    YTELSE,
    SKATT,
    TREKK,
    FRIKORT_BRUK,
}

@Serializable
data class LinjeDTO(
    val id: Int,
    val type: LinjetypeDTO,
    val sats: String?,
    val beloep: String,
)

@Serializable
data class BeregningstidslinjeDTO(
    val beregninger: List<BeregningDTO>,
) {
    companion object {
        fun fra(beregninger: List<Beregning>): BeregningstidslinjeDTO {
            val beregningerDTO =
                beregninger.mapIndexed { beregningIndex, beregning ->
                    BeregningDTO(
                        id = beregningIndex + 1,
                        gjelder = "${beregning.gjelder.id}",
                        utbetalesTil = "${beregning.utbetalesTil?.id ?: beregning.gjelder?.id}", // TODO: Rart den kunnskapen ligger her
                        kidnummer = beregning.kid,
                        status = beregning.status.name,
                        // TODO: Her må det gjøres ting.
                        delberegninger =
                            beregning.delberegning.mapIndexed { delberegningIndex, delberegning ->
                                DelberegningDTO(
                                    id = delberegningIndex + 1,
                                    fom = delberegning.datoIntervall.fom,
                                    tom = requireNotNull(delberegning.datoIntervall.tom) { "Delberegning skal ha lukket periode" },
                                    beloep = delberegning.sum.toPlainString(),
                                    fagomraade = beregning.fagomraade.name, // TODO: Burde ligge på delberegningnivå i modellen også,
                                    linjer =
                                        delberegning.linjer.mapIndexed { linjeIndex, delberegningslinje ->
                                            LinjeDTO(
                                                id = linjeIndex + 1,
                                                beloep = delberegningslinje.sum.toPlainString(),
                                                sats =
                                                    when (delberegningslinje.satsType) {
                                                        Satstype.TABELL -> Satstype.TABELL.name
                                                        else -> delberegningslinje.sats?.let { "${it.toPlainString()} ${delberegningslinje.satsType!!.name}" }
                                                    },
                                                type =
                                                    when (delberegningslinje.type) {
                                                        DelberegningType.YTELSE -> LinjetypeDTO.YTELSE
                                                        DelberegningType.SKATT -> LinjetypeDTO.SKATT
                                                        DelberegningType.TREKK -> LinjetypeDTO.TREKK
                                                        DelberegningType.FRIKORT_BRUK -> LinjetypeDTO.FRIKORT_BRUK
                                                        DelberegningType.SUM,
                                                        DelberegningType.TIDLIGERE_UTBETALT,
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
