package no.nav.utbetaling.beregningsflyt

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

import no.nav.sokos.dare.poc.beregning.integrasjon.IntegrasjonsScenario
import no.nav.utbetaling.beregning.oppdrag.Oppdrag

class TidslinjeInntakTest :
    FunSpec({
        val scenarioer = IntegrasjonsScenario.alle()

        test("all 59 original scenarios are registered") {
            scenarioer.size shouldBe 59
        }

        scenarioer.forEach { scenario ->
            test(scenario.navn) {
                val originalJson = scenario.les().oppdragJson
                val tidslinje = lesTidslinje(originalJson, scenario.navn)
                tidslinje.isEmpty() shouldBe false
                tidslinje.tidslinjer.values.forEach { it.isEmpty() shouldBe false }
            }
        }

        test("malformed JSON fails with the original parser error as cause") {
            val json = "not valid JSON"
            val resultat = Oppdrag.fraJson(json).shouldBeInstanceOf<Oppdrag.UgyldigOppdrag>()

            val feil = shouldThrow<AssertionError> { lesTidslinje(json) }

            feil.message shouldBe "Expected payment, received invalid assignment"
            val årsak = requireNotNull(feil.cause)
            årsak::class shouldBe resultat.feil::class
            årsak.message shouldBe resultat.feil.message
            requireNotNull(årsak.cause)::class shouldBe requireNotNull(resultat.feil.cause)::class
            årsak.cause?.message shouldBe resultat.feil.cause?.message
        }

        test("invalid scenarios include their name and retain the parser cause") {
            val json = "not valid JSON"
            val resultat = Oppdrag.fraJson(json).shouldBeInstanceOf<Oppdrag.UgyldigOppdrag>()

            val feil = shouldThrow<AssertionError> { lesTidslinje(json, "original/scenario") }

            feil.message shouldBe "original/scenario: Expected payment, received invalid assignment"
            val årsak = requireNotNull(feil.cause)
            årsak::class shouldBe resultat.feil::class
            årsak.message shouldBe resultat.feil.message
            requireNotNull(årsak.cause)::class shouldBe requireNotNull(resultat.feil.cause)::class
            årsak.cause?.message shouldBe resultat.feil.cause?.message
        }

        test("mixed payment and simulation fails with the original combination error as cause") {
            val json = "[${inntaksoppdrag("1")}, ${inntaksoppdrag("3")}]"
            val resultat = Oppdrag.fraJson(json).shouldBeInstanceOf<Oppdrag.UgyldigKombinasjon>()

            val feil = shouldThrow<AssertionError> { lesTidslinje(json) }

            feil.message shouldBe "Expected payment, received invalid combination"
            val årsak = requireNotNull(feil.cause)
            årsak::class shouldBe resultat.feil::class
            årsak.message shouldBe resultat.feil.message
        }

        test("simulation fails because payment was expected") {
            val json = inntaksoppdrag("3")
            Oppdrag.fraJson(json).shouldBeInstanceOf<Oppdrag.Simulering>()

            val feil = shouldThrow<AssertionError> { lesTidslinje(json) }

            feil.message shouldBe "Expected payment, received simulation"
        }
    })

private fun inntaksoppdrag(aksjonskode: String) =
    """
    {
      "kodeAksjon": "$aksjonskode",
      "kodeEndring": "NY",
      "kodeFagomraade": "AAP",
      "fagsystemId": "fagsystemId",
      "oppdragGjelderId": "11111111111",
      "datoOppdragGjelderFom": "2000-01-01",
      "saksbehId": "Z999999"
    }
    """.trimIndent()
