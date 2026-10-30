package no.nav.utbetaling.beregningsflyt

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

import no.nav.sokos.dare.poc.TestObjects
import no.nav.sokos.dare.poc.aktoer.AktoerId
import no.nav.sokos.dare.poc.beregning.Beregning
import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.beregning.Delberegning
import no.nav.sokos.dare.poc.beregning.DelberegningType
import no.nav.sokos.dare.poc.beregning.Delberegningslinje
import no.nav.sokos.dare.poc.beregning.EkstraInfo
import no.nav.sokos.dare.poc.beregning.LinjeKlassekode
import no.nav.sokos.dare.poc.beregning.TekstEkstraInfo
import no.nav.sokos.dare.poc.beregning.dataklasser.BeregningsStatus
import no.nav.sokos.dare.poc.beregning.dataklasser.Satstype
import no.nav.sokos.dare.poc.beregning.fordeler.FordelingInput
import no.nav.sokos.dare.poc.beregning.fordeler.SaldoBoetter
import no.nav.sokos.dare.poc.beregning.fordeler.SatsBoetter
import no.nav.sokos.dare.poc.beregning.fordeler.SkatteKontekst
import no.nav.sokos.dare.poc.oppdrag.OppdragId
import no.nav.sokos.dare.poc.oppdrag.OppdragslinjekjedeId
import no.nav.sokos.dare.poc.oppdrag.YtelseKlasseKode
import no.nav.sokos.dare.poc.oppdrag.YtelseKlasseKodeRef
import no.nav.sokos.dare.poc.skattekort.domain.Forskuddstrekk
import no.nav.sokos.dare.poc.skattekort.domain.ForskuddstrekkKode
import no.nav.sokos.dare.poc.skattekort.domain.ForskuddstrekkType
import no.nav.sokos.dare.poc.skattekort.domain.Skattekort
import no.nav.sokos.dare.poc.trekk.FagomraadeKode
import no.nav.sokos.dare.poc.trekk.TrekkAlternativ
import no.nav.utbetaling.beregning.oppdrag.Oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.Opplysning
import no.nav.utbetaling.beregning.oppdrag.Opplysninger
import no.nav.utbetaling.beregning.oppdrag.Personidentifikator
import no.nav.utbetaling.beregning.oppdrag.SaksbehandlerId
import no.nav.utbetaling.beregning.oppdrag.UtbetalesTil
import no.nav.utbetaling.beregning.tid.DefaultTidslinjesamling
import no.nav.utbetaling.beregning.tid.Periode.LukketPeriode
import no.nav.utbetaling.beregning.tid.Tidslinje
import no.nav.utbetaling.beregning.tid.Tidslinjesamling
import no.nav.utbetaling.beregningsflyt.beregning.Beregningsmotor
import no.nav.utbetaling.beregningsflyt.beregning.Ytelsesmotor
import no.nav.utbetaling.beregningsflyt.fordeler.BeregningsFordeler
import no.nav.utbetaling.beregningsflyt.identitet.Beregningsidentitet
import no.nav.utbetaling.beregningsflyt.identitet.Klassekodeoppslag
import no.nav.utbetaling.beregningsflyt.identitet.Oppdragsnokkel
import no.nav.utbetaling.beregningsflyt.kontekst.BeregningsSegment
import no.nav.utbetaling.beregningsflyt.kontekst.BeregningsmotorKontekst
import no.nav.utbetaling.beregningsflyt.kontekst.OppdragDelperiode

