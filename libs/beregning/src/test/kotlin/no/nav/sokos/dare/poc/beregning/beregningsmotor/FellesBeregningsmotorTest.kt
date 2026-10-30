package no.nav.sokos.dare.poc.beregning.beregningsmotor

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

import no.nav.sokos.dare.poc.TestObjects
import no.nav.sokos.dare.poc.aktoer.AktoerId
import no.nav.sokos.dare.poc.beregning.Beregning
import no.nav.sokos.dare.poc.beregning.Dagbeloep
import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.beregning.Delberegning
import no.nav.sokos.dare.poc.beregning.DelberegningType
import no.nav.sokos.dare.poc.beregning.Delberegningslinje
import no.nav.sokos.dare.poc.beregning.EkstraInfo
import no.nav.sokos.dare.poc.beregning.LinjeKlassekode
import no.nav.sokos.dare.poc.beregning.TekstEkstraInfo
import no.nav.sokos.dare.poc.beregning.dataklasser.BeregningsStatus
import no.nav.sokos.dare.poc.beregning.dataklasser.BeregningsStatusflagg
import no.nav.sokos.dare.poc.beregning.dataklasser.Satstype
import no.nav.sokos.dare.poc.beregning.fordeler.BeregningsScratchpad
import no.nav.sokos.dare.poc.beregning.fordeler.Beregningsgrunnlag
import no.nav.sokos.dare.poc.beregning.fordeler.Delperiodegrunnlag
import no.nav.sokos.dare.poc.beregning.fordeler.LinjeGroupKey
import no.nav.sokos.dare.poc.beregning.fordeler.SaldoBoetter
import no.nav.sokos.dare.poc.beregning.fordeler.SatsBoetter
import no.nav.sokos.dare.poc.beregning.fordeler.Segmentgrunnlag
import no.nav.sokos.dare.poc.beregning.fordeler.SkatteKontekst
import no.nav.sokos.dare.poc.beregning.fordeler.TrekkvedtakForBeregning
import no.nav.sokos.dare.poc.beregning.fordeler.beregnInitielleSaldoer
import no.nav.sokos.dare.poc.beregning.fordeler.beregnInitielleSatserForTrekk
import no.nav.sokos.dare.poc.beregning.fordeler.filterRelaterteTilOppdrag
import no.nav.sokos.dare.poc.beregning.fordeler.makeTrekkPeriodeForBeregning
import no.nav.sokos.dare.poc.beregning.fordeler.relaterteBoundaries
import no.nav.sokos.dare.poc.oppdrag.OppdragId
import no.nav.sokos.dare.poc.oppdrag.YtelseKlasseKode
import no.nav.sokos.dare.poc.oppdrag.YtelseKlasseKodeRef
import no.nav.sokos.dare.poc.skattekort.domain.Forskuddstrekk
import no.nav.sokos.dare.poc.skattekort.domain.ForskuddstrekkKode
import no.nav.sokos.dare.poc.skattekort.domain.ForskuddstrekkType
import no.nav.sokos.dare.poc.skattekort.domain.Skattekort
import no.nav.sokos.dare.poc.skattetrekk.SkatteTabell
import no.nav.sokos.dare.poc.trekk.FagomraadeKode
import no.nav.sokos.dare.poc.trekk.TrekkAlternativ
import no.nav.sokos.dare.poc.trekk.TrekkvedtakId

