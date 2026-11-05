package no.nav.utbetaling.beregningsflyt

import java.nio.file.Files
import java.nio.file.Path

import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlinx.serialization.json.Json

/**
 * Leser scenarioer fra [ROT], der hvert scenario er en katalog med kontekst.json, oppdrag.json osv.
 * Stien er relativ til modulkatalogen, som er arbeidskatalogen når testene kjøres.
 */
internal object Scenariolaster {
    private val ROT: Path = Path.of("src/test/resources/scenario")

    fun lastAlle(): List<Scenario> = finnScenarioNavn().sorted().map(::last)

    fun last(navn: String): Scenario {
        val katalog = ROT.resolve(navn)
        return Scenario(
            navn = navn,
            kontekst = scenarioJson.decodeFromString(Kontekst.serializer(), katalog.resolve("kontekst.json").readText()),
            oppdrag = katalog.resolve("oppdrag.json").readText(),
            skatteKontekst = katalog.resolve("skatteKontekst.json").readText(),
            trekk = katalog.resolve("trekk.json").readText(),
            resultat = katalog.resolve("resultat.json").readText(),
        )
    }

    private fun finnScenarioNavn(): List<String> =
        Files.walk(ROT).use { stier ->
            stier
                .filter { it.name == "kontekst.json" }
                .map { ROT.relativize(it.parent).invariantSeparatorsPathString }
                .toList()
        }

    private val scenarioJson: Json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
}
