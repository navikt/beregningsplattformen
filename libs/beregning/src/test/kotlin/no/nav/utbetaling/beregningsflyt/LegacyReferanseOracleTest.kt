package no.nav.utbetaling.beregningsflyt

import java.io.File

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldNotBeSameInstanceAs

import no.nav.sokos.dare.poc.KlassekodeTestdata
import no.nav.sokos.dare.poc.beregning.integrasjon.BeregningsmotorKontekstKomparator
import no.nav.sokos.dare.poc.beregning.integrasjon.IntegrasjonsScenario
import no.nav.sokos.dare.poc.beregning.integrasjon.LegacyReferanseOracle

class LegacyReferanseOracleTest :
    FunSpec({
        val scenarioer = IntegrasjonsScenario.alle().associateBy { it.navn }
        listOf(
            "SatsRelatertTest/utbdato_maanedssats_med_relatert_samme_overfoeringsdato",
            "GrunnleggendeTest/oppdrag_med_et_saldotrekk_som_ikke_utfoeres_fullstendig",
            "SkattFrikortTest/frikort_belopsgrense_uttoemmes",
        ).forEach { navn ->
            test("oracle and timeline repeat independently without mutating fixtures or shared input: $navn") {
                val scenario = scenarioer.getValue(navn)
                val filer = lesFiler(scenario.mappe)
                val utdata = File("build/test-results/integrasjon")
                val opprinneligeUtdata = lesFiler(utdata)
                val klassekoder = KlassekodeTestdata.klassekodeLookup.values.toList()
                val data = scenario.les()
                val separateData = scenario.les()
                data.kontekst shouldBe separateData.kontekst
                data.trekk shouldBe separateData.trekk
                data.relaterteBeregninger shouldBe separateData.relaterteBeregninger
                data.skatteKontekst shouldNotBeSameInstanceAs separateData.skatteKontekst
                data.trekk.zip(separateData.trekk).forEach { (a, b) ->
                    a shouldNotBeSameInstanceAs b
                    a.trekkperioder.zip(b.trekkperioder).forEach { (x, y) -> x shouldNotBeSameInstanceAs y }
                }

                val foersteLegacy = LegacyReferanseOracle.beregn(scenario, klassekoder)
                val foersteTidslinje = beregnTidslinje(data)
                val andreLegacy = LegacyReferanseOracle.beregn(scenario, klassekoder)
                val andreTidslinje = beregnTidslinje(data)
                foersteLegacy shouldNotBeSameInstanceAs andreLegacy
                ResultatSammenligning.sammenlign(foersteTidslinje, foersteLegacy)
                ResultatSammenligning.sammenlign(andreLegacy, foersteLegacy)
                ResultatSammenligning.sammenlign(andreTidslinje, foersteTidslinje)
                BeregningsmotorKontekstKomparator.sammenlign(andreLegacy, data.forventet)
                data.kontekst shouldBe separateData.kontekst
                data.trekk shouldBe separateData.trekk
                data.relaterteBeregninger shouldBe separateData.relaterteBeregninger
                lesFiler(scenario.mappe) shouldBe filer
                lesFiler(utdata) shouldBe opprinneligeUtdata
            }
        }
    })

private fun lesFiler(rot: File): Map<String, String> =
    if (rot.exists()) {
        rot.walkTopDown().filter { it.isFile }.associate { it.relativeTo(rot).invariantSeparatorsPath to it.readText() }
    } else {
        emptyMap()
    }
