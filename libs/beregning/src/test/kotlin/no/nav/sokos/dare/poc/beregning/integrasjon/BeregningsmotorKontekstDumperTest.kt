package no.nav.sokos.dare.poc.beregning.integrasjon

import java.io.File
import java.math.BigDecimal
import java.time.LocalDateTime

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

import no.nav.sokos.dare.poc.TestObjects
import no.nav.sokos.dare.poc.TestUtil.oppdragFraFil
import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Beregningsmotor
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Skattemotor
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Trekkmotor
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Ytelsesmotor
import no.nav.sokos.dare.poc.beregning.fordeler.BeregningsFordeler
import no.nav.sokos.dare.poc.beregning.fordeler.SkatteKontekst
import no.nav.sokos.dare.poc.testFordelingInput
import no.nav.sokos.dare.poc.trekk.TrekkAlternativ

class BeregningsmotorKontekstDumperTest :
    FunSpec({
        val motor = Beregningsmotor(Ytelsesmotor(), Trekkmotor(), Skattemotor())
        val fordeler = BeregningsFordeler(motor)

        test("dumper skriver lesbar fil med beregningsresultat") {
            val vedtak =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.LOPM,
                    type = TestObjects.TrekkType.YTELSESPERIODE_SKATTEREDUSERENDE,
                    perioder = listOf(DatoIntervall("2026-02-01", "2026-02-28"), DatoIntervall("2026-03-01", "2026-03-15")),
                    sats = BigDecimal("1000"),
                )

            val beregningsPeriode = DatoIntervall("2026-02-01", "2026-03-15")
            val beregningsOppdrag = listOf(oppdragFraFil("aapApiTest/simple.xml"))
            val beregningsTrekk = listOf(vedtak)
            val input = testFordelingInput(konto = "konto123")

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
            val beregnet = motor.calculate(kontekst)

            BeregningsmotorKontekstDumper.dump(beregnet, "dumper-test.txt")

            val fil = File("build/test-results/integrasjon/dumper-test.txt")
            fil.exists() shouldBe true

            val innhold = fil.readText()
            innhold shouldContain "BEREGNINGSMOTORKONTEKST"
            innhold shouldContain "2026-02-01"
            innhold shouldContain "2026-03-15"
            innhold shouldContain "konto123"
            innhold shouldContain "SALDOBØTTER"
            innhold shouldContain "SATSBØTTER"
            innhold shouldContain "BEREGNINGER"
            innhold shouldContain "YTELSE"
            innhold shouldContain "SKATT"
            innhold shouldContain "TREKK"

            println(innhold)
        }
    })
