package no.nav.sokos.dare.poc.beregning.fordeler

import java.time.LocalDate

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

import no.nav.sokos.dare.poc.TestUtil.oppdragFraFil
import no.nav.sokos.dare.poc.avvent.Avvent
import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.beregning.EkstraInfo
import no.nav.sokos.dare.poc.beregning.TekstEkstraInfo
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Beregningsmotor
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Skattemotor
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Trekkmotor
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Ytelsesmotor
import no.nav.sokos.dare.poc.beregning.dataklasser.BeregningsStatus
import no.nav.sokos.dare.poc.beregning.dataklasser.BeregningsStatusflagg
import no.nav.sokos.dare.poc.oppdrag.OppdragId
import no.nav.sokos.dare.poc.testFordelingInput

class BeregningsFordelerAvventTest :
    FunSpec({

        val fordeler = BeregningsFordeler(Beregningsmotor(Ytelsesmotor(), Trekkmotor(), Skattemotor()))

        test("should split period at avvent boundary and set AVVENT status") {
            val periode = DatoIntervall("2026-02-01", "2026-02-28")
            val avventPerioder =
                listOf(
                    Avvent(
                        oppdragId = OppdragId(0),
                        periode = DatoIntervall("2026-02-10", "2026-02-20"),
                        kodeArsak = "AVAV",
                        datoOverfores = null,
                        bruker = "test",
                    ),
                )
            val oppdrag = listOf(oppdragFraFil("testdata/BeregningsFordelerBasicTest/enkel-ytelse-feb-til-mid-mar-2026.xml").copy(avventPerioder = avventPerioder))
            val input = testFordelingInput()

            val kontekst =
                fordeler.fordel(
                    input = input,
                    periode = periode,
                    oppdrag = oppdrag,
                    trekk = emptyList(),
                    skatteKontekst = SkatteKontekst(skattekort = null, allokeringer = emptyMap()),
                    relaterteBeregninger = emptyList(),
                )

            val beregninger = kontekst.beregninger()
            beregninger shouldHaveSize 3

            beregninger[0].status shouldBe BeregningsStatus.BEREGNET
            beregninger[0].statusflagg shouldBe listOf(BeregningsStatusflagg.MANGLER_SKATTEKORT)

            beregninger[1].status shouldBe BeregningsStatus.AVVENT
            beregninger[1].statusflagg shouldBe listOf(BeregningsStatusflagg.MANUELL, BeregningsStatusflagg.MANGLER_SKATTEKORT)
            beregninger[1].overfoeres shouldBe null

            beregninger[2].status shouldBe BeregningsStatus.BEREGNET
        }

        test("should set DATOSTYRT statusflagg and datoOverfores for AVRK") {
            val periode = DatoIntervall("2026-03-01", "2026-03-31")
            val avventPerioder =
                listOf(
                    Avvent(
                        oppdragId = OppdragId(0),
                        periode = DatoIntervall("2026-03-01", "2026-03-31"),
                        kodeArsak = "AVRK",
                        datoOverfores = LocalDate.of(2026, 4, 15),
                        bruker = "test",
                    ),
                )
            val oppdrag = listOf(oppdragFraFil("aapApiTest/simple.xml").copy(avventPerioder = avventPerioder))
            val input = testFordelingInput()

            val kontekst =
                fordeler.fordel(
                    input = input,
                    periode = periode,
                    oppdrag = oppdrag,
                    trekk = emptyList(),
                    skatteKontekst = SkatteKontekst(skattekort = null, allokeringer = emptyMap()),
                    relaterteBeregninger = emptyList(),
                )

            val beregninger = kontekst.beregninger()
            beregninger shouldHaveSize 1

            beregninger[0].status shouldBe BeregningsStatus.AVVENT
            beregninger[0].statusflagg shouldBe listOf(BeregningsStatusflagg.DATOSTYRT, BeregningsStatusflagg.MANGLER_SKATTEKORT)
            beregninger[0].overfoeres shouldBe LocalDate.of(2026, 4, 15)
        }

        test("should add AVVENT ekstraInfo to affected segments") {
            val periode = DatoIntervall("2026-03-01", "2026-03-31")
            val avventPerioder =
                listOf(
                    Avvent(
                        oppdragId = OppdragId(0),
                        periode = DatoIntervall("2026-03-01", "2026-03-31"),
                        kodeArsak = "AVAV",
                        datoOverfores = null,
                        bruker = "test",
                    ),
                )
            val oppdrag = listOf(oppdragFraFil("aapApiTest/simple.xml").copy(avventPerioder = avventPerioder))
            val input = testFordelingInput()

            val kontekst =
                fordeler.fordel(
                    input = input,
                    periode = periode,
                    oppdrag = oppdrag,
                    trekk = emptyList(),
                    skatteKontekst = SkatteKontekst(skattekort = null, allokeringer = emptyMap()),
                    relaterteBeregninger = emptyList(),
                )

            val segment = kontekst.segmenter.first()
            segment.ekstraInfo.any { it is TekstEkstraInfo && it.grunn == EkstraInfo.Reasons.AVVENT } shouldBe true
        }
    })
