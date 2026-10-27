package no.nav.sokos.dare.poc.beregning.integrasjon

import java.io.File

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

import no.nav.sokos.dare.poc.BigDecimalSerializer
import no.nav.sokos.dare.poc.LocalDateSerializer
import no.nav.sokos.dare.poc.LocalDateTimeSerializer
import no.nav.sokos.dare.poc.beregning.dto.BeregningsmotorKontekstPresentasjon

object BeregningsmotorKontekstFileRepository {
    private val json =
        Json {
            prettyPrint = true
            encodeDefaults = true
            explicitNulls = false
            ignoreUnknownKeys = true
            serializersModule =
                SerializersModule {
                    contextual(LocalDateSerializer)
                    contextual(LocalDateTimeSerializer)
                    contextual(BigDecimalSerializer)
                }
        }

    fun lagre(
        kontekst: BeregningsmotorKontekstPresentasjon,
        dir: File,
        filnavn: String,
    ) {
        dir.mkdirs()
        val fil = File(dir, filnavn)
        println("Lagrer fil ${fil.absolutePath}")
        fil.writeText(json.encodeToString(kontekst))
    }

    fun les(fil: File): BeregningsmotorKontekstPresentasjon {
        println("Leser fil ${fil.absolutePath}")
        return json.decodeFromString(fil.readText())
    }

    fun les(filnavn: String): BeregningsmotorKontekstPresentasjon = les(File(IntegrationTestConstants.outputDir, filnavn))
}
