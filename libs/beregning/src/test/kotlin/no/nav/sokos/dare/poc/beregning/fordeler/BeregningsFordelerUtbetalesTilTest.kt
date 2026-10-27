package no.nav.sokos.dare.poc.beregning.fordeler

import java.time.LocalDateTime

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

import no.nav.sokos.dare.poc.TestUtil.oppdragFraFil
import no.nav.sokos.dare.poc.aktoer.AktoerId
import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.beregning.EkstraInfo
import no.nav.sokos.dare.poc.beregning.EkstraInfoDsl.verdi
import no.nav.sokos.dare.poc.beregning.TekstEkstraInfo
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Beregningsmotor
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Skattemotor
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Trekkmotor
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Ytelsesmotor
import no.nav.sokos.dare.poc.oppdrag.OppdragslinjekjedeId
import no.nav.sokos.dare.poc.testFordelingInput
import no.nav.sokos.dare.poc.trekk.Trekkvedtak

class BeregningsFordelerUtbetalesTilTest :
    FunSpec({
        val defaultTestFile = "testdata/BeregningsFordelerBasicTest/enkel-ytelse-feb-til-mid-mar-2026.xml"

        test("byggBeregningsmotorKontekst med oppdrag-linjer med forskjellig utbetalesTilId oppretter to segmenter") {
            val baseOppdrag = oppdragFraFil(defaultTestFile)
            val linje1 = baseOppdrag.alleOppdragslinjer.first().copy(utbetalesTilId = AktoerId(100))
            val linje2 =
                linje1.copy(
                    oppdragslinjekjedeId = OppdragslinjekjedeId(-2),
                    utbetalesTilId = AktoerId(999),
                    delytelseId = "other-delytelse",
                )

            val oppdragWithTwoIds = baseOppdrag.medOppdragslinjer(listOf(linje1, linje2))

            val periode = DatoIntervall("2026-02-01", "2026-03-15")
            val oppdrag = listOf(oppdragWithTwoIds)
            val trekk = emptyList<Trekkvedtak>()
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

            result.segmenter shouldHaveSize 2

            val segment1 = result.segmenter[0]
            val segment2 = result.segmenter[1]

            val keys = result.segmenter.map { it.linjeGroupKey }.toSet()
            keys shouldBe
                setOf(
                    LinjeGroupKey(utbetalesTilId = AktoerId(100), kid = null),
                    LinjeGroupKey(utbetalesTilId = AktoerId(999), kid = null),
                )

            segment1.delperioder shouldHaveSize 1
            segment2.delperioder shouldHaveSize 1

            val linjer1 =
                segment1.delperioder
                    .first()
                    .oppdrag.alleOppdragslinjer
            val linjer2 =
                segment2.delperioder
                    .first()
                    .oppdrag.alleOppdragslinjer

            linjer1 shouldHaveSize 1
            linjer2 shouldHaveSize 1

            val ids =
                setOf(
                    linjer1.first().utbetalesTilId,
                    linjer2.first().utbetalesTilId,
                )
            ids shouldBe setOf(AktoerId(100), AktoerId(999))

            // Both segmenter have overstyrtMottaker since utbetalesTilId != gjelderId (AktoerId(1))
            result.segmenter.forEach { segment ->
                segment.ekstraInfo shouldHaveSize 1
                segment.ekstraInfo.first() shouldBe
                    TekstEkstraInfo(
                        grunn = EkstraInfo.Reasons.OVERSTYRT_MOTTAKER,
                        verdier = mapOf("utbetalesTilId" verdi segment.linjeGroupKey!!.utbetalesTilId),
                    )
            }
        }

        test("byggBeregningsmotorKontekst med oppdrag-linjer med samme utbetalesTilId oppretter én segment") {
            val periode = DatoIntervall("2026-02-01", "2026-03-15")
            val oppdrag = listOf(oppdragFraFil(defaultTestFile))
            val trekk = emptyList<Trekkvedtak>()
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
            result.segmenter.first().linjeGroupKey shouldBe LinjeGroupKey(utbetalesTilId = AktoerId(1), kid = null)
            result.segmenter.first().ekstraInfo shouldHaveSize 0
        }

        test("byggBeregningsmotorKontekst med null utbetalesTilId faller tilbake til oppdrag gjelderId for splitting") {
            val baseOppdrag = oppdragFraFil(defaultTestFile)
            // linje1 has null utbetalesTilId, should fall back to oppdrag.gjelderId (AktoerId(1))
            val linje1 = baseOppdrag.alleOppdragslinjer.first().copy(utbetalesTilId = null)
            val linje2 =
                linje1.copy(
                    oppdragslinjekjedeId = OppdragslinjekjedeId(-2),
                    utbetalesTilId = AktoerId(999),
                    delytelseId = "other-delytelse",
                )

            val oppdragWithMixed = baseOppdrag.medOppdragslinjer(listOf(linje1, linje2))

            val periode = DatoIntervall("2026-02-01", "2026-03-15")
            val oppdrag = listOf(oppdragWithMixed)
            val trekk = emptyList<Trekkvedtak>()
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

            result.segmenter shouldHaveSize 2

            val nullFallbackKeys = result.segmenter.map { it.linjeGroupKey }.toSet()
            nullFallbackKeys shouldBe
                setOf(
                    LinjeGroupKey(utbetalesTilId = AktoerId(1), kid = null),
                    LinjeGroupKey(utbetalesTilId = AktoerId(999), kid = null),
                )

            val allLinjerPerSegment =
                result.segmenter.map { segment ->
                    segment.delperioder.map { it.oppdrag.alleOppdragslinjer }.flatten()
                }

            allLinjerPerSegment.forEach { it shouldHaveSize 1 }

            val resolvedIds =
                allLinjerPerSegment
                    .map { linjer ->
                        linjer.first().utbetalesTilId ?: baseOppdrag.gjelderId
                    }.toSet()
            resolvedIds shouldBe setOf(baseOppdrag.gjelderId, AktoerId(999))

            // Only the segment with AktoerId(999) should have overstyrtMottaker
            val overstyrtSegment = result.segmenter.first { it.linjeGroupKey?.utbetalesTilId == AktoerId(999) }
            overstyrtSegment.ekstraInfo shouldHaveSize 1
            overstyrtSegment.ekstraInfo.first() shouldBe
                TekstEkstraInfo(
                    grunn = EkstraInfo.Reasons.OVERSTYRT_MOTTAKER,
                    verdier = mapOf("utbetalesTilId" verdi AktoerId(999)),
                )

            // The segment with gjelderId should not have overstyrtMottaker
            val normalSegment = result.segmenter.first { it.linjeGroupKey?.utbetalesTilId == AktoerId(1) }
            normalSegment.ekstraInfo shouldHaveSize 0
        }

        test("byggBeregningsmotorKontekst med oppdrag-linjer med forskjellig kid oppretter to segmenter") {
            val baseOppdrag = oppdragFraFil(defaultTestFile)
            val linje1 = baseOppdrag.alleOppdragslinjer.first().copy(kid = "KID-A")
            val linje2 =
                linje1.copy(
                    oppdragslinjekjedeId = OppdragslinjekjedeId(-2),
                    kid = "KID-B",
                    delytelseId = "other-delytelse",
                )

            val oppdragWithTwoKids = baseOppdrag.medOppdragslinjer(listOf(linje1, linje2))

            val periode = DatoIntervall("2026-02-01", "2026-03-15")
            val oppdrag = listOf(oppdragWithTwoKids)
            val trekk = emptyList<Trekkvedtak>()
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

            result.segmenter shouldHaveSize 2

            val kidKeys = result.segmenter.map { it.linjeGroupKey }.toSet()
            kidKeys shouldBe
                setOf(
                    LinjeGroupKey(utbetalesTilId = AktoerId(1), kid = "KID-A"),
                    LinjeGroupKey(utbetalesTilId = AktoerId(1), kid = "KID-B"),
                )

            val allLinjerPerSegment =
                result.segmenter.map { segment ->
                    segment.delperioder.map { it.oppdrag.alleOppdragslinjer }.flatten()
                }

            allLinjerPerSegment.forEach { it shouldHaveSize 1 }

            val kids = allLinjerPerSegment.map { it.first().kid }.toSet()
            kids shouldBe setOf("KID-A", "KID-B")

            // utbetalesTilId == gjelderId for both, so no overstyrtMottaker
            // Kid changes between first and second segment, so second segment gets KID_ENDRET
            result.segmenter[0].ekstraInfo shouldHaveSize 0
            result.segmenter[1].ekstraInfo shouldHaveSize 1
            result.segmenter[1].ekstraInfo.first() shouldBe
                TekstEkstraInfo(
                    grunn = EkstraInfo.Reasons.KID_ENDRET,
                )
        }
    })
