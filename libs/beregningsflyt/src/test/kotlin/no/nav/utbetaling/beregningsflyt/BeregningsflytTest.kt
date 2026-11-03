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
import java.time.LocalDate
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
import io.kotest.assertions.throwables.shouldThrow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.modules.SerializersModule
import no.nav.sokos.dare.poc.beregning.EkstraInfo as MotorEkstraInfo
import no.nav.sokos.dare.poc.aktoer.AktoerId
import no.nav.sokos.dare.poc.beregning.TekstEkstraInfo
import no.nav.sokos.dare.poc.beregning.dto.BeregningsmotorKontekstPresentasjon
import no.nav.sokos.dare.poc.beregning.fordeler.SkatteKontekst
import no.nav.utbetaling.beregningsflyt.verifikasjon.EkstraInfo
import no.nav.utbetaling.beregningsflyt.verifikasjon.Kapasitet

class BeregningsflytTest: FunSpec({

    val beregningsflyt = Beregningsflyt()

    withData(nameFn = { it.navn }, Scenario.alle) { scenario ->
        val skatteKontekst = motorJson.decodeFromString<SkatteKontekst>(scenario.raw.skatteKontekst)
        val beregningsperiode = no.nav.utbetaling.beregning.tid.Periode.LukketPeriode(
            fom = LocalDate.parse(scenario.kontekst.periode.fom),
            tom = LocalDate.parse(scenario.kontekst.periode.tom),
        )
        when (val oppdrag = Oppdrag.fraJson(scenario.raw.oppdrag)) {
            is Oppdrag.Utbetaling -> {
                val kontextOgBeregninger = beregningsflyt.beregn(beregningsperiode, oppdrag.oppdragstidslinje, skatteKontekst)

                val beregningsmotorKontekstPresentasjon = BeregningsmotorKontekstPresentasjon(kontextOgBeregninger.first, kontextOgBeregninger.second )
                val modifisertberegningskontekstpresentasjon = beregningsmotorKontekstPresentasjon.copy(beregninger = beregningsmotorKontekstPresentasjon.beregninger.map { beregning ->
                    beregning.copy(gjelder = AktoerId(scenario.kontekst.gjelder.toLong()), overfoeres = LocalDate.parse(scenario.kontekst.overfoeringsdato),
                        beregnet = LocalDateTime.parse(scenario.kontekst.beregnet), utbetalesTil = scenario.kontekst.utbetalesTil?.let { AktoerId(it.toLong()) } )
                })

                modifisertberegningskontekstpresentasjon shouldBe motorJson.decodeFromString(scenario.raw.resultat)
            }
            is Oppdrag.Simulering -> fail("Simleringer støttes ikke i beregningsflyten enda")
            is Oppdrag.UgyldigOppdrag -> fail("Feil: ${oppdrag.feil.stackTraceToString()}")
            is Oppdrag.UgyldigKombinasjon -> fail("Feil: ${oppdrag.feil.stackTraceToString()}")
        }
    }
})

private val motorJson = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
    serializersModule = SerializersModule {
        polymorphicDefaultDeserializer(MotorEkstraInfo::class) { type ->
            when (type) {
                "TekstEkstraInfo" -> TekstEkstraInfo.serializer()
                else -> null
            }
        }
    }
}

class MotorJsonTest : FunSpec({
    test("dekoder kort og fullstendig typenavn for motorens EkstraInfo") {
        val forventet = TekstEkstraInfo(MotorEkstraInfo.Reasons.SKATT_TILLEGGSOPPLYSNING)
        listOf("TekstEkstraInfo", TekstEkstraInfo.serializer().descriptor.serialName).forEach { type ->
            motorJson.decodeFromString<MotorEkstraInfo>(
                """{"type":"$type","grunn":"SKATT_TILLEGGSOPPLYSNING"}""",
            ) shouldBe forventet
        }
    }

    test("beholder motorens fullstendige typenavn ved serialisering") {
        val info: MotorEkstraInfo = TekstEkstraInfo(MotorEkstraInfo.Reasons.SKATT_TILLEGGSOPPLYSNING)
        motorJson.encodeToJsonElement(info).jsonObject.getValue("type").jsonPrimitive.content shouldBe
            "no.nav.sokos.dare.poc.beregning.TekstEkstraInfo"
    }

    test("avviser ukjente typer for motorens EkstraInfo") {
        shouldThrow<SerializationException> {
            motorJson.decodeFromString<MotorEkstraInfo>(
                """{"type":"UkjentEkstraInfo","grunn":"SKATT_TILLEGGSOPPLYSNING"}""",
            )
        }
    }
})
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