class FellesBeregningsmotorTest :
    FunSpec({
        test("shared deduction and percentage tax pipeline preserves identity and consumes buckets") {
            val saldoTrekk =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.SALM,
                    type = TestObjects.TrekkType.YTELSESPERIODE_SKATTEREDUSERENDE,
                    perioder = listOf(fellesPeriode),
                    sats = BigDecimal("400"),
                    belopSaldo = BigDecimal("600"),
                )
            val oevrigTrekk =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.LOPD,
                    type = TestObjects.TrekkType.YTELSESPERIODE_IKKE_SKATTEREDUSERENDE,
                    perioder = listOf(fellesPeriode),
                    sats = BigDecimal("100"),
                )
            val trekk =
                listOf(saldoTrekk, oevrigTrekk).map { vedtak ->
                    val boetter = if (vedtak == saldoTrekk) listOf("2026-03") else fellesPeriode.hverdager.map { it.toString() }
                    TrekkvedtakForBeregning(vedtak, makeTrekkPeriodeForBeregning(vedtak.trekkperioder.single(), boetter))
                }
            val kontekst = DirekteGrunnlag(trekk = trekk)
            val trekkmotor = Trekkmotor()
            val scratchpad = kontekst.scratchpad

            val medTrekk: DirekteGrunnlag = trekkmotor.calculateSkattereduserendeTrekkFelles(kontekst)
            (medTrekk === kontekst) shouldBe true
            scratchpad.linjer().last().sum shouldBe BigDecimal("-400")
            kontekst.saldoBoetter.finnRestSaldo(saldoTrekk.id!!) shouldBe BigDecimal("200")
            kontekst.satsBoetter.restSats(
                trekk
                    .first()
                    .trekkperiode
                    .satsBoetteIds()
                    .single(),
            ) shouldBe BigDecimal.ZERO
            scratchpad.restKapasitet.values.sumOf { it } shouldBe BigDecimal("4600")

            val medSkatt: DirekteGrunnlag = Skattemotor().beregnSkatteTrekkFelles(medTrekk)
            (medSkatt === kontekst) shouldBe true
            scratchpad.linjer().last().sum shouldBe BigDecimal("-1380")

            val resultat: DirekteGrunnlag = trekkmotor.calculateOevrigeTrekkFelles(medSkatt)
            (resultat === kontekst) shouldBe true
            scratchpad.linjer().map { it.type } shouldBe
                listOf(DelberegningType.YTELSE, DelberegningType.TREKK, DelberegningType.SKATT, DelberegningType.TREKK)
            scratchpad.linjer().last().sum shouldBe BigDecimal("-500")
            scratchpad.restKapasitet.values.sumOf { it } shouldBe BigDecimal("2720")
            trekk
                .last()
                .trekkperiode
                .satsBoetteIds()
                .forEach { kontekst.satsBoetter.restSats(it) shouldBe BigDecimal.ZERO }
        }

        test("shared bounded frikort consumes its remainder and taxes only the excess") {
            val kontekst = DirekteGrunnlag(type = ForskuddstrekkType.FRIKORT, frikort = BigDecimal("1200"))

            (Skattemotor().beregnSkatteTrekkFelles(kontekst) === kontekst) shouldBe true

            kontekst.restFrikortBeloep shouldBe BigDecimal.ZERO
            kontekst.scratchpad
                .linjer()
                .single { it.type == DelberegningType.SKATT }
                .sum shouldBe BigDecimal("-1900")
            kontekst.scratchpad
                .linjer()
                .single { it.type == DelberegningType.FRIKORT_BRUK }
                .sum shouldBe BigDecimal("1200")
            kontekst.scratchpad.restKapasitet.values
                .sumOf { it } shouldBe BigDecimal("3100")
        }

        listOf(FagomraadeKode.AAP to 7, FagomraadeKode.SP to 5).forEach { (fagomraade, dager) ->
            test("shared table card uses direct daily values for $fagomraade") {
                val kontekst = DirekteGrunnlag(type = ForskuddstrekkType.TABELLKORT, fagomraade = fagomraade)
                val forventet = SkatteTabell.beregnTabellTrekk("8010", dager, BigDecimal("5000")).trekkbeloep

                (Skattemotor().beregnSkatteTrekkFelles(kontekst) === kontekst) shouldBe true

                val skatt = kontekst.scratchpad.linjer().single { it.type == DelberegningType.SKATT }
                skatt.satsType shouldBe Satstype.TABELL
                skatt.sum shouldBe -forventet
                kontekst.scratchpad.restKapasitet.values
                    .sumOf { it } shouldBe BigDecimal("5000") - forventet
                kontekst.scratchpad.ekstraInfo
                    .filterIsInstance<TekstEkstraInfo>()
                    .any { it.grunn == EkstraInfo.Reasons.SKATT_TABELL_VERDIBEREGNET_PARTISJON } shouldBe
                    (fagomraade == FagomraadeKode.AAP)
            }
        }

        test("shared earlier payment filtering retains only matching benefit class and prorates weekdays") {
            val kontekst = DirekteGrunnlag()
            val skattKlasse = LinjeKlassekode.Skatt("FSKTSKAT")
            val source =
                kontekst.scratchpad
                    .linjer()
                    .single()
                    .copy(beloep = BigDecimal("1"))
            val relatert =
                kontekst.relatert(
                    listOf(
                        Delberegning(
                            oppdragId = OppdragId(1),
                            datoIntervall = fellesPeriode,
                            linjer = listOf(source, source.copy(kodeKlasse = skattKlasse), source.copy(type = DelberegningType.TREKK)),
                        ),
                        Delberegning(oppdragId = OppdragId(2), datoIntervall = fellesPeriode, linjer = listOf(source)),
                    ),
                )
            val target = DatoIntervall("2026-03-02", "2026-03-03")

            val resultat = TidligereUtbetalinger.beregn(listOf(relatert), target, OppdragId(1), source.kodeKlasse!!)

            resultat.size shouldBe 1
            resultat.single().type shouldBe DelberegningType.TIDLIGERE_UTBETALT
            resultat.single().sum shouldBe BigDecimal("-2001")
            resultat
                .single()
                .ekstra
                .filterIsInstance<TekstEkstraInfo>()
                .single()
                .grunn shouldBe EkstraInfo.Reasons.TIDLIGERE_UTBETALT
            TidligereUtbetalinger.prorater(BigDecimal("5001"), fellesPeriode, target) shouldBe BigDecimal("2001")
            TidligereUtbetalinger.prorater(BigDecimal("5001"), fellesPeriode, DatoIntervall("2026-03-04", "2026-03-06")) shouldBe BigDecimal("3000")
            TidligereUtbetalinger.beregn(listOf(relatert), DatoIntervall("2026-03-09", "2026-03-10"), OppdragId(1), source.kodeKlasse!!) shouldBe emptyList()
        }

        test("scalar related calculation helpers select matching benefit periods without aggregates") {
            val kontekst = DirekteGrunnlag()
            val linje = kontekst.scratchpad.linjer().single()
            val relevant = Delberegning(oppdragId = OppdragId(1), datoIntervall = fellesPeriode, linjer = listOf(linje))
            val relatert =
                kontekst.relatert(
                    listOf(
                        relevant,
                        relevant.copy(oppdragId = OppdragId(2)),
                        relevant.copy(linjer = listOf(linje.copy(type = DelberegningType.TREKK))),
                    ),
                )

            relaterteBoundaries(listOf(relatert), setOf(OppdragId(1))) shouldBe listOf(fellesPeriode)
            filterRelaterteTilOppdrag(listOf(relatert), OppdragId(1), fellesPeriode) shouldBe listOf(relatert.copy(delberegning = listOf(relevant)))
            filterRelaterteTilOppdrag(listOf(relatert), OppdragId(3), fellesPeriode) shouldBe emptyList()
        }

        test("public bucket initializers subtract prior deductions from iterable decisions") {
            val vedtak =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.SALM,
                    type = TestObjects.TrekkType.YTELSESPERIODE_SKATTEREDUSERENDE,
                    perioder = listOf(fellesPeriode),
                    sats = BigDecimal("400"),
                    belopSaldo = BigDecimal("600"),
                )
            val trekk =
                TrekkvedtakForBeregning(
                    vedtak,
                    makeTrekkPeriodeForBeregning(vedtak.trekkperioder.single().copy(trekkvedtakId = vedtak.id!!.id), listOf("2026-03")),
                )
            val kontekst = DirekteGrunnlag()
            val relatert =
                kontekst.relatert(
                    listOf(
                        Delberegning(
                            oppdragId = OppdragId(1),
                            datoIntervall = fellesPeriode,
                            linjer =
                                listOf(
                                    Delberegningslinje(
                                        type = DelberegningType.TREKK,
                                        dagsbeloep = emptyList(),
                                        beloep = BigDecimal("-150"),
                                        trekkvedtakId = vedtak.id,
                                    ),
                                ),
                        ),
                    ),
                )

            beregnInitielleSaldoer(listOf(vedtak), listOf(relatert)) shouldBe mapOf(vedtak.id to BigDecimal("450"))
            beregnInitielleSatserForTrekk(setOf(trekk), listOf(relatert)) shouldBe
                mapOf(trekk.trekkperiode.satsBoetteIds().single() to BigDecimal("250"))
        }
    })

