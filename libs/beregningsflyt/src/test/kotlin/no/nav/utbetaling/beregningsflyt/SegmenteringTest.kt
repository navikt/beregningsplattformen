package no.nav.utbetaling.beregningsflyt

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe

import no.nav.utbetaling.beregning.oppdrag.Avventingsårsak
import no.nav.utbetaling.beregning.oppdrag.Kidnummer
import no.nav.utbetaling.beregning.oppdrag.Personidentifikator
import no.nav.utbetaling.beregning.tid.april
import no.nav.utbetaling.beregning.tid.februar
import no.nav.utbetaling.beregning.tid.januar
import no.nav.utbetaling.beregning.tid.juni
import no.nav.utbetaling.beregning.tid.mars
import no.nav.utbetaling.beregning.tid.til
import no.nav.utbetaling.beregningsflyt.testhjelpere.oppdragstidslinje
import no.nav.utbetaling.beregningsflyt.testhjelpere.opplysninger

class SegmenteringTest :
    FunSpec({
        val avventerRefusjonskrav = Avventingsårsak.Refusjonskrav(30.juni(2025))

        test("Avventing midt i en periode gir eget segment, og segmentene før og etter er adskilt") {
            val segmentbyggere =
                Segmentering.grunnlag(
                    oppdragstidslinje =
                        oppdragstidslinje(
                            1.januar(2025) til 31.januar(2025) to opplysninger(),
                            1.februar(2025) til 28.februar(2025) to opplysninger(avventingsårsak = avventerRefusjonskrav),
                            1.mars(2025) til 31.mars(2025) to opplysninger(),
                        ),
                )

            segmentbyggere.map { it.avventingsårsak to it.delperiodegrunnlag.map { (periode) -> periode } } shouldBe
                listOf(
                    null to listOf(1.januar(2025) til 31.januar(2025)),
                    avventerRefusjonskrav to listOf(1.februar(2025) til 28.februar(2025)),
                    null to listOf(1.mars(2025) til 31.mars(2025)),
                )
        }

        test("Hull uten linjer deler ikke segmentet") {
            val segmentbyggere =
                Segmentering.grunnlag(
                    oppdragstidslinje =
                        oppdragstidslinje(
                            1.januar(2025) til 31.januar(2025) to opplysninger(avventingsårsak = avventerRefusjonskrav),
                            1.mars(2025) til 31.mars(2025) to opplysninger(avventingsårsak = avventerRefusjonskrav),
                        ),
                )

            segmentbyggere.single().delperiodegrunnlag.map { (periode) -> periode } shouldBe
                listOf(1.januar(2025) til 31.januar(2025), 1.mars(2025) til 31.mars(2025))
        }

        test("Oppdrag i samme segment deler delperiodegrenser") {
            val segmentbyggere =
                Segmentering.grunnlag(
                    oppdragstidslinje =
                        oppdragstidslinje(
                            1.januar(2025) til 31.mars(2025) to opplysninger(fagsystemId = "A"),
                            1.februar(2025) til 28.februar(2025) to opplysninger(fagsystemId = "B"),
                        ),
                )

            segmentbyggere.single().delperiodegrunnlag.map { (periode, perOppdrag) -> periode to perOppdrag.keys.map { it.fagsystemId.id } } shouldBe
                listOf(
                    (1.januar(2025) til 31.januar(2025)) to listOf("A"),
                    (1.februar(2025) til 28.februar(2025)) to listOf("A", "B"),
                    (1.mars(2025) til 31.mars(2025)) to listOf("A"),
                )
        }

        test("Bygger segmenter fra flere oppdrag med flere linjer") {
            val mottaker1 = Personidentifikator("22222222222")
            val mottaker2 = Personidentifikator("11111111111")

            val segmentbyggere =
                Segmentering.grunnlag(
                    oppdragstidslinje =
                        oppdragstidslinje(
                            31.mars(2025) til 11.april(2025) to opplysninger(fagsystemId = "4Mi993K", delytelseId = "1", utbetalesTil = mottaker1),
                            1.april(2025) til 10.april(2025) to opplysninger(fagsystemId = "4Mi993K", delytelseId = "2", utbetalesTil = mottaker1),
                            5.april(2025) til 9.april(2025) to opplysninger(fagsystemId = "4Mi993K_2", delytelseId = "3", utbetalesTil = mottaker1, kidnummer = "123"),
                            1.mars(2025) til 30.april(2025) to opplysninger(fagsystemId = "4Mi993K_2", delytelseId = "4", utbetalesTil = mottaker2),
                            1.juni(2025) til 30.juni(2025) to opplysninger(fagsystemId = "4Mi993K_2", delytelseId = "5", utbetalesTil = mottaker2, avventingsårsak = avventerRefusjonskrav),
                        ),
                )
            segmentbyggere.size shouldBe 4

            with(segmentbyggere[2]) {
                utbetaltTil shouldBe mottaker1
                kidnummer shouldBe Kidnummer("123")
                avventingsårsak shouldBe null
                delperiodegrunnlag.associate {
                    it.first to
                        it.second.values
                            .single()
                            .size
                } shouldBe
                    mapOf(
                        5.april(2025) til 9.april(2025) to 1,
                    )
            }

            with(segmentbyggere[1]) {
                utbetaltTil shouldBe mottaker1
                kidnummer shouldBe null
                avventingsårsak shouldBe null
                delperiodegrunnlag.associate {
                    it.first to
                        it.second.values
                            .single()
                            .size
                } shouldBe
                    mapOf(
                        31.mars(2025) til 31.mars(2025) to 1,
                        1.april(2025) til 10.april(2025) to 2,
                        11.april(2025) til 11.april(2025) to 1,
                    )
            }

            with(segmentbyggere[0]) {
                utbetaltTil shouldBe mottaker2
                kidnummer shouldBe null
                avventingsårsak shouldBe null
                delperiodegrunnlag.associate {
                    it.first to
                        it.second.values
                            .single()
                            .size
                } shouldBe
                    mapOf(
                        1.mars(2025) til 30.april(2025) to 1,
                    )
            }

            with(segmentbyggere[3]) {
                utbetaltTil shouldBe mottaker2
                kidnummer shouldBe null
                avventingsårsak shouldBe avventerRefusjonskrav
                delperiodegrunnlag.associate {
                    it.first to
                        it.second.values
                            .single()
                            .size
                } shouldBe
                    mapOf(
                        1.juni(2025) til 30.juni(2025) to 1,
                    )
            }
        }

        withData(nameFn = { "Håndterer 2 overlappende linjer i $it oppdrag" }, listOf(1, 2)) { antallOppdrag ->
            val fagsystemId1 = "A"
            val fagsystemId2 = if (antallOppdrag == 1) fagsystemId1 else "B"

            val segmentbyggere =
                Segmentering.grunnlag(
                    oppdragstidslinje =
                        oppdragstidslinje(
                            1.januar til 31.januar to opplysninger(fagsystemId = fagsystemId1),
                            1.januar til 31.januar to opplysninger(fagsystemId = fagsystemId2),
                        ),
                )
            segmentbyggere.size shouldBe 1
            segmentbyggere.single().delperiodegrunnlag.map { it.first } shouldBe listOf(1.januar til 31.januar)
            val delperioder =
                segmentbyggere
                    .single()
                    .delperiodegrunnlag
                    .single()
                    .second
            delperioder.size shouldBe antallOppdrag

            if (antallOppdrag == 1) {
                delperioder.entries
                    .first()
                    .value.size shouldBe 2 // Antall ytelselinjer
            } else {
                delperioder
                    .toList()
                    .first()
                    .second.size shouldBe 1 // Antall ytelselinjer
                delperioder
                    .toList()
                    .last()
                    .second.size shouldBe 1 // Antall ytelselinjer
            }
        }
    })
