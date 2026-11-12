package no.nav.utbetaling.beregningsflyt

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

import no.nav.utbetaling.beregning.oppdrag.Avventingsårsak
import no.nav.utbetaling.beregning.oppdrag.Kidnummer
import no.nav.utbetaling.beregning.oppdrag.Oppdrag
import no.nav.utbetaling.beregning.oppdrag.Personidentifikator
import no.nav.utbetaling.beregning.tid.april
import no.nav.utbetaling.beregning.tid.juni
import no.nav.utbetaling.beregning.tid.mars
import no.nav.utbetaling.beregning.tid.til

class SegmenteringTest :
    FunSpec({

        test("Bygger segmenter fra flere oppdrag med flere linjer") {
            val oppdragJson = object {}.javaClass.getResource("/segmentering/oppdrag.json")?.readText() ?: error("Kunne ikke lese oppdrag.json")
            val oppdragstidslinje = (Oppdrag.fraJson(oppdragJson) as? Oppdrag.Utbetaling)?.oppdragstidslinje ?: error("Kunne ikke lage oppdragstidslinje fra oppdrag.json")

            // Det finnes linjer fra 1.mars(2025) til 30.juni(2025) - innskrenker beregningsperioden en dag i hver ende
            val segmentbyggere = Segmentering.segmentbyggere(oppdragstidslinje, 2.mars(2025) til 29.juni(2025))
            segmentbyggere.size shouldBe 4

            with(segmentbyggere[0]) {
                utbetaltTil shouldBe Personidentifikator("23519035766")
                kidnummer shouldBe Kidnummer("123")
                avventingsårsak shouldBe null
                tidslinje.associate { it.first to it.second.size } shouldBe mapOf(
                    5.april(2025) til 9.april(2025) to 1,
                )
            }

            with(segmentbyggere[1]) {
                utbetaltTil shouldBe Personidentifikator("23519035766")
                kidnummer shouldBe null
                avventingsårsak shouldBe null
                tidslinje.associate { it.first to it.second.size } shouldBe mapOf(
                    31.mars(2025) til 31.mars(2025) to 1,
                    1.april(2025) til 10.april(2025) to 2,
                    11.april(2025) til 11.april(2025) to 1,
                )
            }

            with(segmentbyggere[2]) {
                utbetaltTil shouldBe Personidentifikator("11111111111")
                kidnummer shouldBe null
                avventingsårsak shouldBe null
                tidslinje.associate { it.first to it.second.size } shouldBe mapOf(
                    2.mars(2025) til 30.april(2025) to 1,
                )
            }

            with(segmentbyggere[3]) {
                utbetaltTil shouldBe Personidentifikator("11111111111")
                kidnummer shouldBe null
                avventingsårsak shouldBe Avventingsårsak.Refusjonskrav(30.juni(2025))
                tidslinje.associate { it.first to it.second.size } shouldBe mapOf(
                    1.juni(2025) til 29.juni(2025) to 1,
                )
            }
        }
    })
