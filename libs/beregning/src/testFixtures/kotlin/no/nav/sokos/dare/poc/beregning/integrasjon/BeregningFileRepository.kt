package no.nav.sokos.dare.poc.beregning.integrasjon

import java.io.File

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

import no.nav.sokos.dare.poc.BigDecimalSerializer
import no.nav.sokos.dare.poc.LocalDateSerializer
import no.nav.sokos.dare.poc.LocalDateTimeSerializer
import no.nav.sokos.dare.poc.beregning.Beregning

object BeregningFileRepository {
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
        beregninger: List<Beregning>,
        filnavn: String,
    ) {
        val outputDir = IntegrationTestConstants.outputDir
        outputDir.mkdirs()
        File(outputDir, filnavn).writeText(json.encodeToString(beregninger))
    }

    fun lagre(
        beregninger: List<Beregning>,
        dir: File,
        filnavn: String,
    ) {
        dir.mkdirs()
        File(dir, filnavn).writeText(json.encodeToString(beregninger))
    }

    fun les(filnavn: String): List<Beregning> {
        val fil = File(IntegrationTestConstants.outputDir, filnavn)
        println("Leser fil ${fil.absolutePath}")
        return json.decodeFromString(fil.readText())
    }

    fun les(fil: File): List<Beregning> = json.decodeFromString(fil.readText())
}
