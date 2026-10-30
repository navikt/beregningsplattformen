package no.nav.utbetaling.beregningsflyt

import java.io.File
import java.net.URL
import java.nio.file.Files
import java.util.UUID

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

import no.nav.sokos.dare.poc.beregning.integrasjon.BeregningFileRepository
import no.nav.sokos.dare.poc.beregning.integrasjon.BeregningsmotorKontekstFileRepository
import no.nav.sokos.dare.poc.beregning.integrasjon.BeregningsmotorKontekstKomparator
import no.nav.sokos.dare.poc.beregning.integrasjon.IntegrasjonsScenario
import no.nav.sokos.dare.poc.beregning.integrasjon.SkatteKontekstFileRepository
import no.nav.sokos.dare.poc.beregning.integrasjon.TestKontekstFileRepository
import no.nav.sokos.dare.poc.beregning.integrasjon.TrekkFileRepository

class ScenarioTilgangTest :
    FunSpec({
        val beregningModul =
            generateSequence(File("").absoluteFile) { it.parentFile }
                .map { File(it, "libs/beregning") }
                .first { File(it, "src/main/resources/testsupport/integrasjon").isDirectory }
        val originalRot = File(beregningModul, "src/main/resources/testsupport/integrasjon")
        val originaleMapper =
            originalRot
                .walkTopDown()
                .maxDepth(2)
                .filter {
                    it.isDirectory &&
                        it
                            .relativeTo(originalRot)
                            .invariantSeparatorsPath
                            .split('/')
                            .size == 2
                }.filter { File(it, "kontekst.json").isFile }
                .sortedBy { it.relativeTo(originalRot).invariantSeparatorsPath }
                .toList()

        test("shared discovery finds every original scenario once in sorted order") {
            originaleMapper.shouldNotBeEmpty()
            val navn = IntegrasjonsScenario.alle().map { it.navn }
            navn shouldBe originaleMapper.map { it.relativeTo(originalRot).invariantSeparatorsPath }
            navn shouldBe navn.distinct().sorted()
        }

        test("all original required inputs and golden results are loaded with optional related calculations") {
            val scenarioer = IntegrasjonsScenario.alle().associateBy { it.navn }
            originaleMapper.forEach { original ->
                val navn = original.relativeTo(originalRot).invariantSeparatorsPath
                val scenario = scenarioer.getValue(navn)
                scenario.mappe.canonicalFile shouldBe
                    File(beregningModul, "build/resources/main/testsupport/integrasjon/$navn").canonicalFile
                val inndata = scenario.les()
                inndata.scenario shouldBe scenario
                listOf("oppdrag.json", "kontekst.json", "trekk.json", "skatteKontekst.json", "resultat.json").forEach { filnavn ->
                    File(original, filnavn).isFile shouldBe true
                    File(scenario.mappe, filnavn).readText() shouldBe File(original, filnavn).readText()
                }
                inndata.oppdragJson shouldBe File(original, "oppdrag.json").readText()
                inndata.kontekst shouldBe TestKontekstFileRepository.les(File(original, "kontekst.json"))
                inndata.trekk shouldBe TrekkFileRepository.les(File(original, "trekk.json"))
                val skatteFil = File(original, "skatteKontekst.json")
                val originalSkatteKontekst = SkatteKontekstFileRepository.les(skatteFil)
                val skattekortJson = Json.parseToJsonElement(skatteFil.readText()).jsonObject["skattekort"]
                inndata.skatteKontekst shouldBe
                    originalSkatteKontekst.copy(
                        skattekort =
                            originalSkatteKontekst.skattekort?.let { kort ->
                                val innlestKort = requireNotNull(inndata.skatteKontekst.skattekort)
                                val kortJson = requireNotNull(skattekortJson).jsonObject
                                // Only omitted registration timestamps are generated independently on each read.
                                kort.copy(
                                    tidspktReg = if ("tidspktReg" in kortJson) kort.tidspktReg else innlestKort.tidspktReg,
                                    forskuddsTrekk =
                                        kort.forskuddsTrekk.mapIndexed { indeks, trekk ->
                                            if ("tidspktReg" in kortJson.getValue("forskuddsTrekk").jsonArray[indeks].jsonObject) {
                                                trekk
                                            } else {
                                                trekk.copy(tidspktReg = innlestKort.forskuddsTrekk[indeks].tidspktReg)
                                            }
                                        },
                                )
                            },
                    )
                inndata.forventet shouldBe BeregningsmotorKontekstFileRepository.les(File(original, "resultat.json"))
                val relatertFil = File(original, "relaterteBeregninger.json")
                inndata.relaterteBeregninger shouldBe
                    if (relatertFil.exists()) BeregningFileRepository.les(relatertFil) else emptyList()
            }
        }

        test("a missing scenario fails without generating a golden result") {
            val mappe = File("build/scenario-tilgang-missing")
            mappe.exists() shouldBe false
            shouldThrow<IllegalArgumentException> {
                IntegrasjonsScenario("missing/scenario", mappe).les()
            }
            mappe.exists() shouldBe false
        }

        test("the new runner preserves every original fixture byte across all 59 scenarios") {
            originaleMapper.size shouldBe 59
            val foer =
                originalRot
                    .walkTopDown()
                    .filter { it.isFile }
                    .associate { it.relativeTo(originalRot).invariantSeparatorsPath to it.readBytes().toList() }
            originaleMapper.forEach { original ->
                val scenario = IntegrasjonsScenario(original.relativeTo(originalRot).invariantSeparatorsPath, original)
                val data = scenario.les()
                BeregningsmotorKontekstKomparator.sammenlign(beregnTidslinje(data), data.forventet)
            }
            val etter =
                originalRot
                    .walkTopDown()
                    .filter { it.isFile }
                    .associate { it.relativeTo(originalRot).invariantSeparatorsPath to it.readBytes().toList() }
            etter shouldBe foer
        }

        test("a missing golden with all synthetic inputs fails without writing results or changing bytes") {
            val mappe = File("build/scenario-tilgang-missing-golden-${UUID.randomUUID()}")
            val syntetiskeFiler =
                mapOf(
                    "oppdrag.json" to "[]",
                    "kontekst.json" to "{}",
                    "trekk.json" to "[]",
                    "skatteKontekst.json" to "{}",
                )
            Files.createDirectory(mappe.toPath())
            val opprettede = mutableListOf<File>()
            try {
                syntetiskeFiler.forEach { (navn, innhold) ->
                    val fil = File(mappe, navn)
                    Files.write(fil.toPath(), innhold.toByteArray(), java.nio.file.StandardOpenOption.CREATE_NEW)
                    opprettede += fil
                }
                val foer = opprettede.associate { it.name to it.readBytes().toList() }
                val feil =
                    shouldThrow<IllegalArgumentException> {
                        beregnTidslinje(IntegrasjonsScenario("synthetic/missing-golden", mappe).les())
                    }
                feil.message.orEmpty() shouldContain File(mappe, "resultat.json").absolutePath
                File(mappe, "resultat.json").exists() shouldBe false
                mappe.listFiles()?.map { it.name }?.sorted() shouldBe syntetiskeFiler.keys.sorted()
                opprettede.associate { it.name to it.readBytes().toList() } shouldBe foer
            } finally {
                opprettede.forEach { Files.delete(it.toPath()) }
                Files.delete(mappe.toPath())
            }
        }

        test("a missing resource root fails instead of discovering no scenarios") {
            val klasseLaster =
                object : ClassLoader(null) {
                    override fun getResource(name: String): URL? = null
                }
            shouldThrow<IllegalArgumentException> {
                IntegrasjonsScenario.alle(klasseLaster)
            }
        }

        test("a resource root inside a jar is rejected") {
            val klasseLaster =
                object : ClassLoader(null) {
                    override fun getResource(name: String): URL = URL("jar:file:/unused.jar!/testsupport/integrasjon")
                }
            shouldThrow<IllegalArgumentException> {
                IntegrasjonsScenario.alle(klasseLaster)
            }
        }

        test("an empty resource root fails instead of discovering no scenarios") {
            val klasseLaster =
                object : ClassLoader(null) {
                    override fun getResource(name: String): URL = originaleMapper.first().toURI().toURL()
                }
            shouldThrow<IllegalArgumentException> {
                IntegrasjonsScenario.alle(klasseLaster)
            }
        }
    })
