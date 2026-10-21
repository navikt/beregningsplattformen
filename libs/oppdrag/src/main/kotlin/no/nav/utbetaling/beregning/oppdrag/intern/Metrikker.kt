package no.nav.utbetaling.beregning.oppdrag.intern

import tools.jackson.databind.JsonNode

import no.nav.utbetaling.beregning.oppdrag.Oppdrag
import no.nav.utbetaling.beregning.oppdrag.intern.FraXmlFormat.xmlMapper
import no.nav.utbetaling.beregning.oppdrag.xml.XmlOppdrag

internal object Metrikker {
    fun fra(oppdrag: Iterable<JsonNode>) =
        oppdrag.flatMap { node ->
            fra(
                node =
                    when (node.erFraJson()) {
                        true -> node.spesialhåndterJson()
                        false -> node
                    },
            )
        }

    private fun JsonNode.erFraJson() = hasNonNull("oppdragsLinje150")

    private fun JsonNode.spesialhåndterJson(): JsonNode {
        val oppdrag110 = FraXmlFormat.oppdrag110FraJson(this.toString()).values.single()
        val oppdrag = XmlOppdrag(oppdrag110 = oppdrag110)
        return xmlMapper.valueToTree(oppdrag)
    }

    private fun fra(
        node: JsonNode,
        path: String = "",
        tags: Map<String, String> = emptyMap(),
    ): Set<Oppdrag.Metrikk> {
        if (node.isNull || node.isMissingNode) {
            return emptySet()
        }

        return when {
            node.isObject -> {
                node
                    .properties()
                    .asSequence()
                    .filterNot { it.key in tagKeys }
                    .flatMap { (key, value) ->
                        fra(
                            node = value,
                            path = path.nestePath(key),
                            tags = tags.medTag(node.tagOrNull()),
                        ).asSequence()
                    }.toSet()
            }

            node.isArray ->
                node
                    .asSequence()
                    .flatMap {
                        fra(
                            node = it,
                            path = path,
                            tags = tags,
                        ).asSequence()
                    }.toSet()

            else -> {
                node
                    .verdi()
                    ?.let { value -> path.lagMetrikk(value, tags) }
                    ?: emptySet()
            }
        }
    }

    private fun Map<String, String>.medTag(tag: Pair<String, String>?) = tag?.let { this + it } ?: this

    private fun String.nestePath(key: String) = if (isEmpty()) key else "$this.$key"

    private fun String.lagMetrikk(
        value: String,
        tags: Map<String, String>,
    ) = setOf(
        Oppdrag.Metrikk(
            felt = gyldigMetrikknavn(),
            tags = tags + ("verdi" to metrikkverdi(value)),
        ),
    )

    private fun String.metrikkverdi(value: String) = value.takeIf { this in visVerdierFor }?.gyldigMetrikkverdi() ?: "skjult"

    private fun JsonNode.verdi(): String? {
        val value = asString()
        if (value.isBlank()) return null
        if (value.startsWith("1970-01-01")) return null
        return value
    }

    private fun JsonNode.tagOrNull() =
        tagKeys.firstNotNullOfOrNull { tagKey ->
            get(tagKey)
                ?.takeIf { !it.isNull }
                ?.asString()
                ?.takeIf { it.isNotBlank() }
                ?.let { tagKey to it }
        }

    private val visVerdierFor =
        setOf(
            "oppdrag-110.kodeAksjon",
            "oppdrag-110.kodeEndring",
            "oppdrag-110.utbetFrekvens",
            "oppdrag-110.avstemming-115.kodeKomponent",
            "oppdrag-110.oppdrags-linje-150.typeSats",
            "oppdrag-110.oppdrags-linje-150.brukKjoreplan",
            "oppdrag-110.oppdrags-linje-150.fradragTillegg",
            "oppdrag-110.oppdrags-linje-150.kodeEndringLinje",
            "oppdrag-110.oppdrags-enhet-120.typeEnhet",
        )

    private val tagKeys =
        setOf(
            "kodeFagomraade",
            "kodeKlassifik",
        )

    private fun String.gyldigMetrikknavn() = replace(navnRegex, "_")

    private fun String.gyldigMetrikkverdi() = replace(verdiRegex, "_")

    private val navnRegex = Regex("(^[^a-zA-Z_:])|([^a-zA-Z0-9_:])")
    private val verdiRegex = Regex("([^a-zA-Z0-9_:])")
}