class TidslinjemotorTest :
    FunSpec({
        listOf(listOf(2, 3, 4), listOf(4, 3, 2), listOf(3, 4, 2)).forEach { kilderekkefolge ->
            test("limited tax exemption follows dates across recurring output groups with source order $kilderekkefolge") {
                val kilde = vekslendeKilde(kilderekkefolge)
                val opprinnelig = kilde.tidslinjer.mapValues { it.value.toList() }
                val identitet = IntegrasjonsIdentitet(kilde)
                val skatt =
                    prosentSkatt().let { kontekst ->
                        kontekst.copy(
                            skattekort =
                                kontekst.skattekort!!.copy(
                                    forskuddsTrekk =
                                        listOf(
                                            Forskuddstrekk(
                                                typeForskuddsTrekk = ForskuddstrekkType.FRIKORT,
                                                trekkKode = ForskuddstrekkKode.LOENN_FRA_NAV,
                                                frikortBelop = 150,
                                            ),
                                        ),
                                ),
                        )
                    }
                val fordeler = motorFordeler(identitet)

                fun bygg() = fordeler.byggBeregningsmotorKontekst(motorInput.copy(gjelder = AktoerId(1)), motorUke, kilde, emptyList(), skatt, emptyList(), BigDecimal("150"))
                val kontekst = bygg()
                val fersk = bygg()
                kontekst.beregningsrekkefolge().map { (_, dp) -> dp.periode.fom.dayOfMonth } shouldBe listOf(2, 3, 4)
                kontekst.beregningsrekkefolge().forEach { (segment, dp) ->
                    segment.delperioder.any { it === dp } shouldBe true
                    kontekst.segmenter.any { it === segment } shouldBe true
                }
                Beregningsmotor(Ytelsesmotor(identitet)).calculate(kontekst)

                kontekst.summerPerDato(DelberegningType.FRIKORT_BRUK) shouldBe listOf("100", "50", "0").map(::BigDecimal)
                kontekst.summerPerDato(DelberegningType.SKATT) shouldBe listOf("0", "-25", "-50").map(::BigDecimal)
                kontekst.segmenter.map { it.linjeGroupKey?.kid } shouldBe listOf("A", "B")
                kontekst.beregninger().map { it.delberegning.map { dp -> dp.datoIntervall.fom.dayOfMonth } } shouldBe listOf(listOf(2, 4), listOf(3))
                kontekst.segmenter.map { it.ekstraInfo.filterIsInstance<TekstEkstraInfo>().count { info -> info.grunn == EkstraInfo.Reasons.KID_ENDRET } } shouldBe
                    if (kilderekkefolge.first() == 3) listOf(1, 0) else listOf(0, 1)
                kontekst.segmenter
                    .flatMap { it.delperioder }
                    .flatMap { it.oppdrag.tidslinjer.keys }
                    .toSet() shouldBe kilde.tidslinjer.keys
                kilde.tidslinjer.mapValues { it.value.toList() } shouldBe opprinnelig
                kontekst.restFrikortBeloep shouldBe BigDecimal.ZERO
                fersk.restFrikortBeloep shouldBe BigDecimal("150")
                fersk.segmenter.all { it.scratchpad.isEmpty() } shouldBe true
                (kontekst.satsBoetter === fersk.satsBoetter) shouldBe false
                (kontekst.saldoBoetter === fersk.saldoBoetter) shouldBe false
            }
        }

        listOf(listOf(2, 3, 4), listOf(3, 4, 2)).forEach { kilderekkefolge ->
            test("overlapping date ties retain source group encounter order $kilderekkefolge") {
                val kilde = vekslendeKilde(kilderekkefolge, samtidig = true)
                val identitet = IntegrasjonsIdentitet(kilde)
                val kontekst =
                    motorFordeler(identitet).byggBeregningsmotorKontekst(
                        motorInput.copy(gjelder = AktoerId(1)),
                        motorUke,
                        kilde,
                        emptyList(),
                        ingenSkatt,
                        emptyList(),
                    )
                val forventet = if (kilderekkefolge.first() == 3) listOf("B", "A") else listOf("A", "B")
                kontekst.beregningsrekkefolge().map { (segment, _) -> segment.linjeGroupKey?.kid } shouldBe forventet
                kontekst.beregningsrekkefolge().map { (_, dp) -> dp.periode } shouldBe List(2) { DatoIntervall("2026-03-02", "2026-03-02") }
                Ytelsesmotor(identitet).beregnYtelse(kontekst)
                kontekst.segmenter.map {
                    it.scratchpad.values
                        .single()
                        .beregninger.values
                        .flatten()
                        .sumOf { linje -> linje.sum }
                } shouldBe
                    if (kilderekkefolge.first() == 3) listOf("100", "200").map(::BigDecimal) else listOf("200", "100").map(::BigDecimal)
            }
        }

        listOf(true, false).forEach { skattereduserende ->
            test("monthly deduction bucket follows dates across recurring groups tax reducing $skattereduserende") {
                val kilde = vekslendeKilde(listOf(4, 3, 2))
                val trekk =
                    TestObjects.trekkVedtak(
                        trekkalt = TrekkAlternativ.LOPM,
                        type = if (skattereduserende) TestObjects.TrekkType.YTELSESPERIODE_SKATTEREDUSERENDE else TestObjects.TrekkType.YTELSESPERIODE_IKKE_SKATTEREDUSERENDE,
                        perioder = listOf(motorUke),
                        sats = BigDecimal("150"),
                    )
                val kontekst = motorFordeler(IntegrasjonsIdentitet(kilde)).fordel(motorInput.copy(gjelder = AktoerId(1)), motorUke, kilde, listOf(trekk), prosentSkatt(), emptyList())
                kontekst.summerPerDato(DelberegningType.TREKK) shouldBe
                    (if (skattereduserende) listOf("-100", "-50", "0") else listOf("-70", "-70", "-10")).map(::BigDecimal)
                kontekst.summerPerDato(DelberegningType.SKATT) shouldBe
                    (if (skattereduserende) listOf("0", "-15", "-30") else listOf("-30", "-30", "-30")).map(::BigDecimal)
                kontekst.satsBoetter.satser.values
                    .single() shouldBe BigDecimal.ZERO
            }
        }

        test("five weekdays preserve source chain order, distinct identities, rates, classes and incremental capacity") {
            val kilde = motorKilde()
            val identitet = MotorIdentitet(kilde)
            val kontekst = motorFordeler(identitet).byggBeregningsmotorKontekst(motorInput, motorUke, kilde, emptyList(), ingenSkatt, emptyList())
            identitet.emitterte shouldBe emptyList()

            (Ytelsesmotor(identitet).beregnYtelse(kontekst) === kontekst) shouldBe true

            val linjer = kontekst.linjer()
            linjer.map { it.type } shouldBe listOf(DelberegningType.YTELSE, DelberegningType.YTELSE)
            linjer.map { it.sum } shouldBe listOf(BigDecimal("500"), BigDecimal("200"))
            linjer.map { it.oppdragslinjekjedeId } shouldBe listOf(OppdragslinjekjedeId(91), OppdragslinjekjedeId(17))
            identitet.emitterte shouldBe kilde.tidslinjer.keys.toList()
            linjer.map { it.kodeKlasse } shouldBe listOf(klasse("AAPUAA"), klasse("AAPNEW"))
            linjer.map { it.sats } shouldBe listOf(BigDecimal("100"), BigDecimal("40"))
            linjer.map { it.satsType } shouldBe listOf(Satstype.DAG, Satstype.DAG)
            linjer.map {
                it.kapasitet!!
                    .dagbeloep.values
                    .sumOf { belop -> belop }
            } shouldBe listOf(BigDecimal("500"), BigDecimal("700"))
            val dp =
                kontekst.segmenter
                    .single()
                    .delperioder
                    .single()
            dp.vedtakSatsForDag(motorUke.fom) shouldBe BigDecimal("170")
        }

        test("shared pipeline applies tax reducing deduction then percentage tax then ordinary deduction on the same context") {
            val kilde = motorKilde()
            val identitet = MotorIdentitet(kilde)
            val trekk =
                listOf(
                    TestObjects.trekkVedtak(
                        trekkalt = TrekkAlternativ.SALM,
                        type = TestObjects.TrekkType.YTELSESPERIODE_SKATTEREDUSERENDE,
                        perioder = listOf(motorUke),
                        sats = BigDecimal("100"),
                        belopSaldo = BigDecimal("200"),
                    ),
                    TestObjects.trekkVedtak(
                        trekkalt = TrekkAlternativ.LOPD,
                        type = TestObjects.TrekkType.YTELSESPERIODE_IKKE_SKATTEREDUSERENDE,
                        perioder = listOf(motorUke),
                        sats = BigDecimal("10"),
                    ),
                )
            val kontekst = motorFordeler(identitet).byggBeregningsmotorKontekst(motorInput, motorUke, kilde, trekk, prosentSkatt(), emptyList())

            (Beregningsmotor(Ytelsesmotor(identitet)).calculate(kontekst) === kontekst) shouldBe true

            kontekst.linjer().map { it.type } shouldBe
                listOf(DelberegningType.YTELSE, DelberegningType.YTELSE, DelberegningType.TREKK, DelberegningType.SKATT, DelberegningType.TREKK)
            kontekst.linjer().map { it.sum } shouldBe listOf("500", "200", "-100", "-180", "-50").map(::BigDecimal)
            kontekst.segmenter
                .single()
                .scratchpad.values
                .single()
                .restKapasitet.values
                .sumOf { it } shouldBe BigDecimal("370")
            kontekst.saldoBoetter.finnRestSaldo(trekk.first().id!!) shouldBe BigDecimal("100")
            kontekst.satsBoetter.satser.values
                .all { it.compareTo(BigDecimal.ZERO) == 0 } shouldBe true
        }

        test("distributor convenience method builds calculates and returns the context") {
            val kilde = motorKilde()
            val kontekst = BeregningsFordeler(MotorIdentitet(kilde), motorKlasser).fordel(motorInput, motorUke, kilde, emptyList(), ingenSkatt, emptyList())
            kontekst.linjer().filter { it.type == DelberegningType.YTELSE }.sumOf { it.sum } shouldBe BigDecimal("700")
            kontekst
                .beregninger()
                .single()
                .delberegning
                .single()
                .oppdragId shouldBe OppdragId(7)
        }

        test("weekend only benefit keeps a zero scalar and the empty daily convention") {
            val kilde = motorKilde()
            val periode = DatoIntervall("2026-03-07", "2026-03-08")
            val kontekst = motorFordeler(MotorIdentitet(kilde)).fordel(motorInput, periode, kilde, emptyList(), ingenSkatt, emptyList())
            kontekst.linjer().count { it.type == DelberegningType.YTELSE } shouldBe 2
            kontekst.linjer().filter { it.type == DelberegningType.YTELSE }.forEach {
                it.sum shouldBe BigDecimal.ZERO
                it.beloep shouldBe BigDecimal.ZERO
                it.dagsbeloep shouldBe emptyList()
                it.kapasitet!!
                    .dagbeloep.values
                    .all { belop -> belop == BigDecimal.ZERO } shouldBe true
            }
        }

        test("provenance-only midweek split emits exactly one benefit row after union coverage proof") {
            val kilde = oppdeltKilde("2026-03-03", "2026-03-04")
            kilde.tidslinjer.values
                .single()
                .count() shouldBe 2
            val identitet = MotorIdentitet(kilde)
            val kontekst = direkteKontekst(kilde, identitet)
            Ytelsesmotor(identitet).beregnYtelse(kontekst)
            kontekst
                .linjer()
                .filter { it.type == DelberegningType.YTELSE }
                .single()
                .sum shouldBe BigDecimal("500")
            kontekst.segmenter
                .single()
                .delperioder
                .map { it.periode } shouldBe listOf(motorUke)
        }

        test("public intake unit-only midweek split remains one calculated subperiod and does not mutate the source") {
            val kilde = motorKilde(single = true, enheter = true)
            val opprinnelig = kilde.tidslinjer.mapValues { it.value.toList() }
            kilde.tidslinjer.values
                .single()
                .count() shouldBe 2
            val kontekst = motorFordeler(MotorIdentitet(kilde)).fordel(motorInput, motorUke, kilde, emptyList(), ingenSkatt, emptyList())
            kontekst
                .linjer()
                .filter { it.type == DelberegningType.YTELSE }
                .single()
                .sum shouldBe BigDecimal("500")
            kontekst.segmenter
                .single()
                .delperioder
                .map { it.periode } shouldBe listOf(motorUke)
            kilde.tidslinjer.mapValues { it.value.toList() } shouldBe opprinnelig
        }

        test("motor emits chains in encounter order even for deliberately reverse-sorted UUIDs") {
            val tidslinjer = motorKilde().tidslinjer.values.toList()
            val kilde =
                DefaultTidslinjesamling(
                    linkedMapOf(
                        UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff") to tidslinjer[0],
                        UUID.fromString("00000000-0000-0000-0000-000000000001") to tidslinjer[1],
                    ),
                )
            val identitet = MotorIdentitet(kilde)
            val kontekst = direkteKontekst(kilde, identitet)
            Ytelsesmotor(identitet).beregnYtelse(kontekst)
            identitet.emitterte shouldBe kilde.tidslinjer.keys.toList()
            kontekst.linjer().map { it.sum } shouldBe listOf(BigDecimal("500"), BigDecimal("200"))
        }

        test("a gap inside a manually supplied subperiod rejects before chain emission") {
            val kilde = oppdeltKilde("2026-03-03", "2026-03-05")
            val identitet = MotorIdentitet(kilde)
            shouldThrow<IllegalArgumentException> { Ytelsesmotor(identitet).beregnYtelse(direkteKontekst(kilde, identitet)) }
            identitet.emitterte shouldBe emptyList()
        }

        test("calculation signature mismatch inside a manually supplied subperiod rejects before chain emission") {
            val kilde = oppdeltKilde("2026-03-03", "2026-03-04", BigDecimal("101"))
            val identitet = MotorIdentitet(kilde)
            shouldThrow<IllegalArgumentException> { Ytelsesmotor(identitet).beregnYtelse(direkteKontekst(kilde, identitet)) }
            identitet.emitterte shouldBe emptyList()
        }

        test("business assignment identity filters previous payments and preserves capping diagnostics and afterpayment marking") {
            val kilde = motorKilde()
            val identitet = MotorIdentitet(kilde)
            val kontekst =
                motorFordeler(identitet).byggBeregningsmotorKontekst(
                    motorInput,
                    motorUke,
                    kilde,
                    emptyList(),
                    ingenSkatt,
                    listOf(tidligereMotor(OppdragId(7), "800"), tidligereMotor(OppdragId(999), "9999")),
                )
            Ytelsesmotor(identitet).beregnYtelse(kontekst)
            kontekst.linjer().map { it.type } shouldBe listOf(DelberegningType.YTELSE, DelberegningType.TIDLIGERE_UTBETALT, DelberegningType.YTELSE)
            kontekst.linjer().map { it.sum } shouldBe listOf("500", "-800", "200").map(::BigDecimal)
            val scratchpad =
                kontekst.segmenter
                    .single()
                    .scratchpad.values
                    .single()
            scratchpad.restKapasitet.values.sumOf { it } shouldBe BigDecimal("200")
            // Legacy overwrites the marker for each encountered class, including classes without prior payment.
            scratchpad.erEtterbetaling[motorUke] shouldBe false
            scratchpad.ekstraInfo
                .filterIsInstance<TekstEkstraInfo>()
                .single()
                .grunn shouldBe EkstraInfo.Reasons.KREVER_TILBAKEKREVING
            kontekst
                .linjer()
                .last()
                .kapasitet!!
                .dagbeloep.values
                .sumOf { it } shouldBe BigDecimal("200")
        }

        test("prior payments prorate weekdays on a clipped subperiod and mark afterpayment") {
            val kilde = motorKilde(single = true)
            val identitet = MotorIdentitet(kilde)
            val periode = DatoIntervall("2026-03-02", "2026-03-03")
            val kontekst = motorFordeler(identitet).byggBeregningsmotorKontekst(motorInput, periode, kilde, emptyList(), ingenSkatt, listOf(tidligereMotor(OppdragId(7), "101")))
            Ytelsesmotor(identitet).beregnYtelse(kontekst)
            kontekst.linjer().map { it.sum } shouldBe listOf("200", "-41").map(::BigDecimal)
            val scratchpad =
                kontekst.segmenter
                    .single()
                    .scratchpad.values
                    .single()
            scratchpad.restKapasitet.values.sumOf { it } shouldBe BigDecimal("159")
            scratchpad.erEtterbetaling[periode] shouldBe true
        }
    })

