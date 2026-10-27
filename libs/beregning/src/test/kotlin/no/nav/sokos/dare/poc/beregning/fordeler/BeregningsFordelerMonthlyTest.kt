package no.nav.sokos.dare.poc.beregning.fordeler

import java.math.BigDecimal
import java.time.LocalDateTime

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

import no.nav.sokos.dare.poc.TestObjects
import no.nav.sokos.dare.poc.TestUtil.oppdragFraFil
import no.nav.sokos.dare.poc.aktoer.AktoerId
import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Beregningsmotor
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Skattemotor
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Trekkmotor
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Ytelsesmotor
import no.nav.sokos.dare.poc.testFordelingInput
import no.nav.sokos.dare.poc.trekk.TrekkAlternativ

class BeregningsFordelerMonthlyTest :
    FunSpec({
        val defaultTestFile = "testdata/BeregningsFordelerBasicTest/enkel-ytelse-feb-til-mid-mar-2026.xml"
        test("byggBeregningsmotorKontekst med månedlig satstype trekkperiode som spenner over to måneder oppretter én segment med to månedlige delperioder") {
            val vedtak =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.LOPM,
                    type = TestObjects.TrekkType.YTELSESPERIODE_SKATTEREDUSERENDE,
                    perioder = listOf(DatoIntervall("2026-02-01", "2026-03-15")),
                    sats = BigDecimal("1000"),
                )
            val periode = DatoIntervall("2026-02-01", "2026-03-15")
            val oppdrag = listOf(oppdragFraFil(defaultTestFile))
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
            segment.linjeGroupKey shouldBe LinjeGroupKey(utbetalesTilId = AktoerId(1), kid = null)
            segment.delperioder shouldHaveSize 2

            segment.delperioder[0].periode shouldBe DatoIntervall("2026-02-01", "2026-02-28")
            segment.delperioder[0].trekk shouldHaveSize 1
            segment.delperioder[0]
                .trekk
                .first()
                .trekkperiode.boetter shouldBe listOf("2026-02")

            segment.delperioder[1].periode shouldBe DatoIntervall("2026-03-01", "2026-03-15")
            segment.delperioder[1].trekk shouldHaveSize 1
            segment.delperioder[1]
                .trekk
                .first()
                .trekkperiode.boetter shouldBe listOf("2026-03")

            val trekkperiodeId = vedtak.trekkperioder.first().id!!
            result.satsBoetter.restSats(SatsBoetteId(trekkperiodeId, "2026-02")) shouldBe BigDecimal("1000")
            result.satsBoetter.restSats(SatsBoetteId(trekkperiodeId, "2026-03")) shouldBe BigDecimal("1000")
        }

        test("byggBeregningsmotorKontekst med månedlig satstype trekkperiode som spenner over tre måneder oppretter tre delperioder") {
            val vedtak =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.LOPM,
                    type = TestObjects.TrekkType.YTELSESPERIODE_SKATTEREDUSERENDE,
                    perioder = listOf(DatoIntervall("2026-01-15", "2026-03-10")),
                    sats = BigDecimal("500"),
                )
            val periode = DatoIntervall("2026-01-15", "2026-03-10")
            val oppdrag = listOf(oppdragFraFil("testdata/BeregningsFordelerBasicTest/enkel-ytelse-jan-til-mid-mar-2026.xml"))
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
            segment.linjeGroupKey shouldBe LinjeGroupKey(utbetalesTilId = AktoerId(1), kid = null)
            segment.delperioder shouldHaveSize 3

            segment.delperioder[0].periode shouldBe DatoIntervall("2026-01-15", "2026-01-31")
            segment.delperioder[0]
                .trekk
                .first()
                .trekkperiode.boetter shouldBe listOf("2026-01")

            segment.delperioder[1].periode shouldBe DatoIntervall("2026-02-01", "2026-02-28")
            segment.delperioder[1]
                .trekk
                .first()
                .trekkperiode.boetter shouldBe listOf("2026-02")

            segment.delperioder[2].periode shouldBe DatoIntervall("2026-03-01", "2026-03-10")
            segment.delperioder[2]
                .trekk
                .first()
                .trekkperiode.boetter shouldBe listOf("2026-03")
        }

        test("byggBeregningsmotorKontekst med ikke-månedlig satstype trekkperiode som spenner over flere måneder splitter ikke") {
            val vedtak =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.LOPP,
                    type = TestObjects.TrekkType.YTELSESPERIODE_SKATTEREDUSERENDE,
                    perioder = listOf(DatoIntervall("2026-02-01", "2026-03-15")),
                    sats = BigDecimal("50"),
                )
            val periode = DatoIntervall("2026-02-01", "2026-03-15")
            val oppdrag = listOf(oppdragFraFil(defaultTestFile))
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
            segment.linjeGroupKey shouldBe LinjeGroupKey(utbetalesTilId = AktoerId(1), kid = null)
            segment.delperioder shouldHaveSize 1

            val content = segment.delperioder.first()
            content.periode shouldBe DatoIntervall("2026-02-01", "2026-03-15")
        }

        test("byggBeregningsmotorKontekst med månedlig satstype trekkperiode innenfor én måned splitter ikke") {
            val vedtak =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.LOPM,
                    type = TestObjects.TrekkType.YTELSESPERIODE_SKATTEREDUSERENDE,
                    perioder = listOf(DatoIntervall("2026-02-05", "2026-02-20")),
                    sats = BigDecimal("1000"),
                )
            val periode = DatoIntervall("2026-02-05", "2026-02-20")
            val oppdrag = listOf(oppdragFraFil(defaultTestFile))
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
            segment.linjeGroupKey shouldBe LinjeGroupKey(utbetalesTilId = AktoerId(1), kid = null)
            segment.delperioder shouldHaveSize 1

            val content = segment.delperioder.first()
            content.periode shouldBe DatoIntervall("2026-02-05", "2026-02-20")
        }
    })
