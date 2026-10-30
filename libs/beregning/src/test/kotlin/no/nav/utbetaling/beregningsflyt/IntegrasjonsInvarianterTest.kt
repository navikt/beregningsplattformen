package no.nav.utbetaling.beregningsflyt

import java.io.File
import java.lang.reflect.ParameterizedType
import java.util.UUID

import kotlinx.serialization.json.Json

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldNotBeSameInstanceAs

import no.nav.sokos.dare.poc.KlassekodeTestdata
import no.nav.sokos.dare.poc.beregning.dto.BeregningsmotorKontekstPresentasjon
import no.nav.sokos.dare.poc.beregning.integrasjon.BeregningsmotorKontekstKomparator
import no.nav.sokos.dare.poc.beregning.integrasjon.IntegrasjonsScenario
import no.nav.sokos.dare.poc.beregning.integrasjon.LegacyReferanseOracle
import no.nav.utbetaling.beregning.oppdrag.Opplysninger
import no.nav.utbetaling.beregning.tid.Tidslinjesamling
import no.nav.utbetaling.beregningsflyt.kontekst.BeregningsmotorKontekst
import no.nav.utbetaling.beregningsflyt.kontekst.OppdragDelperiode

class IntegrasjonsInvarianterTest :
    FunSpec({
        val scenarioer = IntegrasjonsScenario.alle().associateBy { it.navn }
        val kjedetScenario = "GrunnleggendeTest/oppdrag_med_komplisert_kjeding"
        val saldoScenario = "GrunnleggendeTest/oppdrag_med_et_saldotrekk_som_ikke_utfoeres_fullstendig"
        val satsScenario = "SatsRelatertTest/utbdato_maanedssats_med_satsboette"

        listOf(kjedetScenario, saldoScenario, satsScenario).forEach { navn ->
            test("fresh original parsing and calculation preserve prior live state and strict output: $navn") {
                val scenario = scenarioer.getValue(navn)
                val data = scenario.les()
                val foerste = beregnTidslinjeMedTilstand(data)
                val presentasjon = foerste.presentasjon()
                val presentasjonsbilde = presentasjon.øyeblikksbilde()
                val boetter = foerste.kontekst.boettetilstand()
                val kapasitet = foerste.kontekst.restkapasiteter()
                val kilde = foerste.oppdrag.tidslinjer.mapValues { it.value.toList() }
                if (navn == kjedetScenario) {
                    foerste.oppdrag.tidslinjer.values
                        .any { tidslinje -> tidslinje.map { it.second.delytelseId }.distinct().size > 1 } shouldBe true
                }
                if (navn == saldoScenario) {
                    foerste.kontekst.saldoBoetter.saldoer.keys
                        .shouldNotBeEmpty()
                    (foerste.kontekst.saldoBoetter.saldoer != foerste.kontekst.saldoBoetter.innkommendeSaldoer) shouldBe true
                }
                if (navn == satsScenario) {
                    foerste.kontekst.satsBoetter.satser.keys
                        .shouldNotBeEmpty()
                    (foerste.kontekst.satsBoetter.satser != foerste.kontekst.satsBoetter.innkommendeSatser) shouldBe true
                }

                val andreData = scenario.les()
                andreData.oppdragJson shouldBe data.oppdragJson
                val andre = beregnTidslinjeMedTilstand(andreData)
                andre.oppdrag shouldNotBeSameInstanceAs foerste.oppdrag
                andre.oppdrag.tidslinjer shouldNotBeSameInstanceAs foerste.oppdrag.tidslinjer
                andre.identitet shouldNotBeSameInstanceAs foerste.identitet
                andre.kontekst shouldNotBeSameInstanceAs foerste.kontekst
                andre.kontekst.satsBoetter shouldNotBeSameInstanceAs foerste.kontekst.satsBoetter
                andre.kontekst.saldoBoetter shouldNotBeSameInstanceAs foerste.kontekst.saldoBoetter
                andre.kontekst.satsBoetter.satser shouldNotBeSameInstanceAs foerste.kontekst.satsBoetter.satser
                andre.kontekst.saldoBoetter.saldoer shouldNotBeSameInstanceAs foerste.kontekst.saldoBoetter.saldoer
                andre.kontekst.segmenter.zip(foerste.kontekst.segmenter).forEach { (nytt, gammelt) ->
                    nytt shouldNotBeSameInstanceAs gammelt
                    nytt.scratchpad shouldNotBeSameInstanceAs gammelt.scratchpad
                    nytt.scratchpad.values.zip(gammelt.scratchpad.values).forEach { (ny, gammel) ->
                        ny shouldNotBeSameInstanceAs gammel
                    }
                }

                // UUIDs belong to each parse; the comparator checks bijective chain relationships instead.
                ResultatSammenligning.sammenlign(andre.presentasjon(), presentasjon)
                val referanse = LegacyReferanseOracle.beregn(scenario, KlassekodeTestdata.klassekodeLookup.values.toList())
                ResultatSammenligning.sammenlign(presentasjon, referanse)
                BeregningsmotorKontekstKomparator.sammenlign(presentasjon, data.forventet)
                presentasjon.øyeblikksbilde() shouldBe presentasjonsbilde
                foerste.presentasjon().øyeblikksbilde() shouldBe presentasjonsbilde
                foerste.kontekst.boettetilstand() shouldBe boetter
                foerste.kontekst.restkapasiteter() shouldBe kapasitet
                foerste.oppdrag.tidslinjer.mapValues { it.value.toList() } shouldBe kilde
            }
        }

        test("subperiod assignment field and getter retain UUID Opplysninger timeline collection types") {
            val felt = OppdragDelperiode::class.java.getDeclaredField("oppdrag")
            val getter = OppdragDelperiode::class.java.getDeclaredMethod("getOppdrag")
            listOf(felt.genericType, getter.genericReturnType).forEach { type ->
                val samling = type.shouldBeInstanceOf<ParameterizedType>()
                samling.rawType shouldBe Tidslinjesamling::class.java
                samling.actualTypeArguments.toList() shouldBe listOf(UUID::class.java, Opplysninger::class.java)
            }
        }

        test("compiled timeline graph including nested classes and captured fields has no legacy aggregate types") {
            val kodeRot =
                File(
                    OppdragDelperiode::class.java.protectionDomain.codeSource.location
                        .toURI(),
                )
            kodeRot.isDirectory shouldBe true
            val klasser =
                File(kodeRot, "no/nav/utbetaling/beregningsflyt")
                    .walkTopDown()
                    .filter { it.isFile && it.extension == "class" }
                    .map {
                        Class.forName(
                            it
                                .relativeTo(kodeRot)
                                .invariantSeparatorsPath
                                .removeSuffix(".class")
                                .replace('/', '.'),
                            false,
                            OppdragDelperiode::class.java.classLoader,
                        )
                    }.toList()
            klasser.shouldNotBeEmpty()
            (OppdragDelperiode::class.java in klasser) shouldBe true
            klasser.forEach { klasse ->
                val typer =
                    klasse.declaredFields.map { it.genericType } +
                        klasse.declaredConstructors.flatMap { it.genericParameterTypes.toList() } +
                        klasse.declaredMethods.flatMap { it.genericParameterTypes.toList() + it.genericReturnType } +
                        klasse.genericInterfaces.toList() + listOfNotNull(klasse.genericSuperclass)
                typer.forEach { type ->
                    withClue("${klasse.name}: ${type.typeName}") {
                        Regex("""no\.nav\.sokos\.dare\.poc\.oppdrag\.(Oppdrag|Oppdragslinje|Oppdragslinjekjede)(?![\w$])""")
                            .containsMatchIn(type.typeName) shouldBe false
                    }
                }
            }
        }

        test("timeline main sources reject legacy aggregate imports construction conversion and closures") {
            val modul =
                generateSequence(File("").absoluteFile) { it.parentFile }
                    .map { File(it, "libs/beregning") }
                    .first { File(it, "src/main/kotlin").isDirectory }
            val kilder =
                File(modul, "src/main/kotlin/no/nav/utbetaling/beregningsflyt")
                    .walkTopDown()
                    .filter { it.isFile && it.extension in setOf("kt", "java") }
                    .toList()
            kilder.shouldNotBeEmpty()
            kilder.forEach { fil ->
                withClue(fil.relativeTo(modul).path) { legacyAggregatReferanser(fil.readText()) shouldBe emptyList() }
            }
        }

        test("source guard detects aliases fully qualified aggregates wildcard imports conversions and captured aggregates") {
            listOf(
                "import no.nav.sokos.dare.poc.oppdrag.Oppdrag",
                "import no.nav.sokos.dare.poc.oppdrag.Oppdragslinje as Linje",
                "import no.nav.sokos.dare.poc.oppdrag.Oppdragslinjekjede as Kjede",
                "import no.nav.sokos.dare.poc.oppdrag.*",
                "typealias Skjult = no.nav.sokos.dare.poc.oppdrag.Oppdrag",
                "val kilde = no.nav.sokos.dare.poc.oppdrag.Oppdragslinje()",
                "fun konverter(): no.nav.sokos.dare.poc.oppdrag.Oppdragslinjekjede = error(\"legacy\")",
                "fun lukk(kilde: Oppdrag): () -> Int = { kilde.hashCode() }",
                "val kjeder: List<Oppdragslinjekjede> = emptyList()",
            ).forEach { kilde ->
                withClue(kilde) { legacyAggregatReferanser(kilde).shouldNotBeEmpty() }
            }
        }

        test("source guard permits scalar identity and classification imports including aliases") {
            listOf("OppdragId", "OppdragslinjekjedeId", "YtelseKlasseKode", "YtelseKlasseKodeRef").forEach { navn ->
                legacyAggregatReferanser("import no.nav.sokos.dare.poc.oppdrag.$navn as Skalar") shouldBe emptyList()
            }
        }
    })