private val motorUke = DatoIntervall("2026-03-02", "2026-03-06")
private val motorInput = FordelingInput("123", AktoerId(42), LocalDateTime.of(2026, 3, 10, 10, 0), null, BeregningsStatus.BEREGNET, mutableListOf(), LocalDate.parse("2026-03-15"))
private val ingenSkatt = SkatteKontekst(null, emptyMap())
private val motorKlasser = Klassekodeoppslag { YtelseKlasseKode(YtelseKlasseKodeRef(it.kode), "test", true, true, listOf(FagomraadeKode.AAP)) }

private fun klasse(kode: String) =
    LinjeKlassekode.Ytelse(
        motorKlasser.finn(
            no.nav.utbetaling.beregning.oppdrag
                .YtelseKlassekode(kode),
        ),
    )

private fun motorFordeler(identitet: Beregningsidentitet) = BeregningsFordeler(identitet, motorKlasser, Beregningsmotor(Ytelsesmotor(identitet)))

private fun BeregningsmotorKontekst.linjer() = segmenter.flatMap { it.scratchpad.values }.flatMap { it.beregninger.values }.flatten()

private fun BeregningsmotorKontekst.summerPerDato(type: DelberegningType) =
    beregninger().flatMap { it.delberegning }.sortedBy { it.datoIntervall.fom }.map { dp ->
        dp.linjer.filter { it.type == type }.sumOf { it.sum }
    }

