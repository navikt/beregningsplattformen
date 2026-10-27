package no.nav.sokos.dare.poc.beregning.fordeler

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

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
import no.nav.sokos.dare.poc.trekk.Faggruppe

class BeregningsFordelerSkattekortTest :
    FunSpec({

        val fordeler = BeregningsFordeler(Beregningsmotor(Ytelsesmotor(), Trekkmotor(), Skattemotor()))
        val periode = DatoIntervall("2026-02-01", "2026-03-15")
        val oppdrag = listOf(oppdragFraFil("aapApiTest/simple.xml"))
        val input = testFordelingInput()

        test("byggBeregningsmotorKontekst uten skattekort setter skatteKontekst med null skattekort") {
            val result =
                fordeler.byggBeregningsmotorKontekst(
                    periode = periode,
                    oppdrag = oppdrag,
                    trekk = emptyList(),
                    input = input,
                    skatteKontekst = SkatteKontekst(skattekort = null, allokeringer = emptyMap()),
                    relaterteBeregninger = emptyList(),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )
            result.skatteKontekst shouldBe SkatteKontekst(skattekort = null, allokeringer = emptyMap())
            result.skatteKontekst.skattekort shouldBe null
        }

        test("byggBeregningsmotorKontekst med prosentkort skattekort sender skattekort til skatteKontekst") {
            val skattekort =
                Skattekort(
                    aktoerId = AktoerId(1),
                    inntektsaar = 2026,
                    kilde = "TEST",
                    identifikator = 1,
                    utstedtDato = LocalDate.of(2026, 1, 1),
                    forskuddsTrekk =
                        listOf(
                            Forskuddstrekk(
                                typeForskuddsTrekk = ForskuddstrekkType.PROSENTKORT,
                                trekkKode = ForskuddstrekkKode.LOENN_FRA_NAV,
                                prosentsats = BigDecimal("25.0"),
                            ),
                        ),
                )

            val result =
                fordeler.byggBeregningsmotorKontekst(
                    periode = periode,
                    oppdrag = oppdrag,
                    trekk = emptyList(),
                    input = input,
                    skatteKontekst = SkatteKontekst(skattekort = skattekort, allokeringer = emptyMap()),
                    relaterteBeregninger = emptyList(),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )

            result.skatteKontekst.skattekort shouldBe skattekort
            val resultSkattekort = result.skatteKontekst.skattekort!!
            resultSkattekort.aktoerId shouldBe AktoerId(1)
            resultSkattekort.inntektsaar shouldBe 2026
            resultSkattekort.forskuddsTrekk.size shouldBe 1
            resultSkattekort.forskuddsTrekk.first().typeForskuddsTrekk shouldBe ForskuddstrekkType.PROSENTKORT
            resultSkattekort.forskuddsTrekk.first().prosentsats shouldBe BigDecimal("25.0")
        }

        test("byggBeregningsmotorKontekst med tabellkort skattekort sender skattekort til skatteKontekst") {
            val skattekort =
                Skattekort(
                    aktoerId = AktoerId(1),
                    inntektsaar = 2026,
                    kilde = "SKE",
                    identifikator = 2,
                    utstedtDato = LocalDate.of(2026, 1, 1),
                    forskuddsTrekk =
                        listOf(
                            Forskuddstrekk(
                                typeForskuddsTrekk = ForskuddstrekkType.TABELLKORT,
                                trekkKode = ForskuddstrekkKode.LOENN_FRA_NAV,
                                trekktabell = "7100",
                            ),
                        ),
                )

            val result =
                fordeler.byggBeregningsmotorKontekst(
                    periode = periode,
                    oppdrag = oppdrag,
                    trekk = emptyList(),
                    input = input,
                    skatteKontekst = SkatteKontekst(skattekort = skattekort, allokeringer = emptyMap()),
                    relaterteBeregninger = emptyList(),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )

            result.skatteKontekst.skattekort shouldBe skattekort
            val resultSkattekort = result.skatteKontekst.skattekort!!
            resultSkattekort.kilde shouldBe "SKE"
            resultSkattekort.forskuddsTrekk.first().typeForskuddsTrekk shouldBe ForskuddstrekkType.TABELLKORT
            resultSkattekort.forskuddsTrekk.first().trekktabell shouldBe "7100"
        }

        test("byggBeregningsmotorKontekst med allokeringer sender allokeringer til skatteKontekst") {
            val allokeringer =
                mapOf(
                    ForskuddstrekkKode.LOENN_FRA_NAV to Faggruppe.ARBYT,
                    ForskuddstrekkKode.PENSJON_FRA_NAV to Faggruppe.ARBYT,
                )

            val result =
                fordeler.byggBeregningsmotorKontekst(
                    periode = periode,
                    oppdrag = oppdrag,
                    trekk = emptyList(),
                    input = input,
                    skatteKontekst = SkatteKontekst(skattekort = null, allokeringer = allokeringer),
                    relaterteBeregninger = emptyList(),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )

            result.skatteKontekst.allokeringer shouldBe allokeringer
            result.skatteKontekst.skattekort shouldBe null
        }

        test("byggBeregningsmotorKontekst med tomme allokeringer sender tomt map") {
            val result =
                fordeler.byggBeregningsmotorKontekst(
                    periode = periode,
                    oppdrag = oppdrag,
                    trekk = emptyList(),
                    input = input,
                    skatteKontekst = SkatteKontekst(skattekort = null, allokeringer = emptyMap()),
                    relaterteBeregninger = emptyList(),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )
            result.skatteKontekst.allokeringer shouldBe emptyMap()
        }
    })
