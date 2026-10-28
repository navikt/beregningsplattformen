package no.nav.utbetaling.beregningsflyt.verifikasjon

import kotlinx.serialization.builtins.ListSerializer
import java.net.JarURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Collectors
import kotlinx.serialization.json.Json

/**
 * Leser scenarioer fra classpath-ressursen [ROT]: både de som er pakket i denne lib-jaren,
 * og eventuelle egne scenarioer konsumentprosjektet legger på samme path
 * (f.eks. src/main/resources/scenario eller src/test/resources/scenario).
 */
internal object Scenariolaster {
    fun lastAlle(): List<Scenario> = finnScenarioNavn().sorted().map(::last)

    fun last(navn: String): Scenario {
        val base = "$ROT/$navn"
        return Scenario(
            navn = navn,
            kontekst = scenarioJson.decodeFromString(Kontekst.serializer(), lesTekst("$base/kontekst.json")),
            oppdrag =
                scenarioJson.decodeFromString(
                    ListSerializer(Oppdrag.serializer()),
                    lesTekst("$base/oppdrag.json"),
                ),
            resultat = scenarioJson.decodeFromString(Resultat.serializer(), lesTekst("$base/resultat.json")),
            skatteKontekst =
                scenarioJson.decodeFromString(
                    SkatteKontekst.serializer(),
                    lesTekst("$base/skatteKontekst.json"),
                ),
            trekk =
                scenarioJson.decodeFromString(
                    ListSerializer(Trekkvedtak.serializer()),
                    lesTekst("$base/trekk.json"),
                ),
        )
    }

    fun finnScenarioNavn(): Set<String> {
        val funnet = mutableListOf<String>()
        for (url in classLoader().getResources(ROT).toList()) {
            funnet +=
                when (url.protocol) {
                    "file" -> fraKatalog(url)
                    "jar" -> fraJar(url)
                    else -> emptySet()
                }
        }
        val duplikater = funnet.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        require(duplikater.isEmpty()) {
            "Scenario-navn er definert både i lib-en og i prosjektet: " +
                "${duplikater.sorted().joinToString()}. Gi dine egne scenarioer unike navn under $ROT/."
        }
        return funnet.toCollection(linkedSetOf())
    }

    private fun lesTekst(sti: String): String =
        (classLoader().getResourceAsStream(sti) ?: error("Fant ikke ressurs på classpath: $sti"))
            .bufferedReader()
            .use { it.readText() }

    private fun fraKatalog(url: URL): Set<String> {
        val rot = Paths.get(url.toURI())
        Files.walk(rot).use { stream ->
            return stream
                .filter { Files.isRegularFile(it) && it.fileName.toString() == "kontekst.json" }
                .map { rot.relativize(it.parent).toString().replace('\\', '/') }
                .filter { it.isNotEmpty() }
                .collect(Collectors.toCollection(::linkedSetOf))
        }
    }

    private fun fraJar(url: URL): Set<String> {
        val tilkobling = url.openConnection() as JarURLConnection
        tilkobling.useCaches = false
        tilkobling.jarFile.use { jar ->
            return jar
                .entries()
                .asSequence()
                .map { it.name }
                .filter { it.startsWith("$ROT/") && it.endsWith("/kontekst.json") }
                .map { it.removePrefix("$ROT/").removeSuffix("/kontekst.json") }
                .filter { it.isNotEmpty() }
                .toCollection(linkedSetOf())
        }
    }

    private fun classLoader(): ClassLoader =
        Thread.currentThread().contextClassLoader ?: Scenariolaster::class.java.classLoader

    private const val ROT = "scenario"
    private val scenarioJson: Json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
}