private val fellesPeriode = DatoIntervall("2026-03-02", "2026-03-06")
private val fellesBeregnet = LocalDateTime.parse("2026-03-06T12:00:00")

private class DirekteDelperiode(
    override val trekk: List<TrekkvedtakForBeregning>,
    override val fagomraade: FagomraadeKode,
) : Delperiodegrunnlag {
    override val oppdragId = OppdragId(1)
    override val periode = fellesPeriode
    override val konto = "konto"
    override val gjelder = AktoerId(1)
    override val beregnet = fellesBeregnet
    override val utbetalesTil: AktoerId? = null
    override val status = BeregningsStatus.BEREGNET
    override val ekstraInfo = mutableListOf<EkstraInfo>()
    override val overfoeringsdato: LocalDate = fellesPeriode.tom!!
    override val tidligereTrukneTrekkvedtakIMaaneden = emptySet<TrekkvedtakId>()
    override val relaterteBeregninger = emptyList<Beregning>()

    override fun forDag(dato: LocalDate): BigDecimal = if (dato in periode.hverdager) BigDecimal("1000") else BigDecimal.ZERO

    override fun vedtakSatsForDag(dato: LocalDate): BigDecimal = forDag(dato)

    override fun trekkgrunnlag(periode: DatoIntervall): BigDecimal = periode.hverdager.sumOf { forDag(it) }
}

