package no.nav.sokos.dare.poc.beregning.beregningsmotor

import java.math.BigDecimal
import java.time.LocalDateTime

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe

import no.nav.sokos.dare.poc.TestObjects
import no.nav.sokos.dare.poc.TestUtil.oppdragFraFil
import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.beregning.DelberegningType
import no.nav.sokos.dare.poc.beregning.EkstraInfo.Reasons.TREKK
import no.nav.sokos.dare.poc.beregning.fordeler.BeregningsFordeler
import no.nav.sokos.dare.poc.beregning.fordeler.SkatteKontekst
import no.nav.sokos.dare.poc.oppdrag.OppdragId
import no.nav.sokos.dare.poc.testFordelingInput
import no.nav.sokos.dare.poc.trekk.TrekkAlternativ

class TrekkmotorBeregningTest :
    FunSpec({

        val fordeler = BeregningsFordeler(Beregningsmotor(Ytelsesmotor(), Trekkmotor(), Skattemotor()))
        val ytelsesmotor = Ytelsesmotor()
        val trekkmotor = Trekkmotor()

        fun oppdragMedId(id: Long = 1) =
            oppdragFraFil("aapApiTest/simple.xml").let {
                it.copy(id = OppdragId(id))
            }

        test("beregnSkattereduserendeTrekk oppretter trekk delberegningslinje med korrekt type") {
            val trekk =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.LOPD,
                    type = TestObjects.TrekkType.YTELSESPERIODE_SKATTEREDUSERENDE,
                    perioder = listOf(DatoIntervall("2026-03-02", "2026-03-06")),
                    sats = BigDecimal("100"),
                )

            val beregningsPeriode = DatoIntervall("2026-03-02", "2026-03-06")
            val beregningsOppdrag = listOf(oppdragMedId())
            val beregningsTrekk = listOf(trekk)
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
            val trekkLinje =
                scratchpad.beregninger[periode]!!
                    .first { it.type == DelberegningType.TREKK }

            trekkLinje.type shouldBe DelberegningType.TREKK
            trekkLinje.sats shouldBe BigDecimal("100")
        }

        test("beregnSkattereduserendeTrekk reduserer gjeldendeKapasitet med trekk dagsbeloep") {
            val trekk =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.LOPD,
                    type = TestObjects.TrekkType.YTELSESPERIODE_SKATTEREDUSERENDE,
                    perioder = listOf(DatoIntervall("2026-03-02", "2026-03-06")),
                    sats = BigDecimal("100"),
                )

            val beregningsPeriode = DatoIntervall("2026-03-02", "2026-03-06")
            val beregningsOppdrag = listOf(oppdragMedId())
            val beregningsTrekk = listOf(trekk)
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

            // Capture kapasitet before trekk
            val scratchpadBefore = medYtelse.segmenter.first().scratchpad[OppdragId(1)]!!
            val kapasitetBefore = scratchpadBefore.restKapasitet.values.sumOf { it }

            val result = trekkmotor.calculateSkattereduserendeTrekk(medYtelse)

            val scratchpad = result.segmenter.first().scratchpad[OppdragId(1)]!!
            val kapasitetAfter = scratchpad.restKapasitet.values.sumOf { it }

            // Kapasitet should be reduced after trekk
            kapasitetAfter shouldBeLessThan kapasitetBefore

            // Each weekday's kapasitet should be reduced by the trekk dagsbeloep
            // vedtaksSats=974, trekk sats=100 per day → kapasitet reduced by 100 per day
            scratchpad.restKapasitet.values.forEach { kapasitet ->
                kapasitet shouldBe BigDecimal("974") - BigDecimal("100")
            }
        }

        test("beregnSkattereduserendeTrekk trekk sum samsvarer med dagsbeloep sum") {
            val trekk =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.LOPD,
                    type = TestObjects.TrekkType.YTELSESPERIODE_SKATTEREDUSERENDE,
                    perioder = listOf(DatoIntervall("2026-03-02", "2026-03-06")),
                    sats = BigDecimal("100"),
                )

            val beregningsPeriode = DatoIntervall("2026-03-02", "2026-03-06")
            val beregningsOppdrag = listOf(oppdragMedId())
            val beregningsTrekk = listOf(trekk)
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
            val trekkLinje =
                scratchpad.beregninger[periode]!!
                    .first { it.type == DelberegningType.TREKK }

            // 5 weekdays × 100 dagsats = 500 total trekk, stored as negative
            trekkLinje.sum shouldBe BigDecimal("-500")
            trekkLinje.dagsbeloep shouldHaveSize 5
        }

        test("beregnOevrigeTrekk bruker beregningsdato når overfoeringsdato mangler for utbetalingstrekk") {
            val trekk =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.LOPM,
                    type = TestObjects.TrekkType.UTBETALINGSDATO_IKKE_SKATTEREDUSERENDE,
                    perioder = listOf(DatoIntervall("2026-03-01", "2026-03-31")),
                    sats = BigDecimal("200"),
                )

            val beregningsPeriode = DatoIntervall("2026-03-01", "2026-03-31")
            val beregningsOppdrag = listOf(oppdragMedId())
            val beregningsTrekk = listOf(trekk)
            val input =
                testFordelingInput(
                    beregnet = LocalDateTime.of(2026, 3, 10, 12, 0),
                    overfoeringsdato = null,
                )

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
            val result = trekkmotor.calculateOevrigeTrekk(medYtelse)

            val periode = DatoIntervall("2026-03-01", "2026-03-31")
            val scratchpad = result.segmenter.first().scratchpad[OppdragId(1)]!!
            val trekkLinje =
                scratchpad.beregninger[periode]!!
                    .first { it.type == DelberegningType.TREKK }

            trekkLinje.sum shouldBe BigDecimal("-100")
        }

        test("beregnSkattereduserendeTrekk med trekk som overstiger kapasitet begrenses til kapasitet") {
            // Use a trekk sats larger than vedtaksSats (974)
            val trekk =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.LOPD,
                    type = TestObjects.TrekkType.YTELSESPERIODE_SKATTEREDUSERENDE,
                    perioder = listOf(DatoIntervall("2026-03-02", "2026-03-06")),
                    sats = BigDecimal("2000"),
                )

            val beregningsPeriode = DatoIntervall("2026-03-02", "2026-03-06")
            val beregningsOppdrag = listOf(oppdragMedId())
            val beregningsTrekk = listOf(trekk)
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
            val trekkLinje =
                scratchpad.beregninger[periode]!!
                    .first { it.type == DelberegningType.TREKK }

            // Trekk should be limited by kapasitet (974 per day × 5 days = 4870), stored as negative
            trekkLinje.sum shouldBeGreaterThan BigDecimal("-10000") // 2000 × 5 = 10000
            trekkLinje.sum shouldBeLessThan BigDecimal.ZERO

            // gjeldendeKapasitet should be zero or near zero after large trekk
            scratchpad.restKapasitet.values.forEach { kapasitet ->
                kapasitet shouldBe BigDecimal.ZERO
            }
        }

        // --- beregnOevrigeTrekk calculation tests ---

        test("beregnOevrigeTrekk oppretter trekk delberegningslinje med korrekt type") {
            val trekk =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.LOPD,
                    type = TestObjects.TrekkType.YTELSESPERIODE_IKKE_SKATTEREDUSERENDE,
                    perioder = listOf(DatoIntervall("2026-03-02", "2026-03-06")),
                    sats = BigDecimal("100"),
                )

            val beregningsPeriode = DatoIntervall("2026-03-02", "2026-03-06")
            val beregningsOppdrag = listOf(oppdragMedId())
            val beregningsTrekk = listOf(trekk)
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
            val result = trekkmotor.calculateOevrigeTrekk(medYtelse)

            val periode = DatoIntervall("2026-03-02", "2026-03-06")
            val scratchpad = result.segmenter.first().scratchpad[OppdragId(1)]!!
            val trekkLinje =
                scratchpad.beregninger[periode]!!
                    .first { it.type == DelberegningType.TREKK }

            trekkLinje.type shouldBe DelberegningType.TREKK
            trekkLinje.sats shouldBe BigDecimal("100")
        }

        test("beregnOevrigeTrekk reduserer gjeldendeKapasitet med trekk dagsbeloep") {
            val trekk =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.LOPD,
                    type = TestObjects.TrekkType.YTELSESPERIODE_IKKE_SKATTEREDUSERENDE,
                    perioder = listOf(DatoIntervall("2026-03-02", "2026-03-06")),
                    sats = BigDecimal("100"),
                )

            val beregningsPeriode = DatoIntervall("2026-03-02", "2026-03-06")
            val beregningsOppdrag = listOf(oppdragMedId())
            val beregningsTrekk = listOf(trekk)
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
            val kapasitetBefore =
                medYtelse.segmenter
                    .first()
                    .scratchpad[OppdragId(1)]!!
                    .restKapasitet.values
                    .sumOf { it }

            val result = trekkmotor.calculateOevrigeTrekk(medYtelse)

            val scratchpad = result.segmenter.first().scratchpad[OppdragId(1)]!!
            val kapasitetAfter = scratchpad.restKapasitet.values.sumOf { it }

            kapasitetAfter shouldBeLessThan kapasitetBefore

            scratchpad.restKapasitet.values.forEach { kapasitet ->
                kapasitet shouldBe BigDecimal("974") - BigDecimal("100")
            }
        }

        test("beregnOevrigeTrekk trekk sum samsvarer med dagsbeloep sum") {
            val trekk =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.LOPD,
                    type = TestObjects.TrekkType.YTELSESPERIODE_IKKE_SKATTEREDUSERENDE,
                    perioder = listOf(DatoIntervall("2026-03-02", "2026-03-06")),
                    sats = BigDecimal("100"),
                )

            val beregningsPeriode = DatoIntervall("2026-03-02", "2026-03-06")
            val beregningsOppdrag = listOf(oppdragMedId())
            val beregningsTrekk = listOf(trekk)
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
            val result = trekkmotor.calculateOevrigeTrekk(medYtelse)

            val periode = DatoIntervall("2026-03-02", "2026-03-06")
            val scratchpad = result.segmenter.first().scratchpad[OppdragId(1)]!!
            val trekkLinje =
                scratchpad.beregninger[periode]!!
                    .first { it.type == DelberegningType.TREKK }

            // 5 weekdays × 100 dagsats = 500 total trekk, stored as negative
            trekkLinje.sum shouldBe BigDecimal("-500")
            trekkLinje.dagsbeloep shouldHaveSize 5
        }

        test("beregnOevrigeTrekk bruker gjenværende kapasitet etter skattereduserende trekk") {
            val skattereduserendeTrekk =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.LOPD,
                    type = TestObjects.TrekkType.YTELSESPERIODE_SKATTEREDUSERENDE,
                    perioder = listOf(DatoIntervall("2026-03-02", "2026-03-06")),
                    sats = BigDecimal("500"),
                )

            val oevrigTrekk =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.LOPD,
                    type = TestObjects.TrekkType.YTELSESPERIODE_IKKE_SKATTEREDUSERENDE,
                    perioder = listOf(DatoIntervall("2026-03-02", "2026-03-06")),
                    sats = BigDecimal("600"),
                )

            val beregningsPeriode = DatoIntervall("2026-03-02", "2026-03-06")
            val beregningsOppdrag = listOf(oppdragMedId())
            val beregningsTrekk = listOf(skattereduserendeTrekk, oevrigTrekk)
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
            val medSkatteredTrekk = trekkmotor.calculateSkattereduserendeTrekk(medYtelse)

            // After skattereduserende: kapasitet = 974 - 500 = 474 per day
            val periode = DatoIntervall("2026-03-02", "2026-03-06")
            val scratchpadAfterSkatteRed = medSkatteredTrekk.segmenter.first().scratchpad[OppdragId(1)]!!
            scratchpadAfterSkatteRed.restKapasitet.values.forEach { it shouldBe BigDecimal("474") }

            val result = trekkmotor.calculateOevrigeTrekk(medSkatteredTrekk)

            val scratchpad = result.segmenter.first().scratchpad[OppdragId(1)]!!
            val trekkLinjer =
                scratchpad.beregninger[periode]!!
                    .filter { it.type == DelberegningType.TREKK }

            trekkLinjer shouldHaveSize 2

            // øvrig trekk (600/day) is limited by remaining kapasitet (474/day), stored as negative
            val oevrigTrekkLinje = trekkLinjer[1]
            oevrigTrekkLinje.sum shouldBeGreaterThan BigDecimal("-3000") // would be 600 × 5 uncapped
            oevrigTrekkLinje.sum shouldBeLessThan BigDecimal.ZERO

            // All kapasitet consumed
            scratchpad.restKapasitet.values.forEach { it shouldBe BigDecimal.ZERO }
        }

        // --- saldoBoetter tests (line 115 and 155) ---

        test("saldotrekk med tilstrekkelig saldo trekkes normalt") {
            // SALD trekk with belopSaldo large enough → trekk is NOT capped by saldo
            val trekk =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.SALD,
                    type = TestObjects.TrekkType.YTELSESPERIODE_IKKE_SKATTEREDUSERENDE,
                    perioder = listOf(DatoIntervall("2026-03-02", "2026-03-06")),
                    sats = BigDecimal("100"),
                    belopSaldo = BigDecimal("10000"),
                )

            val beregningsPeriode = DatoIntervall("2026-03-02", "2026-03-06")
            val beregningsOppdrag = listOf(oppdragMedId())
            val beregningsTrekk = listOf(trekk)
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
            val result = trekkmotor.calculateOevrigeTrekk(medYtelse)

            val periode = DatoIntervall("2026-03-02", "2026-03-06")
            val scratchpad = result.segmenter.first().scratchpad[OppdragId(1)]!!
            val trekkLinje =
                scratchpad.beregninger[periode]!!
                    .first { it.type == DelberegningType.TREKK }

            // 5 hverdager × 100 dagsats = 500, saldo er 10000 → ikke begrenset, stored as negative
            trekkLinje.sum shouldBe BigDecimal("-500")
        }

        test("saldotrekk med utilstrekkelig saldo begrenses av saldo") {
            // SALD trekk with belopSaldo smaller than normal trekk → trekk is capped
            val trekk =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.SALD,
                    type = TestObjects.TrekkType.YTELSESPERIODE_IKKE_SKATTEREDUSERENDE,
                    perioder = listOf(DatoIntervall("2026-03-02", "2026-03-06")),
                    sats = BigDecimal("100"),
                    belopSaldo = BigDecimal("200"),
                )

            val beregningsPeriode = DatoIntervall("2026-03-02", "2026-03-06")
            val beregningsOppdrag = listOf(oppdragMedId())
            val beregningsTrekk = listOf(trekk)
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
            val result = trekkmotor.calculateOevrigeTrekk(medYtelse)

            val periode = DatoIntervall("2026-03-02", "2026-03-06")
            val scratchpad = result.segmenter.first().scratchpad[OppdragId(1)]!!
            val trekkLinje =
                scratchpad.beregninger[periode]!!
                    .first { it.type == DelberegningType.TREKK }

            // Normal trekk ville vært 500, men saldo er 200 → begrenset til 200, stored as negative
            trekkLinje.sum.compareTo(BigDecimal("-200")) shouldBe 0
        }

        test("brukSaldo reduserer gjenstående saldo etter trekk") {
            val trekk =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.SALD,
                    type = TestObjects.TrekkType.YTELSESPERIODE_IKKE_SKATTEREDUSERENDE,
                    perioder = listOf(DatoIntervall("2026-03-02", "2026-03-06")),
                    sats = BigDecimal("100"),
                    belopSaldo = BigDecimal("10000"),
                )

            val beregningsPeriode = DatoIntervall("2026-03-02", "2026-03-06")
            val beregningsOppdrag = listOf(oppdragMedId())
            val beregningsTrekk = listOf(trekk)
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
            val saldoFoer = kontekst.saldoBoetter.finnRestSaldo(trekk.id!!)

            val medYtelse = ytelsesmotor.beregnYtelse(kontekst)
            trekkmotor.calculateOevrigeTrekk(medYtelse)

            val saldoEtter = kontekst.saldoBoetter.finnRestSaldo(trekk.id)

            // Saldo skal være redusert med trekkbeløpet (500)
            saldoFoer shouldBe BigDecimal("10000")
            saldoEtter shouldBe BigDecimal("10000") - BigDecimal("500")
        }

        test("saldotrekk med saldo lik null trekkes normalt") {
            // belopSaldo = null → ingen saldoBoetter-oppføring, saldo begrenser ikke
            val trekk =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.SALD,
                    type = TestObjects.TrekkType.YTELSESPERIODE_IKKE_SKATTEREDUSERENDE,
                    perioder = listOf(DatoIntervall("2026-03-02", "2026-03-06")),
                    sats = BigDecimal("100"),
                    belopSaldo = null,
                )

            val beregningsPeriode = DatoIntervall("2026-03-02", "2026-03-06")
            val beregningsOppdrag = listOf(oppdragMedId())
            val beregningsTrekk = listOf(trekk)
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
            val result = trekkmotor.calculateOevrigeTrekk(medYtelse)

            val periode = DatoIntervall("2026-03-02", "2026-03-06")
            val scratchpad = result.segmenter.first().scratchpad[OppdragId(1)]!!
            val trekkLinje =
                scratchpad.beregninger[periode]!!
                    .first { it.type == DelberegningType.TREKK }

            // gjenstaaendeSaldoBeloep=null → ingen saldobegrensning, normalt trekk, stored as negative
            trekkLinje.sum shouldBe BigDecimal("-500")
        }
    })
