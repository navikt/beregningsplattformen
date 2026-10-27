package no.nav.sokos.dare.poc.beregning.fordeler

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month.DECEMBER
import java.time.Month.JANUARY
import java.time.YearMonth

import no.nav.sokos.dare.poc.aktoer.AktoerId
import no.nav.sokos.dare.poc.avvent.Avvent
import no.nav.sokos.dare.poc.beregning.Beregning
import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.beregning.DelberegningType
import no.nav.sokos.dare.poc.beregning.EkstraInfo
import no.nav.sokos.dare.poc.beregning.EkstraInfoDsl.verdi
import no.nav.sokos.dare.poc.beregning.TekstEkstraInfo
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Beregningsmotor
import no.nav.sokos.dare.poc.beregning.dataklasser.BeregningsStatus
import no.nav.sokos.dare.poc.beregning.dataklasser.BeregningsStatusflagg
import no.nav.sokos.dare.poc.oppdrag.Oppdrag
import no.nav.sokos.dare.poc.oppdrag.Oppdragslinje
import no.nav.sokos.dare.poc.trekk.FagomraadeKode
import no.nav.sokos.dare.poc.trekk.SatsType
import no.nav.sokos.dare.poc.trekk.Trekkberegningstype
import no.nav.sokos.dare.poc.trekk.Trekkperiode
import no.nav.sokos.dare.poc.trekk.Trekkvedtak

/**
 * BeregningsFordeler deler opp en beregningskontekst i biter som kan beregnes stand-alone. Den implementerer det som heter "stoppnivåer" på stormaskin. Eksempel på kriterier for oppsplitting:
 *
 * - et trekkvedtak er spesifisert med trekk per måned. Vi tvinger frem en oppdeling på månedsgrense for å enkelt kunne spore hvor mye av trekket som er igjen for måneden
 * - en del av perioden har allerede blitt utbetalt, og brukeren har tabell-kort. Da skal etterbetalingen bruke prosentsatsen for utregning av skatt, mens ny-utbetaling bruker tabellen
 */
