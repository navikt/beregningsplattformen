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

class BeregningsFordelerDailyTest :
    FunSpec({

        test("byggBeregningsmotorKontekst med daglig satstype oppretter én delperiode med dag-boetter") {
            val vedtak =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.LOPD,
                    type = TestObjects.TrekkType.YTELSESPERIODE_SKATTEREDUSERENDE,
                    perioder = listOf(DatoIntervall("2026-03-02", "2026-03-05")),
                    sats = BigDecimal("200"),
                )
            val periode = DatoIntervall("2026-03-02", "2026-03-05")
            val oppdrag = listOf(oppdragFraFil("aapApiTest/simple.xml"))
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
            content.periode shouldBe DatoIntervall("2026-03-02", "2026-03-05")
            content.trekk shouldHaveSize 1
            content.trekk
                .first()
                .trekkperiode.boetter shouldBe
                listOf(
                    "2026-03-02",
                    "2026-03-03",
                    "2026-03-04",
                    "2026-03-05",
                )

            val trekkperiodeId = vedtak.trekkperioder.first().id!!
            result.satsBoetter.restSats(SatsBoetteId(trekkperiodeId, "2026-03-02")) shouldBe BigDecimal("200")
            result.satsBoetter.restSats(SatsBoetteId(trekkperiodeId, "2026-03-03")) shouldBe BigDecimal("200")
            result.satsBoetter.restSats(SatsBoetteId(trekkperiodeId, "2026-03-04")) shouldBe BigDecimal("200")
            result.satsBoetter.restSats(SatsBoetteId(trekkperiodeId, "2026-03-05")) shouldBe BigDecimal("200")
        }

        test("byggBeregningsmotorKontekst med daglig satstype som spenner over flere måneder oppretter én delperiode med dag-boetter") {
            val vedtak =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.LOPD,
                    type = TestObjects.TrekkType.YTELSESPERIODE_SKATTEREDUSERENDE,
                    perioder = listOf(DatoIntervall("2026-02-27", "2026-03-02")),
                    sats = BigDecimal("150"),
                )
            val periode = DatoIntervall("2026-02-27", "2026-03-02")
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
            segment.delperioder shouldHaveSize 1

            val content = segment.delperioder.first()
            content.periode shouldBe DatoIntervall("2026-02-27", "2026-03-02")
            content.trekk shouldHaveSize 1
            content.trekk
                .first()
                .trekkperiode.boetter shouldBe
                listOf(
                    "2026-02-27",
                    "2026-03-02",
                )
        }
    })
