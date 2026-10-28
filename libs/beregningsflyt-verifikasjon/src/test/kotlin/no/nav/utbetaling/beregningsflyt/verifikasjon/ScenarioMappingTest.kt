package no.nav.utbetaling.beregningsflyt.verifikasjon

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotBeBlank
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Files

class ScenarioMappingTest : FunSpec({
    test("Scenario.alle finner og deserialiserer alle scenarioer på classpath") {
        Scenario.alle.shouldNotBeEmpty()
    }

    test("hvert scenario har et navn") {
        Scenario.alle.forEach { scenario ->
            scenario.navn.shouldNotBeBlank()
        }
    }

    test("et kjent scenario er med i Scenario.alle") {
        Scenario.alle.map { it.navn } shouldContain "SkattAarsskifteTest/aarsskifte_frikort"
    }

    test("navnene er unike") {
        val navn = Scenario.alle.map { it.navn }
        navn.toSet().size shouldBe navn.size
    }

    test("scenarioene lastes også når lib-en kun finnes som jar-fil") {
        val jar =
            File("build/libs")
                .listFiles { f -> f.extension == "jar" && f.name.contains("SNAPSHOT") }
                ?.firstOrNull()
                ?: error("Fant ingen bygget jar i build/libs")

        val jarLoader = URLClassLoader(arrayOf(jar.toURI().toURL()), null)
        val original = Thread.currentThread().contextClassLoader
        try {
            Thread.currentThread().contextClassLoader = jarLoader

            val navn = Scenariolaster.finnScenarioNavn()
            navn.shouldNotBeEmpty()
            navn shouldContain "SkattAarsskifteTest/aarsskifte_frikort"

            Scenariolaster.last("SkattAarsskifteTest/aarsskifte_frikort").navn shouldBe
                "SkattAarsskifteTest/aarsskifte_frikort"
        } finally {
            Thread.currentThread().contextClassLoader = original
        }
    }

    test("feiler når et scenario-navn finnes både i lib-en og i prosjektet") {
        val jar =
            File("build/libs")
                .listFiles { f -> f.extension == "jar" && f.name.contains("SNAPSHOT") }
                ?.firstOrNull()
                ?: error("Fant ingen bygget jar i build/libs")

        val egetProsjekt = Files.createTempDirectory("eget-scenario").toFile()
        val kollisjon = File(egetProsjekt, "scenario/SkattAarsskifteTest/aarsskifte_frikort")
        kollisjon.mkdirs()
        File(kollisjon, "kontekst.json").writeText("{}")

        val loader = URLClassLoader(arrayOf(jar.toURI().toURL(), egetProsjekt.toURI().toURL()), null)
        val original = Thread.currentThread().contextClassLoader
        try {
            Thread.currentThread().contextClassLoader = loader
            shouldThrow<IllegalArgumentException> {
                Scenariolaster.finnScenarioNavn()
            }.message!! shouldContain "SkattAarsskifteTest/aarsskifte_frikort"
        } finally {
            Thread.currentThread().contextClassLoader = original
            egetProsjekt.deleteRecursively()
        }
    }
})