private fun vekslendeKilde(
    dager: List<Int>,
    samtidig: Boolean = false,
): Oppdragstidslinje =
    lesTidslinje(
        """{"kodeAksjon":"1","kodeEndring":"NY","kodeFagomraade":"AAP","fagsystemId":"fagsystemId",
        "oppdragGjelderId":"11111111111","datoOppdragGjelderFom":"2000-01-01","saksbehId":"Z999999",
        "oppdragsLinje150":[${dager.joinToString(",") { dag ->
            """{"kodeEndringLinje":"NY","delytelseId":"$dag","kodeKlassifik":"AAPUAA",
            "datoVedtakFom":"2026-03-0${if (samtidig) 2 else dag}","datoVedtakTom":"2026-03-0${if (samtidig) 2 else dag}","sats":100,
            "kid":"${if (dag == 3) "B" else "A"}","fradragTillegg":"T","typeSats":"DAG","saksbehId":"Z999999"}"""
        }}]}""",
    )

private class MotorIdentitet(
    kilde: Tidslinjesamling<UUID, Opplysninger>,
) : Beregningsidentitet {
    val emitterte = mutableListOf<UUID>()
    private val kjeder =
        kilde.tidslinjer.keys
            .zip(listOf(OppdragslinjekjedeId(91), OppdragslinjekjedeId(17)))
            .toMap()

    override fun oppdragId(nokkel: Oppdragsnokkel) = OppdragId(7)

    override fun gjelderId(personidentifikator: Personidentifikator) = AktoerId(42)

    override fun mottakerId(utbetalesTil: UtbetalesTil) = AktoerId(42)

    override fun resultatKjedeId(kjedeId: UUID): OppdragslinjekjedeId {
        emitterte.add(kjedeId)
        return kjeder.getValue(kjedeId)
    }
}

