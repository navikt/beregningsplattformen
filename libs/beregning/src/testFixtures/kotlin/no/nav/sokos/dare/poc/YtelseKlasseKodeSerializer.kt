package no.nav.sokos.dare.poc

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

import no.nav.sokos.dare.poc.oppdrag.YtelseKlasseKode
import no.nav.sokos.dare.poc.oppdrag.YtelseKlasseKodeRef
import no.nav.sokos.dare.poc.trekk.FagomraadeKode

object YtelseKlasseKodeSerializer : KSerializer<YtelseKlasseKode> {
    @Serializable
    private data class Surrogate(
        val kode: String,
        val beskrivelse: String,
        val art: Art? = null,
        val fagomraader: List<FagomraadeKode> = emptyList(),
    ) {
        @Serializable
        data class Art(
            val grunnlagSkatt: Boolean = false,
            val grunnlagTrekk: Boolean = false,
        )
    }

    override val descriptor: SerialDescriptor = Surrogate.serializer().descriptor

    override fun deserialize(decoder: Decoder): YtelseKlasseKode {
        val s = decoder.decodeSerializableValue(Surrogate.serializer())
        return YtelseKlasseKode(
            kode = YtelseKlasseKodeRef(s.kode),
            beskrivelse = s.beskrivelse,
            grunnlagSkatt = s.art?.grunnlagSkatt ?: false,
            grunnlagTrekk = s.art?.grunnlagTrekk ?: false,
            fagomraader = s.fagomraader,
        )
    }

    override fun serialize(
        encoder: Encoder,
        value: YtelseKlasseKode,
    ) {
        encoder.encodeSerializableValue(
            Surrogate.serializer(),
            Surrogate(
                kode = value.kode.kode,
                beskrivelse = value.beskrivelse,
                art =
                    Surrogate.Art(
                        grunnlagSkatt = value.grunnlagSkatt,
                        grunnlagTrekk = value.grunnlagTrekk,
                    ),
                fagomraader = value.fagomraader,
            ),
        )
    }
}
