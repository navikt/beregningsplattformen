package no.nav.utbetaling.beregning.oppdrag

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import kotlin.jvm.java

import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.ext.javatime.deser.LocalDateDeserializer
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.module.SimpleModule
import tools.jackson.dataformat.xml.XmlMapper
import tools.jackson.module.kotlin.convertValue
import tools.jackson.module.kotlin.kotlinModule

import no.nav.utbetaling.beregning.oppdrag.xml.Oppdrag110
import no.nav.utbetaling.beregning.oppdrag.xml.XmlOppdrag

sealed interface Oppdrag {
    data class Utbetaling(
        val oppdragstidslinje: Oppdragstidslinje,
    ) : Oppdrag

    data class Simulering(
        val oppdragstidslinje: Oppdragstidslinje,
    ) : Oppdrag

    data class UgyldigFormat(
        val feil: String,
    ) : Oppdrag

    data class UgyldigOppdrag(
        val feil: String,
    ) : Oppdrag

    data class UgyldigKombinasjon(
        val feil: String,
    ) : Oppdrag

    companion object {
        fun fraXml(vararg xml: String): Oppdrag {
            val xmlOppdrag =
                xml.mapIndexed { index, enXml ->
                    runCatching { xmlMapper.readValue(enXml, XmlOppdrag::class.java) }
                        .getOrElse { return UgyldigFormat("Feil på xml nummer ${index + 1}: ${it.message}") }
                }

            return fraOppdrag110(xmlOppdrag.mapNotNull { it.oppdrag110 })
        }

        fun fraJson(vararg json: String): Oppdrag {
            val oppdrag110 =
                json.flatMapIndexed { index, enJson ->
                    runCatching {
                        val jsonNode = jsonMapper.readTree(enJson)
                        when {
                            jsonNode.isArray -> jsonMapper.convertValue<List<Oppdrag110>>(jsonNode)
                            jsonNode.isObject -> listOf(jsonMapper.convertValue<Oppdrag110>(jsonNode))
                            else -> return UgyldigFormat("Feil på json nummer ${index + 1}: Må være JSON Objekt eller Array")
                        }
                    }.getOrElse { return UgyldigFormat("Feil på json nummer ${index + 1}: ${it.message}") }
                }

            return fraOppdrag110(oppdrag110)
        }

        private fun fraOppdrag110(oppdrag110: List<Oppdrag110>): Oppdrag {
            val oppdragstidslinje =
                oppdrag110.foldIndexed(Oppdragstidslinje()) { index, sammenslått, etOppdrag110 ->
                    runCatching { sammenslått + Oppdragstidslinje.fraOppdrag110(etOppdrag110) }
                        .getOrElse { return UgyldigOppdrag("Feil på oppdrag nummer ${index + 1}: ${it.message}") }
                }

            val aksjonskoder = oppdrag110.map { it.kodeAksjon.trim() }.toSet()

            return when {
                aksjonskoder.isEmpty() -> UgyldigOppdrag("Det er ikke sendt inn noen oppdrag")
                aksjonskoder.size > 1 -> UgyldigKombinasjon("Det er sendt inn aksjonskoder på tvers av opprettelse og simulering!")
                aksjonskoder.single() == SIMULERING -> Simulering(oppdragstidslinje)
                else -> Utbetaling(oppdragstidslinje)
            }
        }

        private const val SIMULERING = "3"

        private val xmlMapper: ObjectMapper =
            XmlMapper
                .builder()
                .defaultUseWrapper(false)
                .addModule(SimpleModule().addDeserializer(LocalDate::class.java, LocalDateDeserializer(DateTimeFormatter.ISO_OFFSET_DATE)))
                .addModule(kotlinModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
                .build()

        private val jsonMapper: ObjectMapper =
            JsonMapper
                .builder()
                .addModule(kotlinModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
                .build()
    }
}
