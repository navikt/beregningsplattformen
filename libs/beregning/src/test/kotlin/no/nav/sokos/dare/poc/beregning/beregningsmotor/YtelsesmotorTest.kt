package no.nav.sokos.dare.poc.beregning.beregningsmotor

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe

import no.nav.sokos.dare.poc.TestObjects
import no.nav.sokos.dare.poc.TestUtil.oppdragFraFil
import no.nav.sokos.dare.poc.aktoer.AktoerId
import no.nav.sokos.dare.poc.beregning.Beregning
import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.beregning.Delberegning
import no.nav.sokos.dare.poc.beregning.DelberegningType
import no.nav.sokos.dare.poc.beregning.Delberegningslinje
import no.nav.sokos.dare.poc.beregning.LinjeKlassekode
import no.nav.sokos.dare.poc.beregning.fordeler.BeregningsFordeler
import no.nav.sokos.dare.poc.beregning.fordeler.SkatteKontekst
import no.nav.sokos.dare.poc.oppdrag.OppdragId
import no.nav.sokos.dare.poc.oppdrag.YtelseKlasseKode
import no.nav.sokos.dare.poc.oppdrag.YtelseKlasseKodeRef
import no.nav.sokos.dare.poc.testFordelingInput
import no.nav.sokos.dare.poc.trekk.FagomraadeKode
import no.nav.sokos.dare.poc.trekk.TrekkAlternativ
import no.nav.sokos.dare.poc.trekk.Trekkvedtak

