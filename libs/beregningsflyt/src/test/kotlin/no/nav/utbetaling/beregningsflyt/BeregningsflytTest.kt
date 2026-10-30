package no.nav.utbetaling.beregningsflyt

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import no.nav.utbetaling.beregning.oppdrag.Oppdrag
import no.nav.utbetaling.beregningsflyt.verifikasjon.Beregning
import no.nav.utbetaling.beregningsflyt.verifikasjon.Delberegning
import no.nav.utbetaling.beregningsflyt.verifikasjon.Periode
import no.nav.utbetaling.beregningsflyt.verifikasjon.Rad
import no.nav.utbetaling.beregningsflyt.verifikasjon.Scenario
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import io.kotest.assertions.fail
import no.nav.utbetaling.beregningsflyt.verifikasjon.EkstraInfo
import no.nav.utbetaling.beregningsflyt.verifikasjon.Kapasitet

class BeregningsflytTest: FunSpec({

    val beregningsflyt = Beregningsflyt()

    withData(nameFn = { it.navn }, Scenario.alle) {
        when (val oppdrag = Oppdrag.fraJson(it.raw.oppdrag)) {
            is Oppdrag.Utbetaling -> {
                val beregninger = beregningsflyt.beregn(oppdrag.oppdragstidslinje).map { beregning ->
                    Beregning(
                        periode = Periode(beregning.periode.fom.toString(), beregning.periode.tom?.toString()),
                        status = beregning.status.name,
                        statusflagg = beregning.statusflagg.map { it.name },
                        overfoeres = it.kontekst.overfoeringsdato,
                        fagomraade = beregning.fagomraade.name,
                        gjelder = it.kontekst.gjelder, // TODO: Rart
                        beregnet = LocalDateTime.parse(it.kontekst.beregnet).toString(), // TODO: Rart
                        beloepTilUtbetaling = beregning.delberegning.sumOf { it.sum }.heltall(),
                        ekstraInfo = ekstraInfo(motorJson.encodeToJsonElement(beregning.ekstraInfo)),
                        delberegninger = beregning.delberegning.map { delberegning ->
                            // sums er samme type som linjene, så de mappes sammen og skilles etterpå.
                            val rader = (delberegning.linjer + delberegning.sums).map { linje ->
                                val typenavn = linje.type.name.lowercase().replaceFirstChar(Char::uppercase)
                                Rad(
                                    type = linje.type.name,
                                    rowName = linje.kodeKlasse?.let { "$typenavn ${it.visningskode}" } ?: typenavn,
                                    columnValues = linje.dagsbeloep.map { it.beloep.heltall() },
                                    rowValue = linje.beloep.heltall(),
                                    ekstra = ekstraInfo(motorJson.encodeToJsonElement(linje.ekstra)),
                                    capacity = linje.kapasitet?.let { kapasitet ->
                                        Kapasitet(
                                            dayCapacity = kapasitet.dagbeloep.values.map { it.heltall() },
                                            lumpCapacity = kapasitet.periodeBeloep.heltall(),
                                        )
                                    },
                                    satstype = linje.satsType?.name,
                                    sats = linje.sats?.toDouble(),
                                    oppdragslinjekjedeId = linje.oppdragslinjekjedeId?.id?.toInt(),
                                    kodeKlasse = linje.kodeKlasse?.kode,
                                    reskontroKonto = linje.reskontroKonto,
                                    trekkVedtakId = linje.trekkvedtakId?.id?.toInt(),
                                )
                            }
                            Delberegning(
                                oppdragId = requireNotNull(delberegning.oppdragId) { "Delberegning mangler oppdragId" }.id.toInt(),
                                columns = delberegning.datoIntervall.dager.map { it.toString() },
                                rows = rader.dropLast(1),
                                ekstraInfo = ekstraInfo(motorJson.encodeToJsonElement(delberegning.ekstraInfo)),
                                datoIntervall = delberegning.datoIntervall.let { Periode(it.fom.toString(), it.tom?.toString()) },
                                sumColumn = delberegning.sumColumn.map { it.heltall() },
                                sums = rader.last(),
                            )
                        },
                    )
                }
                beregninger shouldBe it.resultat.beregninger
            }
            is Oppdrag.Simulering -> fail("Simleringer støttes ikke i beregningsflyten enda")
            is Oppdrag.UgyldigOppdrag -> fail("Feil: ${oppdrag.feil.stackTraceToString()}")
            is Oppdrag.UgyldigKombinasjon -> fail("Feil: ${oppdrag.feil.stackTraceToString()}")
        }
    }
})

private val motorJson = Json { encodeDefaults = true }
private val fasitJson = Json { ignoreUnknownKeys = true }

// Motorens EkstraInfo har fullt klassenavn som type. Fasiten bruker bare klassenavnet.
// template sammenlignes ikke, siden den er avledet av grunn.
private fun ekstraInfo(motor: JsonElement): List<EkstraInfo> =
    motor.jsonArray.map { element ->
        val felt = element.jsonObject - "template"
        val type = felt.getValue("type").jsonPrimitive.content.substringAfterLast('.')
        fasitJson.decodeFromJsonElement<EkstraInfo>(JsonObject(felt + ("type" to JsonPrimitive(type))))
    }

// Feiler i stedet for å runde av hvis motoren gir desimaler.
private fun BigDecimal.heltall() = intValueExact()