private fun motorKilde(
    single: Boolean = false,
    enheter: Boolean = false,
): Oppdragstidslinje =
    lesTidslinje(
        """{"kodeAksjon":"1","kodeEndring":"NY","kodeFagomraade":"AAP","fagsystemId":"fagsystemId",
        "oppdragGjelderId":"11111111111","datoOppdragGjelderFom":"2000-01-01","saksbehId":"Z999999",
        "oppdragsLinje150":[
        {"kodeEndringLinje":"NY","delytelseId":"9","kodeKlassifik":"AAPUAA","datoVedtakFom":"2026-03-01","datoVedtakTom":"2026-03-31",
         "sats":100,"vedtakssats157":{"vedtakssats":120},"fradragTillegg":"T","typeSats":"DAG","saksbehId":"Z999999"}
        ${if (single) {
            ""
        } else {
            """,{"kodeEndringLinje":"NY","delytelseId":"2","kodeKlassifik":"AAPNEW","datoVedtakFom":"2026-03-01","datoVedtakTom":"2026-03-31",
         "sats":40,"vedtakssats157":{"vedtakssats":50},"fradragTillegg":"T","typeSats":"DAG","saksbehId":"Z999999"}"""
        }}
        ]${if (!enheter) {
            ""
        } else {
            ""","oppdragsEnhet120":[
        {"typeEnhet":"BOS","enhet":"1234","datoEnhetFom":"2026-03-01"},
        {"typeEnhet":"BOS","enhet":"5678","datoEnhetFom":"2026-03-04"}]"""
        }}}""",
    )