private class DirekteSegment(
    delperiode: Delperiodegrunnlag,
) : Segmentgrunnlag {
    override val linjeGroupKey: LinjeGroupKey? = null
    override val delperioder = listOf(delperiode)
    override val ekstraInfo = mutableSetOf<EkstraInfo>()
    override val beregningsStatusflagg = mutableSetOf<BeregningsStatusflagg>()
    override val avventStatus: BeregningsStatus? = null
    override val avventOverfoeres: LocalDate? = null
    override val scratchpad = mutableMapOf(delperiode.oppdragId to BeregningsScratchpad())
}

private class DirekteGrunnlag(
    trekk: List<TrekkvedtakForBeregning> = emptyList(),
    type: ForskuddstrekkType = ForskuddstrekkType.PROSENTKORT,
    frikort: BigDecimal? = null,
    fagomraade: FagomraadeKode = FagomraadeKode.AAP,
) : Beregningsgrunnlag {
    private val delperiode = DirekteDelperiode(trekk, fagomraade)
    override val periode = fellesPeriode
    override val beregnet = fellesBeregnet
    override val segmenter = listOf(DirekteSegment(delperiode))
    override val skatteKontekst =
        SkatteKontekst(
            skattekort =
                Skattekort(
                    aktoerId = AktoerId(1),
                    inntektsaar = 2026,
                    kilde = "TEST",
                    identifikator = 1,
                    utstedtDato = LocalDate.of(2026, 1, 1),
                    forskuddsTrekk =
                        listOf(
                            Forskuddstrekk(
                                typeForskuddsTrekk = type,
                                trekkKode = ForskuddstrekkKode.LOENN_FRA_NAV,
                                prosentsats = BigDecimal("30"),
                                trekktabell = if (type == ForskuddstrekkType.TABELLKORT) "8010" else null,
                                frikortBelop = frikort?.toInt(),
                            ),
                        ),
                ),
            allokeringer = mapOf(ForskuddstrekkKode.LOENN_FRA_NAV to fagomraade.faggruppe),
        )
    override val satsBoetter = SatsBoetter(beregnInitielleSatserForTrekk(trekk, emptyList()))
    override val saldoBoetter = SaldoBoetter(trekk.mapNotNull { vedtak -> vedtak.belopSaldo?.let { vedtak.id to it } }.toMap())
    override var restFrikortBeloep = frikort
    val scratchpad = segmenter.single().scratchpad.getValue(delperiode.oppdragId)

    init {
        val dager = periode.dager.map { Dagbeloep(dato = it, beloep = delperiode.forDag(it)) }
        scratchpad.kapasitetStyring.addYtelse(dager)
        scratchpad.beregninger[periode] =
            mutableListOf(
                Delberegningslinje(
                    type = DelberegningType.YTELSE,
                    dagsbeloep = dager,
                    beloep = BigDecimal.ZERO,
                    sats = BigDecimal("1000"),
                    satsType = Satstype.DAG,
                    kodeKlasse =
                        LinjeKlassekode.Ytelse(
                            YtelseKlasseKode(YtelseKlasseKodeRef("TEST"), "Test benefit", true, true, listOf(fagomraade)),
                        ),
                ),
            )
    }

    override fun aktivDato(): LocalDate = periode.tom!!

    fun relatert(delberegninger: List<Delberegning>) =
        Beregning(
            periode = periode,
            konto = delperiode.konto,
            overfoeres = aktivDato(),
            fagomraade = delperiode.fagomraade,
            gjelder = delperiode.gjelder,
            beregnet = beregnet,
            delberegning = delberegninger,
        )
}

private fun BeregningsScratchpad.linjer() = beregninger.getValue(fellesPeriode)
