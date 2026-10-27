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
import no.nav.sokos.dare.poc.beregning.Beregning
import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.beregning.Delberegning
import no.nav.sokos.dare.poc.beregning.DelberegningType
import no.nav.sokos.dare.poc.beregning.Delberegningslinje
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Beregningsmotor
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Skattemotor
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Trekkmotor
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Ytelsesmotor
import no.nav.sokos.dare.poc.oppdrag.Oppdrag
import no.nav.sokos.dare.poc.oppdrag.OppdragId
import no.nav.sokos.dare.poc.oppdrag.OppdragslinjekjedeId
import no.nav.sokos.dare.poc.testFordelingInput
import no.nav.sokos.dare.poc.trekk.TrekkAlternativ
import no.nav.sokos.dare.poc.trekk.Trekkvedtak
import no.nav.sokos.dare.poc.trekk.TrekkvedtakId

class BeregningsFordelerFoersteUtbetalingTest :
    FunSpec({
        val periode = DatoIntervall("2026-02-01", "2026-03-15")
        val oppdrag = oppdragFraFil("testdata/BeregningsFordelerBasicTest/enkel-ytelse-feb-til-mid-mar-2026.xml")
        val input =
            testFordelingInput(
                beregnet = LocalDateTime.parse("2026-09-30T12:00:00"),
                overfoeringsdato = LocalDate.parse("2026-10-19"),
            )
        val fordeler = BeregningsFordeler(Beregningsmotor(Ytelsesmotor(), Trekkmotor(), Skattemotor()))
        val trekkvedtakId = TrekkvedtakId(100)

        fun historiskBeregning(
            overfoeres: LocalDate? = LocalDate.parse("2026-10-01"),
            oppdragIder: List<OppdragId?> = listOf(oppdrag.id),
            ytelsesperiode: DatoIntervall = periode,
            historiskTrekkvedtakId: TrekkvedtakId? = trekkvedtakId,
            linjetype: DelberegningType = DelberegningType.TREKK,
        ) = Beregning(
            periode = ytelsesperiode,
            konto = "",
            overfoeres = overfoeres,
            fagomraade = oppdrag.fagomraade,
            gjelder = input.gjelder,
            beregnet = LocalDateTime.parse("2026-10-01T12:00:00"),
            delberegning =
                oppdragIder.map { oppdragId ->
                    Delberegning(
                        oppdragId = oppdragId,
                        datoIntervall = ytelsesperiode,
                        linjer =
                            listOf(
                                Delberegningslinje(type = DelberegningType.YTELSE, dagsbeloep = emptyList(), beloep = BigDecimal("1000")),
                                Delberegningslinje(
                                    type = linjetype,
                                    dagsbeloep = emptyList(),
                                    beloep = BigDecimal("-100"),
                                    trekkvedtakId = historiskTrekkvedtakId,
                                ),
                            ),
                    )
                },
        )

        fun byggKontekst(
            historikk: List<Beregning> = emptyList(),
            aktuelleOppdrag: List<Oppdrag> = listOf(oppdrag),
            aktueltInput: FordelingInput = input,
            trekk: List<Trekkvedtak> = emptyList(),
        ) = fordeler.byggBeregningsmotorKontekst(
            periode = periode,
            oppdrag = aktuelleOppdrag,
            trekk = trekk,
            input = aktueltInput,
            skatteKontekst = SkatteKontekst(skattekort = null, allokeringer = emptyMap()),
            relaterteBeregninger = historikk,
            overføringsdato = aktueltInput.overfoeringsdato,
            beregnet = aktueltInput.beregnet,
        )

        fun erFoersteTrekk(kontekst: BeregningsmotorKontekst): List<Boolean> =
            kontekst.segmenter.flatMap { segment ->
                segment.delperioder.map { trekkvedtakId !in it.tidligereTrukneTrekkvedtakIMaaneden }
            }

        test("skal være første utbetaling uten historikk selv om overføringsdato er sent i måneden") {
            erFoersteTrekk(byggKontekst()) shouldBe listOf(true)
        }

        test("skal ikke være første trekk når samme oppdrag har samme trekk i overføringsmåneden") {
            val historikk = listOf(historiskBeregning())
            val kontekst = byggKontekst(historikk)

            erFoersteTrekk(kontekst) shouldBe listOf(false)
            kontekst.segmenter
                .single()
                .delperioder
                .single()
                .relaterteBeregninger shouldBe historikk
        }

        test("skal bruke full historikk også når historisk ytelsesperiode ikke overlapper") {
            val kontekst = byggKontekst(listOf(historiskBeregning(ytelsesperiode = DatoIntervall("2025-12-01", "2025-12-31"))))

            erFoersteTrekk(kontekst) shouldBe listOf(false)
            kontekst.segmenter
                .single()
                .delperioder
                .single()
                .relaterteBeregninger shouldBe emptyList()
        }

        test("skal ignorere tidligere utbetaling uten det aktuelle trekket") {
            val trekk =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.LOPM,
                    type = TestObjects.TrekkType.UTBETALINGSDATO_IKKE_SKATTEREDUSERENDE,
                    perioder = listOf(DatoIntervall("2026-10-01", "2026-10-31")),
                    sats = BigDecimal("100"),
                )
            val kontekst = byggKontekst(listOf(historiskBeregning(historiskTrekkvedtakId = null)), trekk = listOf(trekk))

            erFoersteTrekk(kontekst) shouldBe listOf(true)
            kontekst.segmenter.flatMap { it.delperioder }.forEach { it.trekk shouldHaveSize 1 }
        }

        test("skal ignorere andre trekkvedtak og linjer som ikke er trekk") {
            erFoersteTrekk(byggKontekst(listOf(historiskBeregning(historiskTrekkvedtakId = TrekkvedtakId(999))))) shouldBe listOf(true)
            erFoersteTrekk(byggKontekst(listOf(historiskBeregning(linjetype = DelberegningType.YTELSE)))) shouldBe listOf(true)
        }

        test("skal finne oppdraget blant flere delberegninger") {
            erFoersteTrekk(byggKontekst(listOf(historiskBeregning(oppdragIder = listOf(OppdragId(999), oppdrag.id))))) shouldBe listOf(false)
        }

        test("skal ignorere utbetaling for et annet oppdrag i samme måned") {
            erFoersteTrekk(byggKontekst(listOf(historiskBeregning(oppdragIder = listOf(OppdragId(999)))))) shouldBe listOf(true)
        }

        test("skal ignorere utbetaling i en annen måned") {
            erFoersteTrekk(byggKontekst(listOf(historiskBeregning(overfoeres = LocalDate.parse("2026-09-30"))))) shouldBe listOf(true)
        }

        test("skal ignorere utbetaling i samme måned et annet år") {
            erFoersteTrekk(byggKontekst(listOf(historiskBeregning(overfoeres = LocalDate.parse("2025-10-19"))))) shouldBe listOf(true)
        }

        test("skal ignorere historikk uten overføringsdato selv om beregnet er i samme måned") {
            erFoersteTrekk(byggKontekst(listOf(historiskBeregning(overfoeres = null)))) shouldBe listOf(true)
        }

        test("skal bruke beregnet som inneværende dato når overføringsdato mangler") {
            val aktueltInput = input.copy(overfoeringsdato = null)

            erFoersteTrekk(byggKontekst(listOf(historiskBeregning()), aktueltInput = aktueltInput)) shouldBe listOf(true)
            erFoersteTrekk(
                byggKontekst(
                    listOf(historiskBeregning(overfoeres = LocalDate.parse("2026-09-01"))),
                    aktueltInput = aktueltInput,
                ),
            ) shouldBe listOf(false)
        }

        test("skal gi samme beslutning uavhengig av rekkefølgen på historikken") {
            val historikk =
                listOf(
                    historiskBeregning(overfoeres = null),
                    historiskBeregning(),
                    historiskBeregning(overfoeres = LocalDate.parse("2026-09-30")),
                )

            erFoersteTrekk(byggKontekst(historikk)) shouldBe listOf(false)
            erFoersteTrekk(byggKontekst(historikk.reversed())) shouldBe listOf(false)
        }

        test("skal gi samme beslutning i alle segmenter og delperioder for oppdraget") {
            val linje = oppdrag.alleOppdragslinjer.first()
            val oppdeltOppdrag =
                oppdrag.medOppdragslinjer(
                    listOf(
                        linje.copy(utbetalesTilId = AktoerId(100)),
                        linje.copy(oppdragslinjekjedeId = OppdragslinjekjedeId(-2), utbetalesTilId = AktoerId(200), delytelseId = "annen"),
                    ),
                )
            val trekk =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.LOPM,
                    type = TestObjects.TrekkType.YTELSESPERIODE_SKATTEREDUSERENDE,
                    perioder = listOf(periode),
                    sats = BigDecimal("100"),
                )

            listOf(emptyList(), listOf(historiskBeregning())).forEach { historikk ->
                val kontekst = byggKontekst(historikk, aktuelleOppdrag = listOf(oppdeltOppdrag), trekk = listOf(trekk))

                kontekst.segmenter shouldHaveSize 2
                kontekst.segmenter.forEach { it.delperioder shouldHaveSize 2 }
                erFoersteTrekk(kontekst) shouldBe List(4) { historikk.isEmpty() }
            }
        }

        test("skal beregne separat trekkhistorikk for to oppdrag i samme kontekst") {
            val annetOppdrag = oppdrag.copy(id = OppdragId(999))
            val kontekst = byggKontekst(listOf(historiskBeregning()), aktuelleOppdrag = listOf(oppdrag, annetOppdrag))
            val delperioder = kontekst.segmenter.flatMap { it.delperioder }

            delperioder shouldHaveSize 2
            delperioder.map { it.oppdrag.id }.zip(erFoersteTrekk(kontekst)).toMap() shouldBe mapOf(oppdrag.id to false, annetOppdrag.id to true)
        }
    })
