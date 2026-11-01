package no.nav.utbetaling.beregning.oppdrag

import java.time.LocalDate

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class OffentligParserRegresjonTest :
    FunSpec({
        test("unknown assignment root fields are still rejected") {
            Oppdrag.fraJson(parserOppdrag(oppdragsfelt = """, "unknownField": null""")).shouldBeInstanceOf<Oppdrag.UgyldigOppdrag>()
        }

        listOf("", """, "datoOverfores": null""").forEach { datofelt ->
            test("AVAV accepts an absent transfer date: $datofelt") {
                val resultat = Oppdrag.fraJson(parserOppdrag(avventing = parserAvventing("AVAV", datofelt)))
                resultat.avventingsårsak().shouldBeInstanceOf<Avventingsårsak.Avregning>().til shouldBe null
            }
        }

        test("AVAV retains the supplied transfer date") {
            val resultat =
                Oppdrag.fraJson(
                    parserOppdrag(avventing = parserAvventing("AVAV", """, "datoOverfores": "2026-04-17"""")),
                )
            resultat.avventingsårsak() shouldBe Avventingsårsak.Avregning(LocalDate.of(2026, 4, 17))
        }

        test("AVRK still accepts an absent transfer date") {
            Oppdrag.fraJson(parserOppdrag(avventing = parserAvventing("AVRK"))).avventingsårsak() shouldBe
                Avventingsårsak.Refusjonskrav(null)
        }

        test("AVRK still retains the supplied transfer date") {
            val resultat =
                Oppdrag.fraJson(
                    parserOppdrag(avventing = parserAvventing("AVRK", """, "datoOverfores": "2026-04-17"""")),
                )
            resultat.avventingsårsak() shouldBe Avventingsårsak.Refusjonskrav(LocalDate.of(2026, 4, 17))
        }

        test("malformed AVAV transfer dates are still rejected") {
            Oppdrag
                .fraJson(parserOppdrag(avventing = parserAvventing("AVAV", """, "datoOverfores": "not a date"""")))
                .shouldBeInstanceOf<Oppdrag.UgyldigOppdrag>()
        }

        test("unknown assignment line fields are still rejected") {
            Oppdrag.fraJson(parserOppdrag(linjefelt = """, "unknownField": null""")).shouldBeInstanceOf<Oppdrag.UgyldigOppdrag>()
        }

        test("malformed JSON is still rejected") {
            Oppdrag.fraJson("not valid JSON").shouldBeInstanceOf<Oppdrag.UgyldigOppdrag>()
        }
    })

private fun Oppdrag.avventingsårsak() =
    shouldBeInstanceOf<Oppdrag.Utbetaling>()
        .oppdragstidslinje[LocalDate.of(2026, 3, 2)]
        .values
        .single()
        .avventingsårsak
        ?.opplysning

private fun parserAvventing(
    årsak: String,
    datofelt: String = "",
) = """{"datoAvventFom": "2026-03-02", "datoAvventTom": "2026-03-06", "kodeArsak": "$årsak"$datofelt}"""

private fun parserOppdrag(
    linjefelt: String = "",
    avventing: String = "",
    oppdragsfelt: String = "",
) = """
    {
      "kodeAksjon": "1",
      "kodeEndring": "NY",
      "kodeFagomraade": "AAP",
      "fagsystemId": "parser-regresjon",
      "oppdragGjelderId": "11111111111",
      "datoOppdragGjelderFom": "2000-01-01",
      "saksbehId": "Z999999",
      "avvent118": [$avventing],
      "oppdragsLinje150": [{
        "kodeEndringLinje": "NY",
        "delytelseId": "1",
        "kodeKlassifik": "AAPUAA",
        "datoVedtakFom": "2026-03-02",
        "datoVedtakTom": "2026-03-06",
        "sats": 500,
        "fradragTillegg": "T",
        "typeSats": "DAG",
        "saksbehId": "Z999999"
        $linjefelt
      }]
      $oppdragsfelt
    }
    """.trimIndent()