private fun oppdeltKilde(
    tom: String,
    fom: String,
    sats: BigDecimal = BigDecimal("100"),
): Tidslinjesamling<UUID, Opplysninger> {
    val (id, tidslinje) = motorKilde().tidslinjer.entries.first()
    val verdi = tidslinje.first().second
    return DefaultTidslinjesamling(
        mapOf(
            id to
                Tidslinje
                    .Builder<Opplysninger>()
                    .periode(LukketPeriode(motorUke.fom, LocalDate.parse(tom)), verdi)
                    .periode(LukketPeriode(LocalDate.parse(fom), motorUke.tom!!), verdi.copy(sats = Opplysning(SaksbehandlerId("Z888888"), verdi.sats.opplysning.copy(verdi = sats))))
                    .build(),
        ),
    )
}

private fun direkteKontekst(
    kilde: Tidslinjesamling<UUID, Opplysninger>,
    identitet: Beregningsidentitet,
): BeregningsmotorKontekst {
    val verdi =
        kilde.tidslinjer.values
            .first()
            .first()
            .second
    val dp =
        OppdragDelperiode(
            oppdrag = kilde,
            oppdragsnokkel = Oppdragsnokkel(verdi.fagområde, verdi.fagsystemId),
            oppdragId = OppdragId(7),
            fagomraade = FagomraadeKode.AAP,
            identitet = identitet,
            klassekoder = motorKlasser,
            periode = motorUke,
            konto = motorInput.konto,
            gjelder = motorInput.gjelder,
            beregnet = motorInput.beregnet,
            utbetalesTil = motorInput.gjelder,
            status = motorInput.status,
            trekk = emptyList(),
            overfoeringsdato = motorInput.overfoeringsdato,
            tidligereTrukneTrekkvedtakIMaaneden = emptySet(),
        )
    return BeregningsmotorKontekst(
        periode = motorUke,
        segmenter = listOf(BeregningsSegment(null, listOf(dp))),
        skatteKontekst = ingenSkatt,
        satsBoetter = SatsBoetter(emptyMap()),
        saldoBoetter = SaldoBoetter(emptyMap()),
        overføringsdato = motorInput.overfoeringsdato,
        beregnet = motorInput.beregnet,
    )
}

