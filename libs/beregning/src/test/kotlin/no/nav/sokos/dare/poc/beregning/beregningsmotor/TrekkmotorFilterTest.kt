package no.nav.sokos.dare.poc.beregning.beregningsmotor

import java.math.BigDecimal
import java.time.LocalDateTime

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

import no.nav.sokos.dare.poc.TestObjects
import no.nav.sokos.dare.poc.TestUtil.oppdragFraFil
import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.beregning.DelberegningType
import no.nav.sokos.dare.poc.beregning.fordeler.BeregningsFordeler
import no.nav.sokos.dare.poc.beregning.fordeler.SkatteKontekst
import no.nav.sokos.dare.poc.oppdrag.OppdragId
import no.nav.sokos.dare.poc.testFordelingInput
import no.nav.sokos.dare.poc.trekk.TrekkAlternativ
import no.nav.sokos.dare.poc.trekk.Trekkvedtak

class TrekkmotorFilterTest :
    FunSpec({

        val fordeler = BeregningsFordeler(Beregningsmotor(Ytelsesmotor(), Trekkmotor(), Skattemotor()))
        val ytelsesmotor = Ytelsesmotor()
        val trekkmotor = Trekkmotor()

        fun oppdragMedId(id: Long = 1) =
            oppdragFraFil("aapApiTest/simple.xml").let {
                it.copy(id = OppdragId(id))
            }

        test("beregnSkattereduserendeTrekk plukker kun opp skattereduserende trekk") {
            val skattereduserendeTrekk =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.LOPD,
                    type = TestObjects.TrekkType.YTELSESPERIODE_SKATTEREDUSERENDE,
                    perioder = listOf(DatoIntervall("2026-03-02", "2026-03-06")),
                    sats = BigDecimal("100"),
                )

            val ikkeSkattereduserendeTrekk =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.LOPD,
                    type = TestObjects.TrekkType.YTELSESPERIODE_IKKE_SKATTEREDUSERENDE,
                    perioder = listOf(DatoIntervall("2026-03-02", "2026-03-06")),
                    sats = BigDecimal("50"),
                )

            val beregningsPeriode = DatoIntervall("2026-03-02", "2026-03-06")
            val beregningsOppdrag = listOf(oppdragMedId())
            val beregningsTrekk = listOf(skattereduserendeTrekk, ikkeSkattereduserendeTrekk)
            val input = testFordelingInput()

            val kontekst =
                fordeler.byggBeregningsmotorKontekst(
                    periode = beregningsPeriode,
                    oppdrag = beregningsOppdrag,
                    trekk = beregningsTrekk,
                    input = input,
                    skatteKontekst = SkatteKontekst(skattekort = null, allokeringer = emptyMap()),
                    relaterteBeregninger = emptyList(),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )
            val medYtelse = ytelsesmotor.beregnYtelse(kontekst)
            val result = trekkmotor.calculateSkattereduserendeTrekk(medYtelse)

            val periode = DatoIntervall("2026-03-02", "2026-03-06")
            val scratchpad = result.segmenter.first().scratchpad[OppdragId(1)]!!
            val trekkLinjer =
                scratchpad.beregninger[periode]!!
                    .filter { it.type == DelberegningType.TREKK }

            // Only the skattereduserende trekk should be picked up
            trekkLinjer shouldHaveSize 1
            trekkLinjer.first().sats shouldBe BigDecimal("100")
        }

        test("beregnSkattereduserendeTrekk plukker ikke opp ikke-skattereduserende trekk") {
            val ikkeSkattereduserendeTrekk =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.LOPD,
                    type = TestObjects.TrekkType.YTELSESPERIODE_IKKE_SKATTEREDUSERENDE,
                    perioder = listOf(DatoIntervall("2026-03-02", "2026-03-06")),
                    sats = BigDecimal("50"),
                )

            val beregningsPeriode = DatoIntervall("2026-03-02", "2026-03-06")
            val beregningsOppdrag = listOf(oppdragMedId())
            val beregningsTrekk = listOf(ikkeSkattereduserendeTrekk)
            val input = testFordelingInput()

            val kontekst =
                fordeler.byggBeregningsmotorKontekst(
                    periode = beregningsPeriode,
                    oppdrag = beregningsOppdrag,
                    trekk = beregningsTrekk,
                    input = input,
                    skatteKontekst = SkatteKontekst(skattekort = null, allokeringer = emptyMap()),
                    relaterteBeregninger = emptyList(),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )
            val medYtelse = ytelsesmotor.beregnYtelse(kontekst)
            val result = trekkmotor.calculateSkattereduserendeTrekk(medYtelse)

            val periode = DatoIntervall("2026-03-02", "2026-03-06")
            val scratchpad = result.segmenter.first().scratchpad[OppdragId(1)]!!
            val trekkLinjer =
                scratchpad.beregninger[periode]!!
                    .filter { it.type == DelberegningType.TREKK }

            trekkLinjer shouldHaveSize 0
        }

        test("beregnSkattereduserendeTrekk uten trekk legger ikke til trekk beregninger") {
            val beregningsPeriode = DatoIntervall("2026-03-02", "2026-03-06")
            val beregningsOppdrag = listOf(oppdragMedId())
            val beregningsTrekk = emptyList<Trekkvedtak>()
            val input = testFordelingInput()

            val kontekst =
                fordeler.byggBeregningsmotorKontekst(
                    periode = beregningsPeriode,
                    oppdrag = beregningsOppdrag,
                    trekk = beregningsTrekk,
                    input = input,
                    skatteKontekst = SkatteKontekst(skattekort = null, allokeringer = emptyMap()),
                    relaterteBeregninger = emptyList(),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )
            val medYtelse = ytelsesmotor.beregnYtelse(kontekst)
            val result = trekkmotor.calculateSkattereduserendeTrekk(medYtelse)

            val periode = DatoIntervall("2026-03-02", "2026-03-06")
            val scratchpad = result.segmenter.first().scratchpad[OppdragId(1)]!!
            scratchpad.beregninger[periode]!!.all { it.type == DelberegningType.YTELSE } shouldBe true
        }

        test("beregnSkattereduserendeTrekk plukker opp flere skattereduserende trekk") {
            val trekk1 =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.LOPD,
                    type = TestObjects.TrekkType.YTELSESPERIODE_SKATTEREDUSERENDE,
                    perioder = listOf(DatoIntervall("2026-03-02", "2026-03-06")),
                    sats = BigDecimal("100"),
                )

            val trekk2 =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.LOPD,
                    type = TestObjects.TrekkType.YTELSESPERIODE_SKATTEREDUSERENDE,
                    perioder = listOf(DatoIntervall("2026-03-02", "2026-03-06")),
                    sats = BigDecimal("200"),
                )

            val beregningsPeriode = DatoIntervall("2026-03-02", "2026-03-06")
            val beregningsOppdrag = listOf(oppdragMedId())
            val beregningsTrekk = listOf(trekk1, trekk2)
            val input = testFordelingInput()

            val kontekst =
                fordeler.byggBeregningsmotorKontekst(
                    periode = beregningsPeriode,
                    oppdrag = beregningsOppdrag,
                    trekk = beregningsTrekk,
                    input = input,
                    skatteKontekst = SkatteKontekst(skattekort = null, allokeringer = emptyMap()),
                    relaterteBeregninger = emptyList(),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )
            val medYtelse = ytelsesmotor.beregnYtelse(kontekst)
            val result = trekkmotor.calculateSkattereduserendeTrekk(medYtelse)

            val periode = DatoIntervall("2026-03-02", "2026-03-06")
            val scratchpad = result.segmenter.first().scratchpad[OppdragId(1)]!!
            val trekkLinjer =
                scratchpad.beregninger[periode]!!
                    .filter { it.type == DelberegningType.TREKK }

            trekkLinjer shouldHaveSize 2
        }
    })
