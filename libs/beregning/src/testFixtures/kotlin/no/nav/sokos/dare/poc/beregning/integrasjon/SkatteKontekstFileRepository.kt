package no.nav.sokos.dare.poc.beregning.integrasjon

import java.io.File

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

import no.nav.sokos.dare.poc.beregning.fordeler.SkatteKontekst

object SkatteKontekstFileRepository {
    private val json: ObjectMapper =
        ObjectMapper()
            .registerModule(JavaTimeModule())
            .registerKotlinModule()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .enable(SerializationFeature.INDENT_OUTPUT)

    fun lagre(
        skatteKontekst: SkatteKontekst,
        dir: File,
        filnavn: String,
    ) {
        dir.mkdirs()
        File(dir, filnavn).writeText(json.writeValueAsString(skatteKontekst))
    }

    fun les(fil: File): SkatteKontekst {
        println("Leser fil ${fil.absolutePath}")
        return json.readValue(fil.readText())
    }

    fun les(
        dir: File,
        filnavn: String,
    ): SkatteKontekst = les(File(dir, filnavn))
}