class YtelsesmotorTest :
    FunSpec({

        val fordeler = BeregningsFordeler(Beregningsmotor(Ytelsesmotor(), Trekkmotor(), Skattemotor()))
        val ytelsesmotor = Ytelsesmotor()

        fun oppdragMedId(
            id: Long = 1,
            fileName: String = "aapApiTest/simple.xml",
        ) = oppdragFraFil(fileName).let {
            it.copy(id = OppdragId(id))
        }

        test("beregnYtelse legger til ytelseBeregning i scratchpad for enkelt tilfelle") {
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
            val result = ytelsesmotor.beregnYtelse(kontekst)

            result.segmenter shouldHaveSize 1
            val segment = result.segmenter.first()

            val periode = DatoIntervall("2026-03-02", "2026-03-06")
            segment.scratchpad shouldContainKey OppdragId(1)

            val scratchpad = segment.scratchpad[OppdragId(1)]!!
            scratchpad.beregninger shouldHaveSize 1

            scratchpad.beregninger shouldContainKey periode

            val linjer = scratchpad.beregninger[periode]!!
            linjer shouldHaveSize 1
            linjer.first().type shouldBe DelberegningType.YTELSE
            linjer.first().sats shouldBe BigDecimal("974")
        }

        test("beregnYtelse initialiserer gjeldendeKapasitet med vedtaksSats for hver ukedag") {
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
            val result = ytelsesmotor.beregnYtelse(kontekst)

            val segment = result.segmenter.first()
            val periode = DatoIntervall("2026-03-02", "2026-03-06")
            val scratchpad = segment.scratchpad[OppdragId(1)]!!
            scratchpad.restKapasitet shouldHaveSize 5
            scratchpad.restKapasitet.values.forEach { it shouldBe BigDecimal("974") }
        }

        test("beregnYtelse hopper over helger i gjeldendeKapasitet") {
            val beregningsPeriode = DatoIntervall("2026-03-06", "2026-03-09")
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
            val result = ytelsesmotor.beregnYtelse(kontekst)

            val segment = result.segmenter.first()
            val periode = DatoIntervall("2026-03-06", "2026-03-09")
            val scratchpad = segment.scratchpad[OppdragId(1)]!!
            scratchpad.restKapasitet shouldHaveSize 2
            scratchpad.restKapasitet.keys
                .map { it.toString() }
                .toSet() shouldBe
                setOf("2026-03-06", "2026-03-09")
        }

        test("beregnYtelse med månedlig splitt oppretter scratchpad-oppføringer per delperiode") {
            val vedtak =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.LOPM,
                    type = TestObjects.TrekkType.YTELSESPERIODE_SKATTEREDUSERENDE,
                    perioder = listOf(DatoIntervall("2026-02-01", "2026-03-15")),
                    sats = BigDecimal("1000"),
                )

            val beregningsPeriode = DatoIntervall("2026-02-01", "2026-03-15")
            val beregningsOppdrag = listOf(oppdragMedId(fileName = "testdata/YtelsesmotorTest/enkel-ytelse-feb-til-mid-mar.xml"))
            val beregningsTrekk = listOf(vedtak)
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
            val result = ytelsesmotor.beregnYtelse(kontekst)

            val segment = result.segmenter.first()

            // One oppdrag, two monthly delperioder → one scratchpad entry keyed by OppdragId
            segment.scratchpad shouldHaveSize 1
            segment.scratchpad shouldContainKey OppdragId(1)

            val scratchpad = segment.scratchpad[OppdragId(1)]!!
            // Two delperioder share same scratchpad → two beregninger entries
            scratchpad.beregninger shouldHaveSize 2
            scratchpad.beregninger.values.forEach { linjer ->
                linjer shouldHaveSize 1
                linjer.first().type shouldBe DelberegningType.YTELSE
            }
        }

        test("beregnYtelse med flere oppdrag akkumulerer i samme scratchpad per periode") {
            val beregningsPeriode = DatoIntervall("2026-03-02", "2026-03-06")
            val beregningsOppdrag = listOf(oppdragMedId(10), oppdragMedId(20))
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
            val result = ytelsesmotor.beregnYtelse(kontekst)

            val segment = result.segmenter.first()
            val periode = DatoIntervall("2026-03-02", "2026-03-06")

            // Two oppdrag → two scratchpad entries (one per oppdrag)
            segment.scratchpad shouldHaveSize 2
            segment.scratchpad shouldContainKey OppdragId(10)
            segment.scratchpad shouldContainKey OppdragId(20)

            val scratchpad10 = segment.scratchpad[OppdragId(10)]!!
            val scratchpad20 = segment.scratchpad[OppdragId(20)]!!
            // Each oppdrag has its own ytelse beregningslinje
            scratchpad10.beregninger[periode]!! shouldHaveSize 1
            scratchpad20.beregninger[periode]!! shouldHaveSize 1
            scratchpad10.restKapasitet shouldHaveSize 5
            scratchpad20.restKapasitet shouldHaveSize 5
        }

        test("beregnYtelse should set erEtterbetaling to false when no tidligere utbetalinger exist") {
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
            val result = ytelsesmotor.beregnYtelse(kontekst)

            val segment = result.segmenter.first()
            val periode = DatoIntervall("2026-03-02", "2026-03-06")
            val scratchpad = segment.scratchpad[OppdragId(1)]!!

            scratchpad.erEtterbetaling shouldContainKey periode
            scratchpad.erEtterbetaling[periode] shouldBe false
        }

        test("beregnYtelse should set erEtterbetaling to true when tidligere utbetalinger exist") {
            val periode = DatoIntervall("2026-03-02", "2026-03-06")

            val relatertBeregning =
                Beregning(
                    periode = periode,
                    konto = "",
                    overfoeres = LocalDate.of(2026, 3, 1),
                    fagomraade = FagomraadeKode.AAP,
                    gjelder = AktoerId(1),
                    beregnet = LocalDateTime.of(2026, 3, 1, 12, 0),
                    delberegning =
                        listOf(
                            Delberegning(
                                oppdragId = OppdragId(1),
                                linjer =
                                    listOf(
                                        Delberegningslinje(
                                            type = DelberegningType.YTELSE,
                                            dagsbeloep = emptyList(),
                                            beloep = BigDecimal("4870"),
                                            kodeKlasse =
                                                LinjeKlassekode.Ytelse(
                                                    YtelseKlasseKode(
                                                        kode = YtelseKlasseKodeRef("AAPUAA"),
                                                        beskrivelse = "Test",
                                                        grunnlagSkatt = false,
                                                        grunnlagTrekk = false,
                                                        fagomraader = emptyList(),
                                                    ),
                                                ),
                                        ),
                                    ),
                                datoIntervall = periode,
                            ),
                        ),
                )

            val beregningsPeriode = periode
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
                    relaterteBeregninger = listOf(relatertBeregning),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )
            val result = ytelsesmotor.beregnYtelse(kontekst)

            val segment = result.segmenter.first()
            val scratchpad = segment.scratchpad[OppdragId(1)]!!

            scratchpad.erEtterbetaling shouldContainKey periode
            scratchpad.erEtterbetaling[periode] shouldBe true
        }
    })
