package no.nav.utbetaling.beregning.oppdrag

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class OppdragTest :
    FunSpec({
        test("Ingen oppdrag gir UgyldigOppdrag") {
            val resultat = Oppdrag.fraJson("[]")

            resultat.shouldBeInstanceOf<Oppdrag.UgyldigOppdrag>()
            resultat.feil.message shouldBe "Det er ikke sendt inn noen oppdrag!"
        }

        test("Ugyldig JSON gir UgyldigOppdrag") {
            val resultat = Oppdrag.fraJson("dette er ikke json")

            resultat.shouldBeInstanceOf<Oppdrag.UgyldigOppdrag>()
            resultat.feil.message shouldBe "Ugyldig JSON!"
        }

        test("Oppdrag som ikke kan tolkes gir UgyldigOppdrag") {
            val resultat = Oppdrag.fraJson(oppdragJson(kodeAksjon = "1", fagsystemId = null))

            resultat.shouldBeInstanceOf<Oppdrag.UgyldigOppdrag>()
            resultat.feil.cause?.message shouldBe "Mangler fagsystemId på oppdragsnivå"
        }

        test("Utbetaling og simulering i samme innsending gir UgyldigKombinasjon") {
            val resultat = Oppdrag.fraJson(oppdragJson(kodeAksjon = "1"), oppdragJson(kodeAksjon = "3"))

            resultat.shouldBeInstanceOf<Oppdrag.UgyldigKombinasjon>()
            resultat.feil.message shouldBe "Det er sendt inn aksjonskoder på tvers av utbetaling og simulering!"
        }

        test("Ugyldig kombinasjon sjekkes før oppdragene tolkes") {
            val resultat = Oppdrag.fraJson(oppdragJson(kodeAksjon = "1", fagsystemId = null), oppdragJson(kodeAksjon = "3"))

            resultat.shouldBeInstanceOf<Oppdrag.UgyldigKombinasjon>()
        }
    })

private fun oppdragJson(
    kodeAksjon: String,
    fagsystemId: String? = "fagsystemId",
) = """
    {
      "kodeAksjon": "$kodeAksjon",
      "kodeEndring": "NY",
      "kodeFagomraade": "AAP",
      "fagsystemId": ${fagsystemId?.let { "\"$it\"" }},
      "oppdragGjelderId": "11111111111",
      "datoOppdragGjelderFom": "2000-01-01",
      "saksbehId": "Z999999"
    }
    """.trimIndent()
