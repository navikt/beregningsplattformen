package no.nav.utbetaling.beregningsflyt.beregning

import java.math.BigDecimal.ZERO

import no.nav.sokos.dare.poc.beregning.Dagbeloep
import no.nav.sokos.dare.poc.beregning.DelberegningType
import no.nav.sokos.dare.poc.beregning.Delberegningslinje
import no.nav.sokos.dare.poc.beregning.EkstraInfo.Reasons
import no.nav.sokos.dare.poc.beregning.EkstraInfoDsl.verdi
import no.nav.sokos.dare.poc.beregning.LinjeKlassekode
import no.nav.sokos.dare.poc.beregning.TekstEkstraInfo
import no.nav.sokos.dare.poc.beregning.beregningsmotor.TidligereUtbetalinger
import no.nav.sokos.dare.poc.beregning.dataklasser.Satstype
import no.nav.sokos.dare.poc.beregning.fordeler.BeregningsScratchpad
import no.nav.sokos.dare.poc.beregning.isWeekday
import no.nav.utbetaling.beregning.tid.Periode.LukketPeriode
import no.nav.utbetaling.beregningsflyt.fordeler.beregningssignatur
import no.nav.utbetaling.beregningsflyt.identitet.Beregningsidentitet
import no.nav.utbetaling.beregningsflyt.kontekst.BeregningsmotorKontekst

class Ytelsesmotor(
    private val identitet: Beregningsidentitet,
) {
    private val logger = System.getLogger(Ytelsesmotor::class.java.name)

    fun beregnYtelse(kontekst: BeregningsmotorKontekst): BeregningsmotorKontekst {
        kontekst.beregningsrekkefolge().forEach { (segment, content) ->
            val periode = content.periode
            val tom = requireNotNull(periode.tom) { "Expected a finite calculation interval: $periode" }
            val scratchpad = segment.scratchpad.getOrPut(content.oppdragId) { BeregningsScratchpad() }
            content.oppdrag.tidslinjer.forEach { (kjedeId, tidslinje) ->
                val intervaller = tidslinje.toList()
                val beskrivelse = "${content.oppdragsnokkel} $periode $kjedeId"
                require(intervaller.isNotEmpty()) { "$beskrivelse: Missing chain coverage" }
                require(intervaller.map { it.second.beregningssignatur(identitet) }.distinct().size == 1) {
                    "$beskrivelse: Conflicting calculation signatures"
                }
                var nesteDag = periode.fom
                var dekket = false
                intervaller.sortedBy { it.first.fom }.forEach { (fysiskPeriode, _) ->
                    val fysiskTom = (fysiskPeriode as? LukketPeriode)?.tom
                    require(fysiskTom != null && fysiskPeriode.fom <= nesteDag) { "$beskrivelse: Incomplete continuous chain coverage" }
                    if (fysiskTom >= tom) {
                        dekket = true
                    } else {
                        nesteDag = maxOf(nesteDag, fysiskTom.plusDays(1))
                    }
                }
                require(dekket) { "$beskrivelse: Incomplete continuous chain coverage" }

                val kilde = intervaller.first().second
                val kodeKlasse = LinjeKlassekode.Ytelse(content.klassekoder.finn(kilde.ytelseKlassekode.opplysning))
                val dagsbeloep =
                    periode.dager.map {
                        Dagbeloep(dato = it, beloep = if (it.isWeekday()) kilde.sats.opplysning.verdi else ZERO)
                    }
                scratchpad.kapasitetStyring.addYtelse(dagsbeloep)
                val linjer = scratchpad.beregninger.getOrPut(periode) { mutableListOf() }
                linjer.add(
                    Delberegningslinje(
                        type = DelberegningType.YTELSE,
                        dagsbeloep = if (dagsbeloep.any { it.beloep.compareTo(ZERO) != 0 }) dagsbeloep else emptyList(),
                        beloep = ZERO,
                        ekstra = emptySet(),
                        kapasitet = scratchpad.kapasitetStyring.snapshot(periode),
                        satsType = Satstype.DAG,
                        sats = kilde.sats.opplysning.verdi,
                        oppdragslinjekjedeId = identitet.resultatKjedeId(kjedeId),
                        kodeKlasse = kodeKlasse,
                    ),
                )
                val tidligere = TidligereUtbetalinger.beregn(content.relaterteBeregninger, periode, content.oppdragId, kodeKlasse)
                scratchpad.erEtterbetaling[periode] = tidligere.isNotEmpty()
                tidligere.forEach { linje ->
                    linjer.add(linje)
                    val deduction = linje.sum.negate()
                    if (deduction > ZERO) {
                        val kapasitet = scratchpad.kapasitetStyring.current()
                        val skalarKapasitet = periode.hverdager.sumOf { kapasitet[it] ?: ZERO }
                        val cappedDeduction = deduction.min(skalarKapasitet)
                        if (cappedDeduction < deduction) {
                            val ekstraInfo =
                                TekstEkstraInfo(
                                    grunn = Reasons.KREVER_TILBAKEKREVING,
                                    verdier =
                                        mapOf(
                                            "oppdrag" verdi content.oppdragId,
                                            "periode" verdi periode,
                                            "utbetalt" verdi deduction,
                                            "ytelse" verdi skalarKapasitet,
                                        ),
                                )
                            logger.log(System.Logger.Level.WARNING, ekstraInfo.toString())
                            scratchpad.ekstraInfo.add(ekstraInfo)
                        }
                        scratchpad.kapasitetStyring.applyTidligereUtbetalt(periode, cappedDeduction)
                    }
                }
            }
        }
        return kontekst
    }
}