private fun tidligereMotor(
    id: OppdragId,
    belop: String,
) = Beregning(
    periode = motorUke,
    konto = motorInput.konto,
    fagomraade = FagomraadeKode.AAP,
    gjelder = motorInput.gjelder,
    beregnet = motorInput.beregnet,
    overfoeres = motorInput.overfoeringsdato,
    delberegning =
        listOf(
            Delberegning(
                oppdragId = id,
                datoIntervall = motorUke,
                linjer =
                    listOf(
                        Delberegningslinje(
                            type = DelberegningType.YTELSE,
                            dagsbeloep = emptyList(),
                            beloep = BigDecimal(belop),
                            kodeKlasse = klasse("AAPUAA"),
                        ),
                    ),
            ),
        ),
)

private fun prosentSkatt() =
    SkatteKontekst(
        Skattekort(
            aktoerId = AktoerId(42),
            inntektsaar = 2026,
            kilde = "TEST",
            identifikator = 1,
            utstedtDato = LocalDate.of(2026, 1, 1),
            forskuddsTrekk =
                listOf(
                    Forskuddstrekk(
                        typeForskuddsTrekk = ForskuddstrekkType.PROSENTKORT,
                        trekkKode = ForskuddstrekkKode.LOENN_FRA_NAV,
                        prosentsats = BigDecimal("30"),
                    ),
                ),
        ),
        mapOf(ForskuddstrekkKode.LOENN_FRA_NAV to FagomraadeKode.AAP.faggruppe),
    )