private fun BeregningsmotorKontekst.boettetilstand() =
    listOf(
        satsBoetter.innkommendeSatser.toMap(),
        satsBoetter.satser.toMap(),
        saldoBoetter.innkommendeSaldoer.toMap(),
        saldoBoetter.saldoer.toMap(),
    )

private fun BeregningsmotorKontekst.restkapasiteter() = segmenter.map { segment -> segment.scratchpad.mapValues { it.value.restKapasitet.toMap() } }

private fun BeregningsmotorKontekstPresentasjon.øyeblikksbilde(): String = Json.encodeToString(BeregningsmotorKontekstPresentasjon.serializer(), this)

private fun legacyAggregatReferanser(kilde: String): List<String> {
    val skalarer = setOf("OppdragId", "OppdragslinjekjedeId", "YtelseKlasseKode", "YtelseKlasseKodeRef")
    val normalisert = kilde.replace("`", "").replace(Regex("""\s*\.\s*"""), ".")
    val kvalifiserte =
        Regex("""\bno\.nav\.sokos\.dare\.poc\.oppdrag\.(\w+|\*)""")
            .findAll(normalisert)
            .filter { it.groupValues[1] !in skalarer }
            .map { it.value }
    val aggregater = Regex("""\b(Oppdrag|Oppdragslinje|Oppdragslinjekjede)\b""").findAll(normalisert).map { it.value }
    return (kvalifiserte + aggregater).distinct().toList()
}
