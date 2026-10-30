package no.nav.utbetaling.beregning.oppdrag

import java.time.LocalDate

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

class OffentligParserRegresjonTest :
    FunSpec({
        listOf("utbetalingsgrad150", "tilknyttetMikrofrontend", "valuta160", "tekst160").forEach { felt ->
            val oppdragsnivå = felt == "tilknyttetMikrofrontend"
            val listefelt = felt == "valuta160" || felt == "tekst160"
            val tomVerdi = if (listefelt) "[]" else "null"

            fun json(
                verdi: String,
                påOppdragsnivå: Boolean = oppdragsnivå,
            ): String {
                val metadata = """, "$felt": $verdi"""
                return if (påOppdragsnivå) {
                    parserOppdrag(oppdragsfelt = metadata)
                } else {
                    parserOppdrag(linjefelt = metadata)
                }
            }

            test("legacy $felt accepts only its approved empty form $tomVerdi") {
                Oppdrag
                    .fraJson(json(tomVerdi))
                    .shouldBeInstanceOf<Oppdrag.Utbetaling>()
                    .oppdragstidslinje
                    .isEmpty() shouldBe false
            }

            val avvisteVerdier =
                listOf(""""populated"""", """""""", "0", "100", "false", "true", "{}", """{"verdi": 100}""", "[{}]", "[null]") +
                    if (listefelt) listOf("null") else listOf("[]")
            avvisteVerdier.forEach { verdi ->
                test("unsupported legacy $felt rejects $verdi with a field-specific error") {
                    val feil = Oppdrag.fraJson(json(verdi)).shouldBeInstanceOf<Oppdrag.UgyldigOppdrag>().feil
                    generateSequence(feil) { it.cause }.joinToString { it.message.orEmpty() } shouldContain felt
                }
            }

            test("legacy $felt is rejected at the wrong level") {
                Oppdrag.fraJson(json(tomVerdi, !oppdragsnivå)).shouldBeInstanceOf<Oppdrag.UgyldigOppdrag>()
            }
        }

        test("all approved empty legacy placeholders can be combined") {
            Oppdrag
                .fraJson(
                    parserOppdrag(
                        linjefelt = """, "utbetalingsgrad150": null, "valuta160": [], "tekst160": [], "datoRefFagsystemIdFom": null""",
                        oppdragsfelt = """, "tilknyttetMikrofrontend": null""",
                    ),
                ).shouldBeInstanceOf<Oppdrag.Utbetaling>()
        }

        test("unknown assignment root fields are still rejected") {
            Oppdrag.fraJson(parserOppdrag(oppdragsfelt = """, "unknownField": null""")).shouldBeInstanceOf<Oppdrag.UgyldigOppdrag>()
        }

        test("legacy reference date metadata accepts null on the assignment line") {
            val resultat = Oppdrag.fraJson(parserOppdrag(linjefelt = """, "datoRefFagsystemIdFom": null"""))
            resultat.shouldBeInstanceOf<Oppdrag.Utbetaling>().oppdragstidslinje.isEmpty() shouldBe false
        }

        listOf(""""2026-03-01"""", """""""", "0", "false", "[]", "{}").forEach { verdi ->
            test("populated unsupported legacy reference date is rejected: $verdi") {
                val resultat = Oppdrag.fraJson(parserOppdrag(linjefelt = """, "datoRefFagsystemIdFom": $verdi"""))
                val feil = resultat.shouldBeInstanceOf<Oppdrag.UgyldigOppdrag>().feil
                generateSequence(feil) { it.cause }.joinToString { it.message.orEmpty() } shouldContain "datoRefFagsystemIdFom"
            }
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

        test("legacy reference date metadata is not accepted at assignment root") {
            Oppdrag.fraJson(parserOppdrag(oppdragsfelt = """, "datoRefFagsystemIdFom": null""")).shouldBeInstanceOf<Oppdrag.UgyldigOppdrag>()
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
