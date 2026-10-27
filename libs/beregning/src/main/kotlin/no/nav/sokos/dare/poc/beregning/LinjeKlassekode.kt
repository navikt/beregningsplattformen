package no.nav.sokos.dare.poc.beregning

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

import no.nav.sokos.dare.poc.oppdrag.YtelseKlasseKode
import no.nav.sokos.dare.poc.skattetrekk.SkatteKonto

@Serializable(with = LinjeKlassekodeSerializer::class)
sealed interface LinjeKlassekode {
    val kode: String
    val beskrivelse: String?
    val visningskode: String
        get() =
            when (this) {
                is Ytelse -> ytelse.kode.toString()
                is Skatt -> kode
            }

    data class Ytelse(
        val ytelse: YtelseKlasseKode,
    ) : LinjeKlassekode {
        override val kode: String
            get() = ytelse.kode.kode
        override val beskrivelse: String
            get() = ytelse.beskrivelse
    }

    data class Skatt(
        override val kode: String,
    ) : LinjeKlassekode {
        override val beskrivelse: String? = null

        init {
            require(SkatteKonto.entries.any { it.klassekode == kode }) { "Ukjent skatteklassekode: $kode" }
        }
    }
}

object LinjeKlassekodeSerializer : KSerializer<LinjeKlassekode> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("no.nav.sokos.dare.poc.beregning.LinjeKlassekode") {
            element<String>("kode")
        }

    override fun deserialize(decoder: Decoder): LinjeKlassekode {
        val jsonDecoder =
            decoder as? JsonDecoder
                ?: throw SerializationException("LinjeKlassekode støtter bare JSON-serialisering")
        val jsonObject =
            jsonDecoder.decodeJsonElement() as? JsonObject
                ?: throw SerializationException("LinjeKlassekode må være et JSON-objekt")
        val kode = jsonObject["kode"] as? JsonPrimitive
        if (jsonObject.keys == setOf("kode") && kode?.isString == true) {
            return LinjeKlassekode.Skatt(kode.content)
        }

        val ytelse =
            try {
                jsonDecoder.json.decodeFromJsonElement<YtelseKlasseKode>(jsonObject)
            } catch (exception: SerializationException) {
                throw SerializationException("Ugyldig ytelsesklassekode", exception)
            }
        return LinjeKlassekode.Ytelse(ytelse)
    }

    override fun serialize(
        encoder: Encoder,
        value: LinjeKlassekode,
    ) {
        val jsonEncoder =
            encoder as? JsonEncoder
                ?: throw SerializationException("LinjeKlassekode støtter bare JSON-serialisering")
        val jsonObject =
            when (value) {
                is LinjeKlassekode.Ytelse -> jsonEncoder.json.encodeToJsonElement(value.ytelse)
                is LinjeKlassekode.Skatt -> JsonObject(mapOf("kode" to JsonPrimitive(value.kode)))
            }
        jsonEncoder.encodeJsonElement(jsonObject)
    }
}
