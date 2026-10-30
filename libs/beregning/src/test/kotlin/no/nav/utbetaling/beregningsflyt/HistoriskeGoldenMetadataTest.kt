package no.nav.utbetaling.beregningsflyt

import java.math.BigDecimal

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

import no.nav.sokos.dare.poc.KlassekodeTestdata
import no.nav.sokos.dare.poc.aktoer.AktoerId
import no.nav.sokos.dare.poc.beregning.DelberegningType
import no.nav.sokos.dare.poc.beregning.EkstraInfo
import no.nav.sokos.dare.poc.beregning.TekstEkstraInfo
import no.nav.sokos.dare.poc.beregning.dto.Capacity
import no.nav.sokos.dare.poc.beregning.integrasjon.BeregningsmotorKontekstKomparator
import no.nav.sokos.dare.poc.beregning.integrasjon.IntegrasjonsScenario
import no.nav.sokos.dare.poc.beregning.integrasjon.LegacyReferanseOracle
import no.nav.sokos.dare.poc.oppdrag.OppdragId

class HistoriskeGoldenMetadataTest :
    FunSpec({
        val scenarioer = IntegrasjonsScenario.alle().associateBy { it.navn }
        val historiske =
            linkedMapOf(
                "AvventTest/avvent_med_tabelltrekk" to HistoriskMetadata(),
                "SkattProsentkortTest/prosentkort_beregner_skatt_med_prosentsats" to HistoriskMetadata(),
                "SkattProsentkortTest/prosentkort_med_skattereduserende_trekk" to
                    HistoriskMetadata(
                        kapasitet =
                            mapOf(
                                "0/0/1" to Kapasitetsendring(kapasitet("0", "200"), kapasitet("974")),
                                "0/1/1" to
                                    Kapasitetsendring(
                                        kapasitet("0,0,0,0,0,0,0,0,0,0,0", "200"),
                                        kapasitet("974,974,974,974,0,0,974,974,974,974,974"),
                                    ),
                            ),
                    ),
                "SkattTabellkortTest/tabellkort_biarbeidsgiver_bruker_prosentsats" to HistoriskMetadata(),
                "SkattTabellkortTest/tabellkort_fordelt_grunnlag_med_deltrekk" to
                    HistoriskMetadata(
                        kapasitet = mapOf("0/1/1" to Kapasitetsendring(kapasitet("200,200,200,200,200"), kapasitet("500,500,500,500,500"))),
                    ),
                "SkattTabellkortTest/tabellkort_hovedarbeidsgiver_beregner_tabelltrekk" to HistoriskMetadata(),
                "SkattTabellkortTest/tabellkort_med_skattereduserende_trekk" to
                    HistoriskMetadata(
                        kapasitet = mapOf("0/0/1" to Kapasitetsendring(kapasitet("100,100,100,100,100"), kapasitet("500,500,500,500,500"))),
                    ),
                "SkattTabellkortTest/tabellkort_uten_prosentsats_kun_tabelltrekk" to HistoriskMetadata(),
                "SkattTabellkortTest/tabellkort_verdiberegnet_med_to_vedtakssatser" to
                    HistoriskMetadata(
                        etikett = "Ytelse AAPUAA",
                        kapasitet =
                            mapOf(
                                "0/0/1" to Kapasitetsendring(kapasitet("666,666,666,666,666"), kapasitet("900,900,900,900,900")),
                                "0/1/1" to Kapasitetsendring(kapasitet("814,814,814,814,815"), kapasitet("1100,1100,1100,1100,1100")),
                            ),
                    ),
            )

        historiske.forEach { (navn, historikk) ->
            test("current legacy documents historical golden labels capacity and AVAV metadata: $navn") {
                val scenario = scenarioer.getValue(navn)
                val data = scenario.les()
                val original = data.forventet
                val legacy = LegacyReferanseOracle.beregn(scenario, KlassekodeTestdata.klassekodeLookup.values.toList())

                // Every original monetary/status/bucket check still applies to the actual current legacy result.
                BeregningsmotorKontekstKomparator.sammenlign(legacy, original)
                ResultatSammenligning.sammenlign(beregnTidslinje(data), legacy)
                var ytelsesrader = 0
                val dokumenterteKapasiteter = mutableSetOf<String>()
                legacy.beregninger.zip(original.beregninger).forEachIndexed { b, (aktuell, gammel) ->
                    aktuell.gjelder shouldBe AktoerId(1)
                    if (navn == "AvventTest/avvent_med_tabelltrekk" && b == 1) {
                        gammel.ekstraInfo shouldBe emptySet()
                        val avvent = aktuell.ekstraInfo.single() as TekstEkstraInfo
                        avvent.grunn shouldBe EkstraInfo.Reasons.AVVENT
                        avvent.verdier.mapValues { it.value.verdi } shouldBe
                            mapOf("kodeArsak" to "AVAV", "datoOverfores" to "ikke satt — krever manuell reaktivering")
                        avvent.verdier.values.all { !it.skjult } shouldBe true
                    } else {
                        aktuell.ekstraInfo shouldBe gammel.ekstraInfo
                    }
                    aktuell.delberegninger.zip(gammel.delberegninger).forEachIndexed { d, (aktuellDel, gammelDel) ->
                        aktuellDel.oppdragId shouldBe OppdragId(0)
                        aktuellDel.rows.zip(gammelDel.rows).forEachIndexed { r, (rad, gammelRad) ->
                            val posisjon = "$b/$d/$r"
                            withClue("$navn beregning/delberegning/rad $posisjon") {
                                if (rad.type == DelberegningType.YTELSE) {
                                    ytelsesrader++
                                    gammelRad.rowName shouldBe historikk.etikett
                                    rad.rowName shouldBe "Ytelse YtelseKlasseKodeRef(kode=AAPUAA)"
                                } else {
                                    rad.rowName shouldBe gammelRad.rowName
                                }
                                val endring = historikk.kapasitet[posisjon]
                                if (endring != null) {
                                    dokumenterteKapasiteter += posisjon
                                    sammenlignKapasitet(gammelRad.capacity, endring.original)
                                    sammenlignKapasitet(rad.capacity, endring.legacy)
                                } else {
                                    sammenlignKapasitet(rad.capacity, gammelRad.capacity)
                                }
                            }
                        }
                    }
                }
                (ytelsesrader > 0) shouldBe true
                dokumenterteKapasiteter shouldBe historikk.kapasitet.keys
            }
        }
    })

private data class HistoriskMetadata(
    val etikett: String = "Ytelse",
    val kapasitet: Map<String, Kapasitetsendring> = emptyMap(),
)

private data class Kapasitetsendring(
    val original: Capacity,
    val legacy: Capacity,
)

private fun kapasitet(
    dager: String,
    sum: String = "0",
): Capacity = Capacity(dager.split(',').map(::BigDecimal), BigDecimal(sum))

private fun sammenlignKapasitet(
    faktisk: Capacity?,
    forventet: Capacity?,
) {
    if (forventet == null) {
        faktisk shouldBe null
    } else {
        requireNotNull(faktisk)
        faktisk.dayCapacity?.size shouldBe forventet.dayCapacity?.size
        faktisk.dayCapacity
            .orEmpty()
            .zip(forventet.dayCapacity.orEmpty())
            .forEach { (fakt, forv) -> fakt.compareTo(forv) shouldBe 0 }
        if (forventet.lumpCapacity == null) {
            faktisk.lumpCapacity shouldBe null
        } else {
            faktisk.lumpCapacity?.compareTo(forventet.lumpCapacity) shouldBe 0
        }
    }
}
