package no.nav.sokos.dare.poc.beregning.beregningsmotor

import java.math.BigDecimal

import no.nav.sokos.dare.poc.beregning.Beregning
import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.beregning.DelberegningType
import no.nav.sokos.dare.poc.beregning.Delberegningslinje
import no.nav.sokos.dare.poc.beregning.EkstraInfo.Reasons
import no.nav.sokos.dare.poc.beregning.EkstraInfoDsl.verdi
import no.nav.sokos.dare.poc.beregning.LinjeKlassekode
import no.nav.sokos.dare.poc.beregning.TekstEkstraInfo
import no.nav.sokos.dare.poc.oppdrag.OppdragId
import no.nav.sokos.dare.poc.util.BigDecimalUtils

object TidligereUtbetalinger {
    fun beregn(
        relaterteBeregninger: List<Beregning>,
        periode: DatoIntervall,
        oppdragId: OppdragId,
        kodeKlasse: LinjeKlassekode,
    ): List<Delberegningslinje> =
        relaterteBeregninger.flatMap { beregning ->
            beregning.delberegning
                /* TODO: Her er det som rusker i dagens modell enda - bare ordrelinjer/kjedebiter med
                   riktig klassekode ift ytelsen vi motregner mot skal tas med.
                   Modellen må endres før det er mulig, eller Steinar må godkjenne denne implementasjonen.
                 */
                .filter { it.datoIntervall.overlaps(periode) && it.oppdragId == oppdragId }
                .flatMap { delberegning ->
                    delberegning.linjer
                        .filter { it.type == DelberegningType.YTELSE && it.kodeKlasse == kodeKlasse }
                        .map { ytelseLinje ->
                            val proratert = prorater(ytelseLinje.sum, delberegning.datoIntervall, periode)
                            Delberegningslinje(
                                type = DelberegningType.TIDLIGERE_UTBETALT,
                                dagsbeloep = emptyList(),
                                beloep = -proratert,
                                ekstra =
                                    setOf(
                                        TekstEkstraInfo(
                                            grunn = Reasons.TIDLIGERE_UTBETALT,
                                            verdier =
                                                mapOf(
                                                    "beregningId" verdi (beregning.id?.id ?: "ukjent"),
                                                    "overfoertDato" verdi (beregning.overfoeres ?: "ikke satt"),
                                                ),
                                        ),
                                    ),
                            )
                        }
                }
        }

    /**
     * Proraterer (visstnok et vanlig finansbegrep)
     * et beløp fra en kildeperiode til en målperiode basert på hverdager.
     *
     * Hvis kildeperioden er helt innenfor målet, returneres hele beløpet.
     * Ellers returneres den proporsjonale andelen for de overlappende hverdagene,
     * med tapsfri heltallsdivisjon som sikrer at oppsplittingene summerer til originalbeløpet.
     */
    fun prorater(
        amount: BigDecimal,
        sourcePeriode: DatoIntervall,
        targetPeriode: DatoIntervall,
    ): BigDecimal {
        val sourceHverdager = sourcePeriode.hverdager
        val overlapIndices = sourceHverdager.indices.filter { targetPeriode.contains(sourceHverdager[it]) }

        return BigDecimalUtils.prorater(amount, sourceHverdager.size, overlapIndices)
    }
}
