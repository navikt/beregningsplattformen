package no.nav.sokos.dare.poc.beregning.fordeler

import java.math.BigDecimal
import java.time.LocalDateTime

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

import no.nav.sokos.dare.poc.TestObjects
import no.nav.sokos.dare.poc.TestUtil.oppdragFraFil
import no.nav.sokos.dare.poc.aktoer.AktoerId
import no.nav.sokos.dare.poc.beregning.Beregning
import no.nav.sokos.dare.poc.beregning.Dagbeloep
import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.beregning.Delberegning
import no.nav.sokos.dare.poc.beregning.DelberegningType
import no.nav.sokos.dare.poc.beregning.Delberegningslinje
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Beregningsmotor
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Skattemotor
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Trekkmotor
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Ytelsesmotor
import no.nav.sokos.dare.poc.beregning.dataklasser.BeregningsStatus
import no.nav.sokos.dare.poc.oppdrag.OppdragId
import no.nav.sokos.dare.poc.testFordelingInput
import no.nav.sokos.dare.poc.trekk.FagomraadeKode
import no.nav.sokos.dare.poc.trekk.TrekkAlternativ

class BeregningsFordelerBasicTest :
    FunSpec({

        val periode = DatoIntervall("2026-02-01", "2026-03-15")
        val oppdrag = listOf(oppdragFraFil("testdata/BeregningsFordelerBasicTest/enkel-ytelse-feb-til-mid-mar-2026.xml"))
        val input = testFordelingInput()

        test("byggBeregningsmotorKontekst skal produsere en BeregningsmotorKontekst med riktig struktur og verdier") {
            val result =
                BeregningsFordeler(
                    Beregningsmotor(Ytelsesmotor(), Trekkmotor(), Skattemotor()),
                ).byggBeregningsmotorKontekst(
                    periode = periode,
                    oppdrag = oppdrag,
                    trekk = emptyList(),
                    input = input,
                    skatteKontekst = SkatteKontekst(skattekort = null, allokeringer = emptyMap()),
                    relaterteBeregninger = emptyList(),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )

            result.periode shouldBe periode
            result.periode.fom.toString() shouldBe "2026-02-01"
            result.periode.tom.toString() shouldBe "2026-03-15"

            result.segmenter shouldHaveSize 1
            val segment = result.segmenter.first()
            segment.linjeGroupKey shouldBe LinjeGroupKey(utbetalesTilId = AktoerId(1), kid = null)
            segment.delperioder shouldHaveSize 1

            val foersteDelPeriode = segment.delperioder.first()
            foersteDelPeriode.periode shouldBe periode
            foersteDelPeriode.konto shouldBe ""
            foersteDelPeriode.gjelder shouldBe AktoerId(1)
            foersteDelPeriode.utbetalesTil shouldBe null
            foersteDelPeriode.status shouldBe BeregningsStatus.BEREGNET
            foersteDelPeriode.trekk shouldHaveSize 0
            foersteDelPeriode.ekstraInfo shouldHaveSize 0
            foersteDelPeriode.overfoeringsdato shouldNotBe null

            foersteDelPeriode.oppdrag shouldBe oppdrag.first()

            result.satsBoetter shouldBe SatsBoetter(emptyMap())
            result.saldoBoetter shouldBe SaldoBoetter(emptyMap())
        }

        test("byggBeregningsmotorKontekst med ett trekkvedtak med to trekkperioder oppretter ett segment med to delperioder") {
            val periode1 = DatoIntervall("2026-02-01", "2026-02-28")
            val periode2 = DatoIntervall("2026-03-01", "2026-03-15")

            val trekkvedtak =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.LOPM,
                    type = TestObjects.TrekkType.YTELSESPERIODE_SKATTEREDUSERENDE,
                    perioder = listOf(periode1, periode2),
                    sats = BigDecimal("1000"),
                )

            val periode = DatoIntervall("2026-02-01", "2026-03-15")
            val oppdrag = listOf(oppdragFraFil("testdata/BeregningsFordelerBasicTest/enkel-ytelse-feb-til-mid-mar-2026.xml"))
            val trekk = listOf(trekkvedtak)
            val input = testFordelingInput()

            val result =
                BeregningsFordeler(
                    Beregningsmotor(Ytelsesmotor(), Trekkmotor(), Skattemotor()),
                ).byggBeregningsmotorKontekst(
                    periode = periode,
                    oppdrag = oppdrag,
                    trekk = trekk,
                    input = input,
                    skatteKontekst = SkatteKontekst(skattekort = null, allokeringer = emptyMap()),
                    relaterteBeregninger = emptyList(),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )

            result.periode shouldBe periode

            result.segmenter shouldHaveSize 1
            val segment = result.segmenter.first()
            segment.linjeGroupKey shouldBe LinjeGroupKey(utbetalesTilId = AktoerId(1), kid = null)
            segment.delperioder shouldHaveSize 2

            val dp1 = segment.delperioder[0]
            dp1.periode shouldBe periode1
            dp1.konto shouldBe ""
            dp1.gjelder shouldBe AktoerId(1)
            dp1.status shouldBe BeregningsStatus.BEREGNET

            val dp2 = segment.delperioder[1]
            dp2.periode shouldBe periode2
            dp2.konto shouldBe ""
            dp2.gjelder shouldBe AktoerId(1)
            dp2.status shouldBe BeregningsStatus.BEREGNET
            result.saldoBoetter.finnRestSaldo(trekkvedtak.id!!) shouldBe null
        }

        test("byggBeregningsmotorKontekst med trekkvedtak med belopSaldo populerer saldoBoetter") {
            val vedtak =
                TestObjects
                    .trekkVedtak(
                        trekkalt = TrekkAlternativ.SALM,
                        type = TestObjects.TrekkType.YTELSESPERIODE_SKATTEREDUSERENDE,
                        perioder = listOf(DatoIntervall("2026-02-01", "2026-02-28")),
                        sats = BigDecimal("1000"),
                    ).copy(belopSaldo = BigDecimal("5000"))

            val periode = DatoIntervall("2026-02-01", "2026-02-28")
            val oppdrag = listOf(oppdragFraFil("testdata/BeregningsFordelerBasicTest/enkel-ytelse-feb-til-mid-mar-2026.xml"))
            val trekk = listOf(vedtak)
            val input = testFordelingInput()

            val result =
                BeregningsFordeler(
                    Beregningsmotor(Ytelsesmotor(), Trekkmotor(), Skattemotor()),
                ).byggBeregningsmotorKontekst(
                    periode = periode,
                    oppdrag = oppdrag,
                    trekk = trekk,
                    input = input,
                    skatteKontekst = SkatteKontekst(skattekort = null, allokeringer = emptyMap()),
                    relaterteBeregninger = emptyList(),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )

            result.saldoBoetter.finnRestSaldo(vedtak.id!!) shouldBe BigDecimal("5000")
        }

        test("byggBeregningsmotorKontekst med trekkvedtak uten belopSaldo populerer ikke saldoBoetter") {
            val vedtak =
                TestObjects
                    .trekkVedtak(
                        trekkalt = TrekkAlternativ.LOPM,
                        type = TestObjects.TrekkType.YTELSESPERIODE_SKATTEREDUSERENDE,
                        perioder = listOf(DatoIntervall("2026-02-01", "2026-02-28")),
                        sats = BigDecimal("1000"),
                    ).copy(belopSaldo = null)

            val periode = DatoIntervall("2026-02-01", "2026-02-28")
            val oppdrag = listOf(oppdragFraFil("testdata/BeregningsFordelerBasicTest/enkel-ytelse-feb-til-mid-mar-2026.xml"))
            val trekk = listOf(vedtak)
            val input = testFordelingInput()

            val result =
                BeregningsFordeler(
                    Beregningsmotor(Ytelsesmotor(), Trekkmotor(), Skattemotor()),
                ).byggBeregningsmotorKontekst(
                    periode = periode,
                    oppdrag = oppdrag,
                    trekk = trekk,
                    input = input,
                    skatteKontekst = SkatteKontekst(skattekort = null, allokeringer = emptyMap()),
                    relaterteBeregninger = emptyList(),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )

            result.saldoBoetter.finnRestSaldo(vedtak.id!!) shouldBe null
        }

        test("byggBeregningsmotorKontekst trekker fra relaterteBeregninger for saldoBoetter startverdier") {
            val vedtak =
                TestObjects
                    .trekkVedtak(
                        trekkalt = TrekkAlternativ.SALM,
                        type = TestObjects.TrekkType.YTELSESPERIODE_SKATTEREDUSERENDE,
                        perioder = listOf(DatoIntervall("2026-02-01", "2026-02-28")),
                        sats = BigDecimal("1000"),
                    ).copy(belopSaldo = BigDecimal("5000"))

            val relatertBeregning =
                Beregning(
                    periode = DatoIntervall("2026-01-01", "2026-01-31"),
                    konto = "",
                    overfoeres = java.time.LocalDate.of(2026, 1, 31),
                    fagomraade = FagomraadeKode.AAP,
                    gjelder = AktoerId(1),
                    beregnet = java.time.LocalDateTime.of(2026, 1, 31, 12, 0),
                    delberegning =
                        listOf(
                            Delberegning(
                                linjer =
                                    listOf(
                                        Delberegningslinje(
                                            type = DelberegningType.TREKK,
                                            dagsbeloep = listOf(Dagbeloep(dato = java.time.LocalDate.of(2026, 1, 15), beloep = BigDecimal.ZERO)),
                                            beloep = BigDecimal("-1500"),
                                            trekkvedtakId = vedtak.id,
                                        ),
                                    ),
                                datoIntervall = DatoIntervall("2026-01-15", "2026-01-15"),
                            ),
                        ),
                )

            val periode = DatoIntervall("2026-02-01", "2026-02-28")
            val oppdrag = listOf(oppdragFraFil("testdata/BeregningsFordelerBasicTest/enkel-ytelse-feb-til-mid-mar-2026.xml"))
            val trekk = listOf(vedtak)
            val input = testFordelingInput()

            val fordeler = BeregningsFordeler(Beregningsmotor(Ytelsesmotor(), Trekkmotor(), Skattemotor()))
            val result =
                fordeler.byggBeregningsmotorKontekst(
                    periode = periode,
                    oppdrag = oppdrag,
                    trekk = trekk,
                    input = input,
                    skatteKontekst = SkatteKontekst(skattekort = null, allokeringer = emptyMap()),
                    relaterteBeregninger = listOf(relatertBeregning),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )

            result.saldoBoetter.finnRestSaldo(vedtak.id!!) shouldBe BigDecimal("3500")
        }

        test("byggBeregningsmotorKontekst summerer flere TREKK fra relaterteBeregninger for samme vedtak") {
            val vedtak =
                TestObjects
                    .trekkVedtak(
                        trekkalt = TrekkAlternativ.SALM,
                        type = TestObjects.TrekkType.YTELSESPERIODE_SKATTEREDUSERENDE,
                        perioder = listOf(DatoIntervall("2026-03-01", "2026-03-31")),
                        sats = BigDecimal("1000"),
                    ).copy(belopSaldo = BigDecimal("5000"))

            val relatertBeregning1 =
                Beregning(
                    periode = DatoIntervall("2026-01-01", "2026-01-31"),
                    konto = "",
                    overfoeres = java.time.LocalDate.of(2026, 1, 31),
                    fagomraade = FagomraadeKode.AAP,
                    gjelder = AktoerId(1),
                    beregnet = java.time.LocalDateTime.of(2026, 1, 31, 12, 0),
                    delberegning =
                        listOf(
                            Delberegning(
                                linjer =
                                    listOf(
                                        Delberegningslinje(
                                            type = DelberegningType.TREKK,
                                            dagsbeloep = listOf(Dagbeloep(dato = java.time.LocalDate.of(2026, 1, 15), beloep = BigDecimal.ZERO)),
                                            beloep = BigDecimal("-800"),
                                            trekkvedtakId = vedtak.id,
                                        ),
                                    ),
                                datoIntervall = DatoIntervall("2026-01-15", "2026-01-15"),
                            ),
                        ),
                )

            val relatertBeregning2 =
                Beregning(
                    periode = DatoIntervall("2026-02-01", "2026-02-28"),
                    konto = "",
                    overfoeres = java.time.LocalDate.of(2026, 2, 28),
                    fagomraade = FagomraadeKode.AAP,
                    gjelder = AktoerId(1),
                    beregnet = java.time.LocalDateTime.of(2026, 2, 28, 12, 0),
                    delberegning =
                        listOf(
                            Delberegning(
                                linjer =
                                    listOf(
                                        Delberegningslinje(
                                            type = DelberegningType.TREKK,
                                            dagsbeloep = listOf(Dagbeloep(dato = java.time.LocalDate.of(2026, 2, 15), beloep = BigDecimal.ZERO)),
                                            beloep = BigDecimal("-700"),
                                            trekkvedtakId = vedtak.id,
                                        ),
                                    ),
                                datoIntervall = DatoIntervall("2026-02-15", "2026-02-15"),
                            ),
                        ),
                )

            val periode = DatoIntervall("2026-03-01", "2026-03-31")
            val oppdrag = listOf(oppdragFraFil("testdata/BeregningsFordelerBasicTest/enkel-ytelse-feb-til-mid-mar-2026.xml"))
            val trekk = listOf(vedtak)
            val input = testFordelingInput()

            val fordeler = BeregningsFordeler(Beregningsmotor(Ytelsesmotor(), Trekkmotor(), Skattemotor()))
            val result =
                fordeler.byggBeregningsmotorKontekst(
                    periode = periode,
                    oppdrag = oppdrag,
                    trekk = trekk,
                    input = input,
                    skatteKontekst = SkatteKontekst(skattekort = null, allokeringer = emptyMap()),
                    relaterteBeregninger = listOf(relatertBeregning1, relatertBeregning2),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )

            // 5000 - (800 + 700) = 3500
            result.saldoBoetter.finnRestSaldo(vedtak.id!!) shouldBe BigDecimal("3500")
        }

        test("byggBeregningsmotorKontekst ignorerer TREKK for andre trekkvedtakId i relaterteBeregninger") {
            val vedtak =
                TestObjects
                    .trekkVedtak(
                        trekkalt = TrekkAlternativ.SALM,
                        type = TestObjects.TrekkType.YTELSESPERIODE_SKATTEREDUSERENDE,
                        perioder = listOf(DatoIntervall("2026-02-01", "2026-02-28")),
                        sats = BigDecimal("1000"),
                    ).copy(belopSaldo = BigDecimal("5000"))

            val annetVedtakId =
                no.nav.sokos.dare.poc.trekk
                    .TrekkvedtakId(9999)

            val relatertBeregning =
                Beregning(
                    periode = DatoIntervall("2026-01-01", "2026-01-31"),
                    konto = "",
                    overfoeres = java.time.LocalDate.of(2026, 1, 31),
                    fagomraade = FagomraadeKode.AAP,
                    gjelder = AktoerId(1),
                    beregnet = java.time.LocalDateTime.of(2026, 1, 31, 12, 0),
                    delberegning =
                        listOf(
                            Delberegning(
                                linjer =
                                    listOf(
                                        Delberegningslinje(
                                            type = DelberegningType.TREKK,
                                            dagsbeloep = listOf(Dagbeloep(dato = java.time.LocalDate.of(2026, 1, 15), beloep = BigDecimal.ZERO)),
                                            beloep = BigDecimal("-2000"),
                                            trekkvedtakId = annetVedtakId,
                                        ),
                                    ),
                                datoIntervall = DatoIntervall("2026-01-15", "2026-01-15"),
                            ),
                        ),
                )

            val periode = DatoIntervall("2026-02-01", "2026-02-28")
            val oppdrag = listOf(oppdragFraFil("testdata/BeregningsFordelerBasicTest/enkel-ytelse-feb-til-mid-mar-2026.xml"))
            val trekk = listOf(vedtak)
            val input = testFordelingInput()

            val fordeler = BeregningsFordeler(Beregningsmotor(Ytelsesmotor(), Trekkmotor(), Skattemotor()))
            val result =
                fordeler.byggBeregningsmotorKontekst(
                    periode = periode,
                    oppdrag = oppdrag,
                    trekk = trekk,
                    input = input,
                    skatteKontekst = SkatteKontekst(skattekort = null, allokeringer = emptyMap()),
                    relaterteBeregninger = listOf(relatertBeregning),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )

            // TREKK for annet vedtak should not affect this vedtak's saldo
            result.saldoBoetter.finnRestSaldo(vedtak.id!!) shouldBe BigDecimal("5000")
        }

        test("byggBeregningsmotorKontekst ignorerer ikke-TREKK linjer i relaterteBeregninger") {
            val vedtak =
                TestObjects
                    .trekkVedtak(
                        trekkalt = TrekkAlternativ.SALM,
                        type = TestObjects.TrekkType.YTELSESPERIODE_SKATTEREDUSERENDE,
                        perioder = listOf(DatoIntervall("2026-02-01", "2026-02-28")),
                        sats = BigDecimal("1000"),
                    ).copy(belopSaldo = BigDecimal("5000"))

            val relatertBeregning =
                Beregning(
                    periode = DatoIntervall("2026-01-01", "2026-01-31"),
                    konto = "",
                    overfoeres = java.time.LocalDate.of(2026, 1, 31),
                    fagomraade = FagomraadeKode.AAP,
                    gjelder = AktoerId(1),
                    beregnet = java.time.LocalDateTime.of(2026, 1, 31, 12, 0),
                    delberegning =
                        listOf(
                            Delberegning(
                                linjer =
                                    listOf(
                                        Delberegningslinje(
                                            type = DelberegningType.YTELSE,
                                            dagsbeloep = listOf(Dagbeloep(dato = java.time.LocalDate.of(2026, 1, 15), beloep = BigDecimal("500"))),
                                            beloep = BigDecimal("500"),
                                            trekkvedtakId = vedtak.id,
                                        ),
                                    ),
                                datoIntervall = DatoIntervall("2026-01-15", "2026-01-15"),
                            ),
                        ),
                )

            val periode = DatoIntervall("2026-02-01", "2026-02-28")
            val oppdrag = listOf(oppdragFraFil("testdata/BeregningsFordelerBasicTest/enkel-ytelse-feb-til-mid-mar-2026.xml"))
            val trekk = listOf(vedtak)
            val input = testFordelingInput()

            val fordeler = BeregningsFordeler(Beregningsmotor(Ytelsesmotor(), Trekkmotor(), Skattemotor()))
            val result =
                fordeler.byggBeregningsmotorKontekst(
                    periode = periode,
                    oppdrag = oppdrag,
                    trekk = trekk,
                    input = input,
                    skatteKontekst = SkatteKontekst(skattekort = null, allokeringer = emptyMap()),
                    relaterteBeregninger = listOf(relatertBeregning),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )

            // YTELSE lines should not count as deducted
            result.saldoBoetter.finnRestSaldo(vedtak.id!!) shouldBe BigDecimal("5000")
        }

        test("byggBeregningsmotorKontekst med tom relaterteBeregninger gir full saldo") {
            val vedtak =
                TestObjects
                    .trekkVedtak(
                        trekkalt = TrekkAlternativ.SALM,
                        type = TestObjects.TrekkType.YTELSESPERIODE_SKATTEREDUSERENDE,
                        perioder = listOf(DatoIntervall("2026-02-01", "2026-02-28")),
                        sats = BigDecimal("1000"),
                    ).copy(belopSaldo = BigDecimal("5000"))

            val periode = DatoIntervall("2026-02-01", "2026-02-28")
            val oppdrag = listOf(oppdragFraFil("testdata/BeregningsFordelerBasicTest/enkel-ytelse-feb-til-mid-mar-2026.xml"))
            val trekk = listOf(vedtak)
            val input = testFordelingInput()

            val fordeler = BeregningsFordeler(Beregningsmotor(Ytelsesmotor(), Trekkmotor(), Skattemotor()))
            val result =
                fordeler.byggBeregningsmotorKontekst(
                    periode = periode,
                    oppdrag = oppdrag,
                    trekk = trekk,
                    input = input,
                    skatteKontekst = SkatteKontekst(skattekort = null, allokeringer = emptyMap()),
                    relaterteBeregninger = emptyList(),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )

            result.saldoBoetter.finnRestSaldo(vedtak.id!!) shouldBe BigDecimal("5000")
        }

        test("byggBeregningsmotorKontekst med flere oppdrag sorterer delperioder etter oppdrag-id og deretter periode") {
            val oppdrag1 =
                oppdragFraFil("testdata/BeregningsFordelerBasicTest/enkel-ytelse-feb-til-mid-mar-2026.xml").let {
                    it.copy(id = OppdragId(10))
                }
            val oppdrag2 =
                oppdragFraFil("testdata/BeregningsFordelerBasicTest/enkel-ytelse-feb-til-mid-mar-2026.xml").let {
                    it.copy(id = OppdragId(5))
                }

            val vedtak =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.LOPM,
                    type = TestObjects.TrekkType.YTELSESPERIODE_SKATTEREDUSERENDE,
                    perioder = listOf(DatoIntervall("2026-02-01", "2026-03-15")),
                    sats = BigDecimal("1000"),
                )

            val periode = DatoIntervall("2026-02-01", "2026-03-15")
            val oppdrag = listOf(oppdrag1, oppdrag2)
            val trekk = listOf(vedtak)
            val input = testFordelingInput()

            val result =
                BeregningsFordeler(
                    Beregningsmotor(Ytelsesmotor(), Trekkmotor(), Skattemotor()),
                ).byggBeregningsmotorKontekst(
                    periode = periode,
                    oppdrag = oppdrag,
                    trekk = trekk,
                    input = input,
                    skatteKontekst = SkatteKontekst(skattekort = null, allokeringer = emptyMap()),
                    relaterteBeregninger = emptyList(),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )

            result.segmenter shouldHaveSize 1
            val segment = result.segmenter.first()
            // 2 oppdrag × 2 months = 4 delperioder
            segment.delperioder shouldHaveSize 4

            // Sorted by oppdrag.id (5 before 10), then by periode
            segment.delperioder[0]
                .oppdrag.id shouldBe OppdragId(5)
            segment.delperioder[0].periode shouldBe DatoIntervall("2026-02-01", "2026-02-28")

            segment.delperioder[1]
                .oppdrag.id shouldBe OppdragId(5)
            segment.delperioder[1].periode shouldBe DatoIntervall("2026-03-01", "2026-03-15")

            segment.delperioder[2]
                .oppdrag.id shouldBe OppdragId(10)
            segment.delperioder[2].periode shouldBe DatoIntervall("2026-02-01", "2026-02-28")

            segment.delperioder[3]
                .oppdrag.id shouldBe OppdragId(10)
            segment.delperioder[3].periode shouldBe DatoIntervall("2026-03-01", "2026-03-15")
        }
        // testFordelingInput default overfoeringsdato is 2025-04-01
        test("UTBDATO trekkvedtak should be included when trekkperiode contains overfoeringsdato") {
            val vedtak =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.LOPD,
                    type = TestObjects.TrekkType.UTBETALINGSDATO_IKKE_SKATTEREDUSERENDE,
                    perioder = listOf(DatoIntervall("2025-03-15", "2025-04-15")),
                    sats = BigDecimal("200"),
                )

            val periode = DatoIntervall("2025-03-15", "2025-04-15")
            val oppdrag = listOf(oppdragFraFil("testdata/BeregningsFordelerBasicTest/enkel-ytelse-mars-april-2025.xml"))
            val trekk = listOf(vedtak)
            val input = testFordelingInput()

            val fordeler = BeregningsFordeler(Beregningsmotor(Ytelsesmotor(), Trekkmotor(), Skattemotor()))
            val result =
                fordeler.byggBeregningsmotorKontekst(
                    periode = periode,
                    oppdrag = oppdrag,
                    trekk = trekk,
                    input = input,
                    skatteKontekst = SkatteKontekst(skattekort = null, allokeringer = emptyMap()),
                    relaterteBeregninger = emptyList(),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )

            val segment = result.segmenter.first()
            segment.delperioder
                .first()
                .trekk shouldHaveSize 1
        }

        test("UTBDATO trekkvedtak should be excluded when trekkperiode does not contain overfoeringsdato") {
            val vedtak =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.LOPD,
                    type = TestObjects.TrekkType.UTBETALINGSDATO_IKKE_SKATTEREDUSERENDE,
                    perioder = listOf(DatoIntervall("2025-04-02", "2025-04-30")),
                    sats = BigDecimal("200"),
                )

            val periode = DatoIntervall("2025-04-02", "2025-04-30")
            val oppdrag = listOf(oppdragFraFil("testdata/BeregningsFordelerBasicTest/enkel-ytelse-mars-april-2025.xml"))
            val trekk = listOf(vedtak)
            val input = testFordelingInput()

            val fordeler = BeregningsFordeler(Beregningsmotor(Ytelsesmotor(), Trekkmotor(), Skattemotor()))
            val result =
                fordeler.byggBeregningsmotorKontekst(
                    periode = periode,
                    oppdrag = oppdrag,
                    trekk = trekk,
                    input = input,
                    skatteKontekst = SkatteKontekst(skattekort = null, allokeringer = emptyMap()),
                    relaterteBeregninger = emptyList(),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )

            val segment = result.segmenter.first()
            segment.delperioder
                .first()
                .trekk shouldHaveSize 0
        }

        test("YTELPER trekkvedtak should still use overlap-based filtering") {
            val vedtak =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.LOPD,
                    type = TestObjects.TrekkType.YTELSESPERIODE_SKATTEREDUSERENDE,
                    perioder = listOf(DatoIntervall("2025-04-02", "2025-04-30")),
                    sats = BigDecimal("200"),
                )

            val periode = DatoIntervall("2025-04-02", "2025-04-30")
            val oppdrag = listOf(oppdragFraFil("testdata/BeregningsFordelerBasicTest/enkel-ytelse-mars-april-2025.xml"))
            val trekk = listOf(vedtak)
            val input = testFordelingInput()

            val fordeler = BeregningsFordeler(Beregningsmotor(Ytelsesmotor(), Trekkmotor(), Skattemotor()))
            val result =
                fordeler.byggBeregningsmotorKontekst(
                    periode = periode,
                    oppdrag = oppdrag,
                    trekk = trekk,
                    input = input,
                    skatteKontekst = SkatteKontekst(skattekort = null, allokeringer = emptyMap()),
                    relaterteBeregninger = emptyList(),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )

            val segment = result.segmenter.first()
            segment.delperioder
                .first()
                .trekk shouldHaveSize 1
        }
    })
