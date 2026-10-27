package no.nav.sokos.dare.poc.beregning.fordeler

import java.math.BigDecimal
import java.time.LocalDate
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
import no.nav.sokos.dare.poc.skattekort.domain.Forskuddstrekk
import no.nav.sokos.dare.poc.skattekort.domain.ForskuddstrekkKode
import no.nav.sokos.dare.poc.skattekort.domain.ForskuddstrekkType
import no.nav.sokos.dare.poc.skattekort.domain.Skattekort
import no.nav.sokos.dare.poc.testFordelingInput
import no.nav.sokos.dare.poc.trekk.TrekkAlternativ

class BeregningsFordelerBoundaryTest :
    FunSpec({

        test("byggBeregningsmotorKontekst med to trekkvedtak med delvis overlappende trekkperioder oppretter én segment med fem delperioder") {
            val vedtak1 =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.LOPP,
                    type = TestObjects.TrekkType.YTELSESPERIODE_SKATTEREDUSERENDE,
                    perioder = listOf(DatoIntervall("2026-02-03", "2026-03-08")),
                    sats = BigDecimal("10"),
                )
            val vedtak2 =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.LOPP,
                    type = TestObjects.TrekkType.YTELSESPERIODE_SKATTEREDUSERENDE,
                    perioder = listOf(DatoIntervall("2026-02-15", "2026-03-13")),
                    sats = BigDecimal("20"),
                )
            val periode = DatoIntervall("2026-02-01", "2026-03-15")
            val oppdrag = listOf(oppdragFraFil("testdata/BeregningsFordelerBoundaryTest/enkel-ytelse-feb-til-mid-mar.xml"))
            val trekk = listOf(vedtak1, vedtak2)
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
            segment.delperioder shouldHaveSize 5

            // Before both trekkperioder
            segment.delperioder[0].periode shouldBe DatoIntervall("2026-02-01", "2026-02-02")
            segment.delperioder[0].trekk shouldHaveSize 0

            // Only vedtak1 active
            segment.delperioder[1].periode shouldBe DatoIntervall("2026-02-03", "2026-02-14")
            segment.delperioder[1].trekk shouldHaveSize 1
            segment.delperioder[1]
                .trekk
                .first()
                .id shouldBe vedtak1.id!!

            // Both active (overlap)
            segment.delperioder[2].periode shouldBe DatoIntervall("2026-02-15", "2026-03-08")
            segment.delperioder[2].trekk shouldHaveSize 2

            // Only vedtak2 active
            segment.delperioder[3].periode shouldBe DatoIntervall("2026-03-09", "2026-03-13")
            segment.delperioder[3].trekk shouldHaveSize 1
            segment.delperioder[3]
                .trekk
                .first()
                .id shouldBe vedtak2.id!!

            // After both trekkperioder
            segment.delperioder[4].periode shouldBe DatoIntervall("2026-03-14", "2026-03-15")
            segment.delperioder[4].trekk shouldHaveSize 0
        }

        test("byggBeregningsmotorKontekst fjerner trekkvedtak fra delperioder der deres trekkperiode ikke overlapper") {
            val vedtakEarly =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.LOPP,
                    type = TestObjects.TrekkType.YTELSESPERIODE_SKATTEREDUSERENDE,
                    perioder = listOf(DatoIntervall("2026-02-01", "2026-02-15")),
                    sats = BigDecimal("100"),
                )
            val vedtakLate =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.LOPP,
                    type = TestObjects.TrekkType.YTELSESPERIODE_SKATTEREDUSERENDE,
                    perioder = listOf(DatoIntervall("2026-03-01", "2026-03-15")),
                    sats = BigDecimal("200"),
                )
            val periode = DatoIntervall("2026-02-01", "2026-03-15")
            val oppdrag = listOf(oppdragFraFil("testdata/BeregningsFordelerBasicTest/enkel-ytelse-feb-til-mid-mar-2026.xml"))
            val trekk = listOf(vedtakEarly, vedtakLate)
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

            // First segment: only vedtakEarly
            segment.delperioder[0].periode shouldBe DatoIntervall("2026-02-01", "2026-02-15")
            segment.delperioder[0].trekk shouldHaveSize 1
            segment.delperioder[0]
                .trekk
                .first()
                .id shouldBe vedtakEarly.id!!

            // Middle segment: no trekk
            segment.delperioder[1].periode shouldBe DatoIntervall("2026-02-16", "2026-02-28")
            segment.delperioder[1].trekk shouldHaveSize 0

            // Last segment: only vedtakLate
            segment.delperioder[2].periode shouldBe DatoIntervall("2026-03-01", "2026-03-15")
            segment.delperioder[2].trekk shouldHaveSize 1
            segment.delperioder[2]
                .trekk
                .first()
                .id shouldBe vedtakLate.id!!
        }

        test("periode som krysser årsskifte skal splittes ved nyttår") {
            val periode = DatoIntervall("2025-12-22", "2026-01-09")
            val oppdrag = listOf(oppdragFraFil("aapApiTest/oppdrag_med_ytelse_fra_2025_til_2026.xml"))
            val input = testFordelingInput()

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

            val allePerioder = result.segmenter.flatMap { it.delperioder }.map { it.periode }
            allePerioder.forEach { periode ->
                periode.fom.year shouldBe periode.tom!!.year
            }
            allePerioder.map { it.fom.year }.toSet() shouldBe setOf(2025, 2026)
        }

        test("periode som krysser januar/februar splittes når fjoraars skattekort brukes") {
            val periode = DatoIntervall("2026-01-15", "2026-02-15")
            val oppdrag = listOf(oppdragFraFil("aapApiTest/oppdrag_med_ytelse_fra_2025_til_2026.xml"))
            val input = testFordelingInput()

            val fjoraarsSkattekort =
                Skattekort(
                    aktoerId = AktoerId(1),
                    inntektsaar = 2025,
                    kilde = "TEST",
                    identifikator = 1,
                    utstedtDato = LocalDate.of(2025, 1, 1),
                    forskuddsTrekk =
                        listOf(
                            Forskuddstrekk(
                                typeForskuddsTrekk = ForskuddstrekkType.PROSENTKORT,
                                trekkKode = ForskuddstrekkKode.LOENN_FRA_NAV,
                                prosentsats = BigDecimal(30),
                            ),
                        ),
                )

            val skatteKontekst =
                SkatteKontekst(
                    skattekort = fjoraarsSkattekort,
                    allokeringer = emptyMap(),
                )

            val result =
                BeregningsFordeler(
                    Beregningsmotor(Ytelsesmotor(), Trekkmotor(), Skattemotor()),
                ).byggBeregningsmotorKontekst(
                    periode = periode,
                    oppdrag = oppdrag,
                    trekk = emptyList(),
                    input = input,
                    skatteKontekst = skatteKontekst,
                    relaterteBeregninger = emptyList(),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )

            val allePerioder = result.segmenter.flatMap { it.delperioder }.map { it.periode }
            allePerioder.forEach { periode ->
                periode.fom.monthValue shouldBe periode.tom!!.monthValue
            }
            allePerioder.map { it.fom.monthValue }.toSet() shouldBe setOf(1, 2)
        }
    })
