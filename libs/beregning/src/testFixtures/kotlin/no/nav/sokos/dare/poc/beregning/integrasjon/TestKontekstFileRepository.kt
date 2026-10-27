package no.nav.sokos.dare.poc.beregning.integrasjon

import java.io.File

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

object TestKontekstFileRepository {
    private val json: ObjectMapper =
        ObjectMapper()
            .registerModule(JavaTimeModule())
            .registerKotlinModule()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .enable(SerializationFeature.INDENT_OUTPUT)

    fun les(fil: File): TestKontekst {
        println("Leser fil ${fil.absolutePath}")
        return json.readValue(fil.readText())
    }

    fun les(
        dir: File,
        filnavn: String,
    ): TestKontekst = les(File(dir, filnavn))
}
