package no.nav.sokos.dare.poc.beregning.beregningsmotor

import java.math.BigDecimal
import java.math.BigDecimal.ZERO
import java.time.LocalDate

import mu.KotlinLogging

import no.nav.sokos.dare.poc.beregning.Beregning
import no.nav.sokos.dare.poc.beregning.Dagbeloep
import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.beregning.DelberegningType
import no.nav.sokos.dare.poc.beregning.Delberegningslinje
import no.nav.sokos.dare.poc.beregning.EkstraInfo.Reasons
import no.nav.sokos.dare.poc.beregning.EkstraInfoDsl.verdi
import no.nav.sokos.dare.poc.beregning.LinjeKlassekode
import no.nav.sokos.dare.poc.beregning.TekstEkstraInfo
import no.nav.sokos.dare.poc.beregning.dataklasser.Satstype
import no.nav.sokos.dare.poc.beregning.fordeler.BeregningsScratchpad
import no.nav.sokos.dare.poc.beregning.fordeler.BeregningsmotorKontekst
import no.nav.sokos.dare.poc.beregning.isWeekday
import no.nav.sokos.dare.poc.oppdrag.OppdragId
import no.nav.sokos.dare.poc.oppdrag.Oppdragslinje
import no.nav.sokos.dare.poc.util.BigDecimalUtils

class
Ytelsesmotor {
    private val logger = KotlinLogging.logger {}

    fun beregnYtelse(kontekst: BeregningsmotorKontekst): BeregningsmotorKontekst {
        kontekst.segmenter.forEach { segment ->
            segment.delperioder.forEach { content ->
                val delperiode = content.periode
                val oppdragId = requireNotNull(content.oppdrag.id) { "Oppdrag mangler id" }
                val scratchpad = segment.scratchpad.getOrPut(oppdragId) { BeregningsScratchpad() }

                // På dette tidspunktet er linjeperiodene trimmet i henhold til kjederegler.
                content.oppdrag.alleOppdragslinjer.forEach { linje ->
                    require(linje.periode.contains(content.periode)) {
                        "OppdragDelperiode.periode ${content.periode} er utenfor OppdragsLinje.fom/tom ${linje.periode}"
                    }

                    val dagsBeloep = delperiode.dager.map { linje.beloepForDag(it) }
                    scratchpad.kapasitetStyring.addYtelse(dagsBeloep)

                    val harDagVerdier = dagsBeloep.any { it.beloep.compareTo(ZERO) != 0 }

                    val ytelseBeregning =
                        Delberegningslinje(
                            type = DelberegningType.YTELSE,
                            dagsbeloep = if (harDagVerdier) dagsBeloep else emptyList(), // Denne er her bare for konsistens med gamle testdata
                            beloep = ZERO,
                            ekstra = emptySet(),
                            kapasitet = scratchpad.kapasitetStyring.snapshot(content.periode),
                            satsType = Satstype.DAG, // TODO: Bare AAP
                            sats = linje.sats,
                            oppdragslinjekjedeId = linje.oppdragslinjekjedeId,
                            kodeKlasse = LinjeKlassekode.Ytelse(linje.kodeKlasse),
                        )
                    scratchpad.beregninger
                        .getOrPut(content.periode) { mutableListOf() }
                        .add(ytelseBeregning)
                    val tidligereUtbetaltLinjer =
                        beregnTidligereUtbetalt(
                            relaterteBeregninger = content.relaterteBeregninger,
                            periode = content.periode,
                            oppdragId = content.oppdrag.id,
                            kodeKlasse = LinjeKlassekode.Ytelse(linje.kodeKlasse),
                        )
                    scratchpad.erEtterbetaling[content.periode] = tidligereUtbetaltLinjer.isNotEmpty()
                    tidligereUtbetaltLinjer.forEach { linje ->
                        scratchpad.beregninger
                            .getOrPut(content.periode) { mutableListOf() }
                            .add(linje)

                        // Reduce restKapasitet by the deducted amount (linje.sum is negative)
                        val deduction = linje.sum.negate()
                        if (deduction > ZERO) {
                            val hverdager = content.periode.hverdager
                            val currentKapasitetMap = scratchpad.kapasitetStyring.current()
                            val currentKapasitet = hverdager.map { currentKapasitetMap[it] ?: ZERO }
                            val skalarKapasitet: BigDecimal = currentKapasitet.sumOf { it }
                            val cappedDeduction = deduction.min(skalarKapasitet)
                            if (cappedDeduction < deduction) {
                                val ekstraInfo =
                                    TekstEkstraInfo(
                                        grunn = Reasons.KREVER_TILBAKEKREVING,
                                        verdier =
                                            mapOf(
                                                "oppdrag" verdi content.oppdrag.id,
                                                "periode" verdi content.periode,
                                                "utbetalt" verdi deduction,
                                                "ytelse" verdi skalarKapasitet,
                                            ),
                                    )
                                logger.warn(ekstraInfo.toString())
                                scratchpad.ekstraInfo.add(ekstraInfo)
                            }
                            scratchpad.kapasitetStyring.applyTidligereUtbetalt(content.periode, cappedDeduction)
                        }
                    }
                }
            }
        }
        return kontekst
    }

    internal fun beregnTidligereUtbetalt(
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
    internal fun prorater(
        amount: BigDecimal,
        sourcePeriode: DatoIntervall,
        targetPeriode: DatoIntervall,
    ): BigDecimal {
        val sourceHverdager = sourcePeriode.hverdager
        val overlapIndices = sourceHverdager.indices.filter { targetPeriode.contains(sourceHverdager[it]) }

        return BigDecimalUtils.prorater(amount, sourceHverdager.size, overlapIndices)
    }
}

private fun Oppdragslinje.beloepForDag(dato: LocalDate): Dagbeloep =
    if (periode.contains(dato) && dato.isWeekday()) {
        Dagbeloep(dato = dato, beloep = sats)
    } else {
        Dagbeloep(dato = dato, beloep = ZERO)
    }
