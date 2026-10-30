package no.nav.utbetaling.beregningsflyt.fordeler

import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

import no.nav.sokos.dare.poc.beregning.Beregning
import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.beregning.DelberegningType
import no.nav.sokos.dare.poc.beregning.EkstraInfo
import no.nav.sokos.dare.poc.beregning.EkstraInfoDsl.verdi
import no.nav.sokos.dare.poc.beregning.TekstEkstraInfo
import no.nav.sokos.dare.poc.beregning.dataklasser.BeregningsStatus
import no.nav.sokos.dare.poc.beregning.dataklasser.BeregningsStatusflagg
import no.nav.sokos.dare.poc.beregning.fordeler.FordelingInput
import no.nav.sokos.dare.poc.beregning.fordeler.SaldoBoetter
import no.nav.sokos.dare.poc.beregning.fordeler.SatsBoetter
import no.nav.sokos.dare.poc.beregning.fordeler.SkatteKontekst
import no.nav.sokos.dare.poc.beregning.fordeler.TrekkvedtakForBeregning
import no.nav.sokos.dare.poc.beregning.fordeler.beregnInitielleSaldoer
import no.nav.sokos.dare.poc.beregning.fordeler.beregnInitielleSatserForTrekk
import no.nav.sokos.dare.poc.beregning.fordeler.filterRelaterteTilOppdrag
import no.nav.sokos.dare.poc.beregning.fordeler.makeTrekkPeriodeForBeregning
import no.nav.sokos.dare.poc.trekk.FagomraadeKode
import no.nav.sokos.dare.poc.trekk.SatsType
import no.nav.sokos.dare.poc.trekk.Trekkberegningstype
import no.nav.sokos.dare.poc.trekk.Trekkvedtak
import no.nav.utbetaling.beregning.oppdrag.Avventingsårsak
import no.nav.utbetaling.beregning.oppdrag.Oppdragstidslinje
import no.nav.utbetaling.beregningsflyt.beregning.Beregningsmotor
import no.nav.utbetaling.beregningsflyt.beregning.Ytelsesmotor
import no.nav.utbetaling.beregningsflyt.identitet.Beregningsidentitet
import no.nav.utbetaling.beregningsflyt.identitet.Klassekodeoppslag
import no.nav.utbetaling.beregningsflyt.kontekst.BeregningsSegment
import no.nav.utbetaling.beregningsflyt.kontekst.BeregningsmotorKontekst
import no.nav.utbetaling.beregningsflyt.kontekst.OppdragDelperiode
import no.nav.utbetaling.beregningsflyt.kontekst.avgrensTil