class BeregningsFordeler(
    // TODO: Endre navn til Bereningskontekstbygger?
    private val beregningsmotor: Beregningsmotor, // TODO: Flytt bereningsmotor ut herfra?  Den brukes bare til beregnigsmotor.calculate() og det virker rart at fordel() skal utføre beregning?
) {
    fun fordel(
        input: FordelingInput,
        periode: DatoIntervall,
        oppdrag: List<Oppdrag>,
        trekk: List<Trekkvedtak>,
        skatteKontekst: SkatteKontekst,
        relaterteBeregninger: List<Beregning>,
        ledigFrikortBeloep: BigDecimal? = null,
    ): BeregningsmotorKontekst {
        require(oppdrag.all { it.gjelderId == oppdrag.first().gjelderId }) { "Alle oppdrag i en beregning er for samme aktør" }
        val kontekst =
            byggBeregningsmotorKontekst(
                periode = periode,
                oppdrag = oppdrag,
                trekk = trekk,
                input = input,
                skatteKontekst = skatteKontekst,
                ledigFrikortBeloep = ledigFrikortBeloep,
                relaterteBeregninger = relaterteBeregninger,
                overføringsdato = input.overfoeringsdato,
                beregnet = input.beregnet,
            )
        return beregningsmotor.calculate(kontekst)
    }

    internal fun byggBeregningsmotorKontekst(
        periode: DatoIntervall,
        oppdrag: List<Oppdrag>,
        trekk: List<Trekkvedtak>,
        input: FordelingInput,
        ledigFrikortBeloep: BigDecimal? = null,
        skatteKontekst: SkatteKontekst,
        relaterteBeregninger: List<Beregning>,
        overføringsdato: LocalDate?,
        beregnet: LocalDateTime,
    ): BeregningsmotorKontekst {
        val korrigerteOppdrag = oppdrag
        val segmenter = segmenterPeriode(periode, korrigerteOppdrag, trekk, input, relaterteBeregninger, beregnet)
        val saldoBoetter = beregnInitielleSaldoer(trekk, relaterteBeregninger)
        val satsBoetter = beregnInitielleSatser(segmenter, relaterteBeregninger)

        return BeregningsmotorKontekst(
            periode = periode,
            segmenter = segmenter,
            satsBoetter = SatsBoetter(satsBoetter),
            saldoBoetter = SaldoBoetter(saldoBoetter),
            skatteKontekst = skatteKontekst,
            restFrikortBeloep = ledigFrikortBeloep,
            overføringsdato = overføringsdato,
            beregnet = beregnet,
        )
    }

    // --- Periodeoppdeling ---
    // Deler opp i BeregningsSegmenter, som til slutt vil bli til Beregninger.
    private fun segmenterPeriode(
        periode: DatoIntervall,
        oppdrag: List<Oppdrag>,
        trekk: List<Trekkvedtak>,
        input: FordelingInput,
        relaterteBeregninger: List<Beregning>,
        beregningsdato: LocalDateTime,
    ): List<BeregningsSegment> {
        val avventPerioder = oppdrag.flatMap { it.avventPerioder }
        // Gruppering på kid/utbetales til, fører til BeregningsSegmenter
        val groupKeyOppdrag = oppdragByGroupKey(oppdrag)

        val segmenter =
            groupKeyOppdrag.flatMap { entry ->
                lagBeregningsSegmenter(periode, entry.key, entry.value, trekk, input, relaterteBeregninger, beregningsdato)
            }

        val avventSegmenter = tagAvventSegmenter(segmenter, avventPerioder)
        berikSegmentMedInformasjon(avventSegmenter, input.gjelder)
        return avventSegmenter
    }

    private fun lagBeregningsSegmenter(
        periode: DatoIntervall,
        linjeGroupKey: LinjeGroupKey,
        oppdrag: List<Oppdrag>,
        trekk: List<Trekkvedtak>,
        input: FordelingInput,
        relaterteBeregninger: List<Beregning>,
        beregningsdato: LocalDateTime,
    ): List<BeregningsSegment> {
        // Avventperioder fører også til egne BeregningsSegment
        val avventPerioder = oppdrag.flatMap { it.avventPerioder }

        val segmenter =
            periode.divide(avventPerioder.map { it.periode }).map { segmentPeriode ->
                // Vi samler alle datointervaller som fører til Delberegninger.
                val allTrekkperioder = trekk.flatMap { it.trekkperioder }
                val trekkBoundaries = trekkBoundaries(allTrekkperioder, segmentPeriode)
                val relaterteBoundaries = relaterteBoundaries(relaterteBeregninger, oppdrag)
                val skatteKortBoundaries: List<DatoIntervall> = skattekortBoundaries(segmentPeriode, beregningsdato)
                val ytelsesBoundaries: List<DatoIntervall> = oppdrag.flatMap { it.alleOppdragslinjer }.map { it.periode }
                val yearBoundaries = if (segmentPeriode.tom != null && segmentPeriode.fom.year != segmentPeriode.tom.year) segmentPeriode.years() else emptyList()

                val delPeriodeIntervall = segmentPeriode.divide(trekkBoundaries, relaterteBoundaries, skatteKortBoundaries, yearBoundaries, ytelsesBoundaries)

                val delperioder =
                    delPeriodeIntervall
                        .flatMap { segment ->
                            lagDelperioderForSegment(oppdrag, trekk, input, segment, relaterteBeregninger)
                        }.filterNot {
                            it.oppdrag.alleOppdragslinjer.isEmpty()
                        }
                BeregningsSegment(
                    linjeGroupKey,
                    delperioder,
                )
            }

        return segmenter.map { segment ->
            segment.copy(delperioder = sortDelperioder(segment.delperioder))
        }
    }

    private fun trekkBoundaries(
        trekkPerioder: List<Trekkperiode>,
        segmentPeriode: DatoIntervall,
    ): List<DatoIntervall> =
        trekkPerioder.flatMap { trekkPeriode ->
            if (trekkPeriode.satstype == SatsType.M) {
                trekkPeriode.periode.divide(segmentPeriode.months())
            } else {
                listOf(trekkPeriode.periode)
            }
        }

    private fun skattekortBoundaries(
        periode: DatoIntervall,
        beregningsDato: LocalDateTime,
    ): List<DatoIntervall> {
        val desemberIFjor = LocalDate.of(beregningsDato.year - 1, DECEMBER, 1)
        val needsDecemberSplit = periode.contains(desemberIFjor)
        val janFeb = DatoIntervall(LocalDate.of(beregningsDato.year, JANUARY, 1), LocalDate.of(beregningsDato.year, JANUARY, 31))
        val needsJanFebSplit = periode.isOpen() || periode.overlaps(janFeb)

        return if (needsDecemberSplit || needsJanFebSplit) {
            buildList {
                if (needsDecemberSplit) add(DatoIntervall(periode.fom, desemberIFjor))
                if (needsJanFebSplit) add(janFeb)
            }.toList()
        } else {
            listOf(periode)
        }
    }

    // Deler opp slik at det som returneres innenfor en OppdragDelPeriode kan beregnes til en DelBeregning uten å se til nabo-perioder. Unntaket er beregning av skatt med tabelltrekk.
    private fun lagDelperioderForSegment(
        oppdrag: List<Oppdrag>,
        trekk: List<Trekkvedtak>,
        input: FordelingInput,
        segment: DatoIntervall,
        relaterteBeregninger: List<Beregning> = emptyList(),
    ): List<OppdragDelperiode> =
        oppdrag.map { oppdragPeriode ->
            val overfoeringsmaaned = YearMonth.from(input.effektivOverfoeringsdato())
            val tidligereTrukneTrekkvedtakIMaaneden =
                relaterteBeregninger
                    .filter { beregning -> beregning.overfoeres?.let { YearMonth.from(it) == overfoeringsmaaned } == true }
                    .flatMap { it.delberegning }
                    .filter { it.oppdragId == oppdragPeriode.id }
                    .flatMap { it.linjer }
                    .filter { it.type == DelberegningType.TREKK }
                    .mapNotNull { it.trekkvedtakId }
                    .toSet()
            OppdragDelperiode(
                oppdrag = oppdragPeriode.medOppdragslinjer(oppdragPeriode.alleOppdragslinjer.filter { segment.overlaps(it.periode) }),
                periode = segment,
                konto = input.konto,
                gjelder = input.gjelder,
                beregnet = input.beregnet,
                utbetalesTil = input.utbetalesTil,
                status = input.status,
                trekk = buildTrekkForDelperiode(segment, input, trekk),
                ekstraInfo = input.ekstraInfo,
                overfoeringsdato = input.overfoeringsdato,
                tidligereTrukneTrekkvedtakIMaaneden = tidligereTrukneTrekkvedtakIMaaneden,
                relaterteBeregninger = filterRelaterteTilOppdrag(relaterteBeregninger, oppdragPeriode, segment),
            )
        }

    // --- Grunnlagsbygging ---
    private fun buildTrekkForDelperiode(
        delperiode: DatoIntervall,
        input: FordelingInput,
        trekk: List<Trekkvedtak>,
    ): List<TrekkvedtakForBeregning> =
        trekk.filter { it.isTrekkGyldigIFagomraade(FagomraadeKode.AAP) }.mapNotNull { vedtak ->
            val relevantPeriode =
                vedtak.trekkperioder.firstOrNull { tp ->
                    when (vedtak.trekktype?.trekkberegningstype) {
                        Trekkberegningstype.UTBDATO -> tp.periode.contains(input.effektivOverfoeringsdato())
                        Trekkberegningstype.YTELPER, null -> tp.periode.overlaps(delperiode)
                    }
                }
            relevantPeriode?.let {
                val vedtakBoetter = boetterForTrekkvedtak(vedtak, it, delperiode, input.effektivOverfoeringsdato())
                TrekkvedtakForBeregning(
                    trekkvedtak = vedtak,
                    trekkperiode = makeTrekkPeriodeForBeregning(it, vedtakBoetter),
                )
            }
        }

    /** For UTBDATO-trekk brukes overfoeringsdato som bøtte-nøkkel i stedet for ytelsesperioden */
    private fun boetterForTrekkvedtak(
        vedtak: Trekkvedtak,
        trekkperiode: Trekkperiode,
        delperiode: DatoIntervall,
        overfoeringsdato: LocalDate,
    ): List<String> {
        val defaultBoetter =
            when (trekkperiode.satstype) {
                SatsType.M -> listOf(YearMonth.from(delperiode.fom).toString())
                SatsType.D -> delperiode.hverdager.map { it.toString() }
                else -> emptyList()
            }
        return when (vedtak.trekktype?.trekkberegningstype) {
            Trekkberegningstype.YTELPER, null -> {
                defaultBoetter
            }

            Trekkberegningstype.UTBDATO -> {
                when (trekkperiode.satstype) {
                    SatsType.D -> listOf(overfoeringsdato.toString())
                    SatsType.M -> listOf(YearMonth.from(overfoeringsdato).toString())
                    SatsType.P -> defaultBoetter
                }
            }
        }
    }

    // --- Informasjon og gruppering ---

    private fun sortDelperioder(delperioder: List<OppdragDelperiode>): List<OppdragDelperiode> =
        delperioder.sortedWith(
            compareBy({
                it.oppdrag.id?.id
            }, { it.periode.fom }),
        )

    private fun berikSegmentMedInformasjon(
        segmenter: List<BeregningsSegment>,
        gjelder: AktoerId,
    ) {
        var forrigeKid: String? = null
        segmenter.forEach { segment ->
            val key = segment.linjeGroupKey
            if (key != null && key.utbetalesTilId != gjelder) {
                segment.ekstraInfo.add(
                    TekstEkstraInfo(
                        grunn = EkstraInfo.Reasons.OVERSTYRT_MOTTAKER,
                        verdier =
                            mapOf(
                                "utbetalesTilId" verdi key.utbetalesTilId,
                            ),
                    ),
                )
            }
            if (forrigeKid != null && key?.kid != forrigeKid) {
                segment.ekstraInfo.add(
                    TekstEkstraInfo(
                        grunn = EkstraInfo.Reasons.KID_ENDRET,
                    ),
                )
            }
            forrigeKid = key?.kid
        }
    }

    private fun tagAvventSegmenter(
        segmenter: List<BeregningsSegment>,
        avventPerioder: List<Avvent>,
    ): List<BeregningsSegment> {
        if (avventPerioder.isEmpty()) return segmenter

        return segmenter.map { segment ->
            val segmentPeriode = segment.delperioder.firstOrNull()?.periode ?: return@map segment
            val matchingAvvent =
                avventPerioder.firstOrNull { avvent ->
                    avvent.periode.overlaps(segmentPeriode)
                }
            if (matchingAvvent != null) {
                val statusflagg =
                    when (matchingAvvent.kodeArsak.uppercase()) {
                        "AVAV" -> BeregningsStatusflagg.MANUELL
                        "AVRK" -> BeregningsStatusflagg.DATOSTYRT
                        else -> null
                    }
                val overfoeres =
                    when (matchingAvvent.kodeArsak.uppercase()) {
                        "AVRK" -> matchingAvvent.datoOverfores
                        else -> null
                    }
                segment
                    .copy(
                        avventStatus = BeregningsStatus.AVVENT,
                        avventOverfoeres = overfoeres,
                    ).also { segment ->
                        statusflagg?.let { segment.beregningsStatusflagg.add(it) }
                        segment.ekstraInfo.add(
                            TekstEkstraInfo(
                                grunn = EkstraInfo.Reasons.AVVENT,
                                verdier =
                                    mapOf(
                                        "kodeArsak" verdi matchingAvvent.kodeArsak.uppercase(),
                                        "datoOverfores" verdi
                                            (matchingAvvent.datoOverfores?.toString() ?: "ikke satt — krever manuell reaktivering"),
                                    ),
                            ),
                        )
                    }
            } else {
                segment
            }
        }
    }

    private fun resolveGroupKey(
        linje: Oppdragslinje,
        oppdrag: Oppdrag,
    ): LinjeGroupKey =
        LinjeGroupKey(
            utbetalesTilId = linje.utbetalesTilId ?: oppdrag.gjelderId,
            kid = linje.kid,
        )

    private fun oppdragByGroupKey(oppdrag: List<Oppdrag>): Map<LinjeGroupKey, List<Oppdrag>> {
        val lineGroups =
            oppdrag.flatMap { oppdrag -> oppdrag.alleOppdragslinjer.map { linje -> linje to resolveGroupKey(linje, oppdrag) } }.toMap()
        val allGroups = lineGroups.values.distinct()

        return allGroups
            .mapNotNull { key ->
                val keyOppdrag =
                    oppdrag.mapNotNull { oppdrag ->
                        val keyLines = oppdrag.alleOppdragslinjer.filter { lineGroups[it] == key }
                        when {
                            keyLines.isEmpty() -> null
                            else -> oppdrag.medOppdragslinjer(keyLines)
                        }
                    }
                when {
                    keyOppdrag.isEmpty() -> null
                    else -> key to keyOppdrag
                }
            }.toMap()
    }
}
