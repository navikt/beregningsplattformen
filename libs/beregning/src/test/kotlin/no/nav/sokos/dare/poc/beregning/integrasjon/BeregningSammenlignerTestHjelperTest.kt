package no.nav.sokos.dare.poc.beregning.integrasjon

import java.io.File
import java.math.BigDecimal
import java.time.LocalDateTime

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain

import no.nav.sokos.dare.poc.TestObjects
import no.nav.sokos.dare.poc.TestUtil.oppdragFraFil
import no.nav.sokos.dare.poc.aktoer.AktoerId
import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Beregningsmotor
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Skattemotor
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Trekkmotor
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Ytelsesmotor
import no.nav.sokos.dare.poc.beregning.fordeler.BeregningsFordeler
import no.nav.sokos.dare.poc.beregning.fordeler.SkatteKontekst
import no.nav.sokos.dare.poc.testFordelingInput
import no.nav.sokos.dare.poc.trekk.TrekkAlternativ

class BeregningSammenlignerTestHjelperTest :
    FunSpec({
        val motor = Beregningsmotor(Ytelsesmotor(), Trekkmotor(), Skattemotor())
        val fordeler = BeregningsFordeler(motor)
        val testFilnavn = "komparator-test.json"

        afterSpec {
            File(IntegrationTestConstants.outputDir, testFilnavn).delete()
        }

        test("sammenlignMedFil godkjenner identisk beregning") {
            val vedtak =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.LOPM,
                    type = TestObjects.TrekkType.YTELSESPERIODE_SKATTEREDUSERENDE,
                    perioder = listOf(DatoIntervall("2026-02-01", "2026-02-28")),
                    sats = BigDecimal("1000"),
                )

            val beregningsPeriode = DatoIntervall("2026-02-01", "2026-02-28")
            val beregningsOppdrag = listOf(oppdragFraFil("aapApiTest/enkel-ytelse-feb-til-mid-mar.xml"))
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
            val beregninger = motor.calculate(kontekst).beregninger()

            BeregningFileRepository.lagre(beregninger, testFilnavn)
            BeregningSammenligningTestHjelper.sammenlignMedFil(beregninger, testFilnavn)
        }

        test("sammenlign rapporterer flere avvik samtidig") {
            val vedtak =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.LOPM,
                    type = TestObjects.TrekkType.YTELSESPERIODE_SKATTEREDUSERENDE,
                    perioder = listOf(DatoIntervall("2026-02-01", "2026-02-28")),
                    sats = BigDecimal("1000"),
                )

            val beregningsPeriode1 = DatoIntervall("2026-02-01", "2026-02-28")
            val beregningsOppdrag1 = listOf(oppdragFraFil("aapApiTest/enkel-ytelse-feb-til-mid-mar.xml"))
            val beregningsTrekk1 = listOf(vedtak)
            val input1 = testFordelingInput(konto = "kontoA")
            val beregningsPeriode2 = DatoIntervall("2026-02-01", "2026-02-28")
            val beregningsOppdrag2 = listOf(oppdragFraFil("aapApiTest/enkel-ytelse-feb-til-mid-mar.xml"))
            val beregningsTrekk2 = listOf(vedtak)
            val input2 = testFordelingInput(konto = "kontoB", gjelder = AktoerId(99))

            val kontekst1 =
                fordeler.byggBeregningsmotorKontekst(
                    periode = beregningsPeriode1,
                    oppdrag = beregningsOppdrag1,
                    trekk = beregningsTrekk1,
                    input = input1,
                    skatteKontekst = SkatteKontekst(skattekort = null, allokeringer = emptyMap()),
                    relaterteBeregninger = emptyList(),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )
            val kontekst2 =
                fordeler.byggBeregningsmotorKontekst(
                    periode = beregningsPeriode2,
                    oppdrag = beregningsOppdrag2,
                    trekk = beregningsTrekk2,
                    input = input2,
                    skatteKontekst = SkatteKontekst(skattekort = null, allokeringer = emptyMap()),
                    relaterteBeregninger = emptyList(),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )

            val beregninger1 = motor.calculate(kontekst1).beregninger()
            val beregninger2 =
                motor
                    .calculate(kontekst2)
                    .beregninger()
                    .map { beregning ->
                        beregning.copy(
                            delberegning =
                                beregning.delberegning.map { delberegning ->
                                    delberegning.copy(
                                        linjer =
                                            delberegning.linjer.map { linje ->
                                                linje.copy(beloep = linje.beloep + BigDecimal.ONE)
                                            },
                                    )
                                },
                        )
                    }

            val feil =
                shouldThrow<AssertionError> {
                    BeregningSammenligningTestHjelper.sammenlign(beregninger2, beregninger1)
                }

            feil.message shouldContain "beregning[0].konto"
            feil.message shouldContain "beregning[0].gjelder"
            feil.message shouldContain "beregning[0].delberegninger[0].rows[0].rowValue"
        }
    })