class BeregningsFordeler(
    private val identitet: Beregningsidentitet,
    private val klassekoder: Klassekodeoppslag,
    private val motor: Beregningsmotor = Beregningsmotor(Ytelsesmotor(identitet)),
) {
    fun fordel(
        input: FordelingInput,
        periode: DatoIntervall,
        oppdrag: Oppdragstidslinje,
        trekk: List<Trekkvedtak>,
        skatteKontekst: SkatteKontekst,
        relaterteBeregninger: List<Beregning>,
        ledigFrikortBeloep: BigDecimal? = null,
    ): BeregningsmotorKontekst = motor.calculate(byggBeregningsmotorKontekst(input, periode, oppdrag, trekk, skatteKontekst, relaterteBeregninger, ledigFrikortBeloep))

    fun byggBeregningsmotorKontekst(
        input: FordelingInput,
        periode: DatoIntervall,
        oppdrag: Oppdragstidslinje,
        trekk: List<Trekkvedtak>,
        skatteKontekst: SkatteKontekst,
        relaterteBeregninger: List<Beregning>,
        ledigFrikortBeloep: BigDecimal? = null,
    ): BeregningsmotorKontekst {
        require(periode.tom?.let { it >= periode.fom } == true) { "Expected a finite calculation interval: $periode" }
        val kilde = oppdrag.avgrensTil(periode)
        val avvent = avventSegmentering(kilde, periode)
        val relevanteTrekk = trekk.filter { it.isTrekkGyldigIFagomraade(FagomraadeKode.AAP) }
        val overfoeringsdato = input.overfoeringsdato ?: input.beregnet.toLocalDate()
        val segmenter =
            grupperTidslinjer(kilde, periode, identitet)
                .flatMap { gruppe ->
                    val oppdragId = identitet.oppdragId(gruppe.oppdragsnokkel)
                    val tidligereTrekk =
                        relaterteBeregninger
                            .filter { it.overfoeres?.let { dato -> YearMonth.from(dato) == YearMonth.from(overfoeringsdato) } == true }
                            .flatMap { it.delberegning }
                            .filter { it.oppdragId == oppdragId }
                            .flatMap { it.linjer }
                            .filter { it.type == DelberegningType.TREKK }
                            .mapNotNull { it.trekkvedtakId }
                            .toSet()
                    avvent.getValue(gruppe.oppdragsnokkel).mapNotNull { avventSegment ->
                        val delperioder =
                            delperioder(gruppe.oppdrag, avventSegment.periode, identitet, oppdragId, relevanteTrekk, input.beregnet, relaterteBeregninger).map { delperiode ->
                                OppdragDelperiode(
                                    oppdrag = gruppe.oppdrag,
                                    oppdragsnokkel = gruppe.oppdragsnokkel,
                                    oppdragId = oppdragId,
                                    fagomraade = FagomraadeKode.valueOf(gruppe.oppdragsnokkel.fagomrade.fagområde),
                                    identitet = identitet,
                                    klassekoder = klassekoder,
                                    periode = delperiode,
                                    konto = input.konto,
                                    gjelder = input.gjelder,
                                    beregnet = input.beregnet,
                                    utbetalesTil = input.utbetalesTil,
                                    status = input.status,
                                    trekk = byggTrekk(delperiode, relevanteTrekk, overfoeringsdato),
                                    ekstraInfo = input.ekstraInfo,
                                    overfoeringsdato = input.overfoeringsdato,
                                    tidligereTrukneTrekkvedtakIMaaneden = tidligereTrekk,
                                    relaterteBeregninger = filterRelaterteTilOppdrag(relaterteBeregninger, oppdragId, delperiode),
                                    kildemottaker = requireNotNull(gruppe.linjeGroupKey.utbetalesTilId),
                                )
                            }
                        if (delperioder.isEmpty()) return@mapNotNull null
                        BeregningsSegment(
                            linjeGroupKey = gruppe.linjeGroupKey,
                            delperioder = delperioder,
                            avventStatus = avventSegment.arsak?.let { BeregningsStatus.AVVENT },
                            avventOverfoeres = (avventSegment.arsak as? Avventingsårsak.Refusjonskrav)?.til,
                        ).also { segment -> berikAvvent(segment, avventSegment.arsak) }
                    }
                }.also {
                    // Legacy KID diagnostics follow source-group encounter, not execution order.
                    berikMottakerOgKid(it, input)
                }.groupBy { it.delperioder.first().oppdragsnokkel }
                .values
                .flatMap { perOppdrag ->
                    perOppdrag.sortedBy {
                        it.delperioder
                            .first()
                            .periode.fom
                    }
                }
        return BeregningsmotorKontekst(
            periode = periode,
            segmenter = segmenter,
            skatteKontekst = skatteKontekst,
            satsBoetter = SatsBoetter(beregnInitielleSatserForTrekk(segmenter.flatMap { it.delperioder }.flatMap { it.trekk }, relaterteBeregninger)),
            saldoBoetter = SaldoBoetter(beregnInitielleSaldoer(relevanteTrekk, relaterteBeregninger)),
            restFrikortBeloep = ledigFrikortBeloep,
            overføringsdato = input.overfoeringsdato,
            beregnet = input.beregnet,
        )
    }

    private fun byggTrekk(
        delperiode: DatoIntervall,
        trekk: List<Trekkvedtak>,
        overfoeringsdato: LocalDate,
    ): List<TrekkvedtakForBeregning> =
        trekk.mapNotNull { vedtak ->
            val tp =
                vedtak.trekkperioder.firstOrNull {
                    when (vedtak.trekktype?.trekkberegningstype) {
                        Trekkberegningstype.UTBDATO -> it.periode.contains(overfoeringsdato)
                        Trekkberegningstype.YTELPER, null -> it.periode.overlaps(delperiode)
                    }
                } ?: return@mapNotNull null
            val boetter =
                when (tp.satstype) {
                    SatsType.M -> listOf(YearMonth.from(if (vedtak.trekktype?.trekkberegningstype == Trekkberegningstype.UTBDATO) overfoeringsdato else delperiode.fom).toString())
                    SatsType.D ->
                        if (vedtak.trekktype?.trekkberegningstype == Trekkberegningstype.UTBDATO) {
                            listOf(overfoeringsdato.toString())
                        } else {
                            delperiode.hverdager.map { it.toString() }
                        }
                    SatsType.P -> emptyList()
                }
            TrekkvedtakForBeregning(vedtak, makeTrekkPeriodeForBeregning(tp, boetter))
        }

    private fun berikAvvent(
        segment: BeregningsSegment,
        arsak: Avventingsårsak?,
    ) {
        if (arsak == null) return
        val (kode, dato) =
            when (arsak) {
                is Avventingsårsak.Avregning -> {
                    segment.beregningsStatusflagg.add(BeregningsStatusflagg.MANUELL)
                    "AVAV" to arsak.til
                }
                is Avventingsårsak.Refusjonskrav -> {
                    segment.beregningsStatusflagg.add(BeregningsStatusflagg.DATOSTYRT)
                    "AVRK" to arsak.til
                }
            }
        segment.ekstraInfo.add(
            TekstEkstraInfo(
                grunn = EkstraInfo.Reasons.AVVENT,
                verdier = mapOf("kodeArsak" verdi kode, "datoOverfores" verdi (dato?.toString() ?: "ikke satt — krever manuell reaktivering")),
            ),
        )
    }

    private fun berikMottakerOgKid(
        segmenter: List<BeregningsSegment>,
        input: FordelingInput,
    ) {
        segmenter.groupBy { it.delperioder.first().oppdragsnokkel }.values.forEach { perOppdrag ->
            var forrigeKid: String? = null
            perOppdrag.forEach { segment ->
                val key = requireNotNull(segment.linjeGroupKey)
                if (key.utbetalesTilId != input.gjelder) {
                    segment.ekstraInfo.add(TekstEkstraInfo(grunn = EkstraInfo.Reasons.OVERSTYRT_MOTTAKER, verdier = mapOf("utbetalesTilId" verdi key.utbetalesTilId)))
                }
                if (forrigeKid != null && key.kid != forrigeKid) {
                    segment.ekstraInfo.add(TekstEkstraInfo(grunn = EkstraInfo.Reasons.KID_ENDRET))
                }
                forrigeKid = key.kid
            }
        }
    }
}
