package no.nav.sokos.dare.poc.skattetrekk

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

import no.nav.sokos.dare.poc.skattekort.domain.Tilleggsopplysning

class SkatteKontoTest :
    FunSpec({
        test("skal bevare sju ASCII-sifre i alle reskontrokontoer") {
            SkatteKonto.entries.forEach { konto ->
                konto.konto.matches(Regex("[0-9]{7}")) shouldBe true
                konto.konto.first() shouldBe '0'
            }
        }

        test("skal godta reskontrokonto med sju ASCII-sifre") {
            validerReskontrokonto("0510000")
        }

        test("skal avvise reskontrokontoer som ikke består av sju ASCII-sifre") {
            listOf("510000", "00510000", "051 0000", "051000A", "０５１００００").forEach { ugyldigKonto ->
                val feil =
                    shouldThrow<IllegalArgumentException> {
                        validerReskontrokonto(ugyldigKonto)
                    }

                feil.message shouldBe "Reskontokonto må bestå av sju ASCII-sifre"
            }
        }

        test("skal velge vanlig forskuddstrekk uten relevante tilleggsopplysninger") {
            SkatteKonto.from(emptySet()) shouldBe SkatteKonto.FORSKUDDSTREKK
            SkatteKonto.from(setOf(Tilleggsopplysning.OPPHOLD_I_TILTAKSSONE)) shouldBe SkatteKonto.FORSKUDDSTREKK
        }

        test("skal velge Svalbard for opphold på Svalbard") {
            SkatteKonto.from(setOf(Tilleggsopplysning.OPPHOLD_PAA_SVALBARD)) shouldBe SkatteKonto.FORSKUDDSTREKK_SVALBARD
        }

        test("skal velge kildeskatt for pensjonist") {
            SkatteKonto.from(setOf(Tilleggsopplysning.KILDESKATT_PENSJONIST)) shouldBe SkatteKonto.FORSKUDDSTREKK_KILDESKATT
        }

        test("skal velge kildeskatt for lønn") {
            SkatteKonto.from(setOf(Tilleggsopplysning.KILDESKATT_PAA_LOENN)) shouldBe SkatteKonto.FORSKUDDSTREKK_KILDESKATT
        }

        test("skal velge kildeskatt når tiltakssone også er oppgitt") {
            SkatteKonto.from(
                setOf(
                    Tilleggsopplysning.OPPHOLD_I_TILTAKSSONE,
                    Tilleggsopplysning.KILDESKATT_PAA_LOENN,
                ),
            ) shouldBe SkatteKonto.FORSKUDDSTREKK_KILDESKATT
        }
    })
