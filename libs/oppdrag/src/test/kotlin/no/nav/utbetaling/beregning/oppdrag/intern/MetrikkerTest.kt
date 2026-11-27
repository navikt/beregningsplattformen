package no.nav.utbetaling.beregning.oppdrag.intern

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

import no.nav.utbetaling.beregning.oppdrag.Oppdrag
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.Filhjelper

class MetrikkerTest :
    FunSpec({

        fun forekomst(
            felt: String,
            tags: Map<String, String>,
        ) = Oppdrag.Metrikk.Forekomst(
            tags = mapOf("kodeKlassifik" to "") + tags + ("felt" to felt),
        )

        test("Metrikker fra XML & JSON") {
            val xml = Filhjelper.lesFil("xml/NyttOppdragMedSkyldner.xml")
            val json = Filhjelper.lesFil("json/oppdrag_med_komplisert_kjeding.json")

            val jsonMetrikker = Oppdrag.fraJson(json).metrikker
            val xmlMetrikker = Oppdrag.fraXml(xml).metrikker

            val metrikker = jsonMetrikker + xmlMetrikker

            val forventetJsonMetrikk =
                Oppdrag.Metrikk(
                    navn = "innsendte_felt",
                    forekomster =
                        listOf(
                            forekomst(felt = "oppdrag_110_kodeAksjon", tags = mapOf("kodeFagomraade" to "AAP", "verdi" to "1")),
                            forekomst(felt = "oppdrag_110_kodeEndring", tags = mapOf("kodeFagomraade" to "AAP", "verdi" to "ENDR")),
                            forekomst(felt = "oppdrag_110_kodeFagomraade", tags = mapOf("kodeFagomraade" to "AAP", "verdi" to "AAP")),
                            forekomst(felt = "oppdrag_110_fagsystemId", tags = mapOf("kodeFagomraade" to "AAP", "verdi" to "skjult")),
                            forekomst(felt = "oppdrag_110_utbetFrekvens", tags = mapOf("kodeFagomraade" to "AAP", "verdi" to "MND")),
                            forekomst(felt = "oppdrag_110_oppdragGjelderId", tags = mapOf("kodeFagomraade" to "AAP", "verdi" to "skjult")),
                            forekomst(felt = "oppdrag_110_datoOppdragGjelderFom", tags = mapOf("kodeFagomraade" to "AAP", "verdi" to "skjult")),
                            forekomst(felt = "oppdrag_110_saksbehId", tags = mapOf("kodeFagomraade" to "AAP", "verdi" to "skjult")),
                            forekomst(felt = "oppdrag_110_avstemming_115_kodeKomponent", tags = mapOf("kodeFagomraade" to "AAP", "verdi" to "AAP")),
                            forekomst(felt = "oppdrag_110_avstemming_115_nokkelAvstemming", tags = mapOf("kodeFagomraade" to "AAP", "verdi" to "skjult")),
                            forekomst(felt = "oppdrag_110_avstemming_115_tidspktMelding", tags = mapOf("kodeFagomraade" to "AAP", "verdi" to "skjult")),
                            forekomst(felt = "oppdrag_110_oppdrags_enhet_120_typeEnhet", tags = mapOf("kodeFagomraade" to "AAP", "verdi" to "BOS")),
                            forekomst(felt = "oppdrag_110_oppdrags_enhet_120_enhet", tags = mapOf("kodeFagomraade" to "AAP", "verdi" to "skjult")),
                            forekomst(felt = "oppdrag_110_oppdrags_linje_150_kodeEndringLinje", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "NY")),
                            forekomst(felt = "oppdrag_110_oppdrags_linje_150_vedtakId", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "skjult")),
                            forekomst(felt = "oppdrag_110_oppdrags_linje_150_delytelseId", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "skjult")),
                            forekomst(felt = "oppdrag_110_oppdrags_linje_150_kodeKlassifik", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "AAPUAA")),
                            forekomst(felt = "oppdrag_110_oppdrags_linje_150_datoVedtakFom", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "skjult")),
                            forekomst(felt = "oppdrag_110_oppdrags_linje_150_datoVedtakTom", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "skjult")),
                            forekomst(felt = "oppdrag_110_oppdrags_linje_150_sats", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "skjult")),
                            forekomst(felt = "oppdrag_110_oppdrags_linje_150_fradragTillegg", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "T")),
                            forekomst(felt = "oppdrag_110_oppdrags_linje_150_typeSats", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "DAG")),
                            forekomst(felt = "oppdrag_110_oppdrags_linje_150_brukKjoreplan", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "N")),
                            forekomst(felt = "oppdrag_110_oppdrags_linje_150_saksbehId", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "skjult")),
                            forekomst(felt = "oppdrag_110_oppdrags_linje_150_utbetalesTilId", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "skjult")),
                            forekomst(felt = "oppdrag_110_oppdrags_linje_150_henvisning", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "skjult")),
                            forekomst(felt = "oppdrag_110_oppdrags_linje_150_refFagsystemId", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "skjult")),
                            forekomst(felt = "oppdrag_110_oppdrags_linje_150_vedtakssats_157_vedtakssats", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "skjult")),
                            forekomst(felt = "oppdrag_110_oppdrags_linje_150_attestant_180_attestantId", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "skjult")),
                            forekomst(felt = "oppdrag_110_oppdrags_linje_150_refDelytelseId", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "skjult")),
                        ),
                )

            val forventetXmlMetrikk =
                Oppdrag.Metrikk(
                    navn = "innsendte_felt",
                    forekomster =
                        listOf(
                            forekomst(felt = "oppdrag_110_kodeAksjon", tags = mapOf("kodeFagomraade" to "AAP", "verdi" to "1")),
                            forekomst(felt = "oppdrag_110_kodeEndring", tags = mapOf("kodeFagomraade" to "AAP", "verdi" to "NY")),
                            forekomst(felt = "oppdrag_110_kodeFagomraade", tags = mapOf("kodeFagomraade" to "AAP", "verdi" to "AAP")),
                            forekomst(felt = "oppdrag_110_fagsystemId", tags = mapOf("kodeFagomraade" to "AAP", "verdi" to "skjult")),
                            forekomst(felt = "oppdrag_110_utbetFrekvens", tags = mapOf("kodeFagomraade" to "AAP", "verdi" to "MND")),
                            forekomst(felt = "oppdrag_110_oppdragGjelderId", tags = mapOf("kodeFagomraade" to "AAP", "verdi" to "skjult")),
                            forekomst(felt = "oppdrag_110_datoOppdragGjelderFom", tags = mapOf("kodeFagomraade" to "AAP", "verdi" to "skjult")),
                            forekomst(felt = "oppdrag_110_saksbehId", tags = mapOf("kodeFagomraade" to "AAP", "verdi" to "skjult")),
                            forekomst(felt = "oppdrag_110_avstemming_115_kodeKomponent", tags = mapOf("kodeFagomraade" to "AAP", "verdi" to "AAP")),
                            forekomst(felt = "oppdrag_110_avstemming_115_nokkelAvstemming", tags = mapOf("kodeFagomraade" to "AAP", "verdi" to "skjult")),
                            forekomst(felt = "oppdrag_110_avstemming_115_tidspktMelding", tags = mapOf("kodeFagomraade" to "AAP", "verdi" to "skjult")),
                            forekomst(felt = "oppdrag_110_oppdrags_enhet_120_typeEnhet", tags = mapOf("kodeFagomraade" to "AAP", "verdi" to "BOS")),
                            forekomst(felt = "oppdrag_110_oppdrags_enhet_120_enhet", tags = mapOf("kodeFagomraade" to "AAP", "verdi" to "skjult")),
                            forekomst(felt = "oppdrag_110_oppdrags_linje_150_kodeEndringLinje", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "NY")),
                            forekomst(felt = "oppdrag_110_oppdrags_linje_150_vedtakId", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "skjult")),
                            forekomst(felt = "oppdrag_110_oppdrags_linje_150_delytelseId", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "skjult")),
                            forekomst(felt = "oppdrag_110_oppdrags_linje_150_kodeKlassifik", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "AAPUAA")),
                            forekomst(felt = "oppdrag_110_oppdrags_linje_150_datoVedtakFom", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "skjult")),
                            forekomst(felt = "oppdrag_110_oppdrags_linje_150_datoVedtakTom", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "skjult")),
                            forekomst(felt = "oppdrag_110_oppdrags_linje_150_sats", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "skjult")),
                            forekomst(felt = "oppdrag_110_oppdrags_linje_150_fradragTillegg", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "T")),
                            forekomst(felt = "oppdrag_110_oppdrags_linje_150_typeSats", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "DAG")),
                            forekomst(felt = "oppdrag_110_oppdrags_linje_150_skyldnerId", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "skjult")),
                            forekomst(felt = "oppdrag_110_oppdrags_linje_150_brukKjoreplan", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "N")),
                            forekomst(felt = "oppdrag_110_oppdrags_linje_150_saksbehId", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "skjult")),
                            forekomst(felt = "oppdrag_110_oppdrags_linje_150_utbetalesTilId", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "skjult")),
                            forekomst(felt = "oppdrag_110_oppdrags_linje_150_henvisning", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "skjult")),
                            forekomst(felt = "oppdrag_110_oppdrags_linje_150_refFagsystemId", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "skjult")),
                            forekomst(felt = "oppdrag_110_oppdrags_linje_150_refDelytelseId", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "skjult")),
                            forekomst(felt = "oppdrag_110_oppdrags_linje_150_vedtakssats_157_vedtakssats", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "skjult")),
                            forekomst(felt = "oppdrag_110_oppdrags_linje_150_attestant_180_attestantId", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "skjult")),
                        ),
                )

            metrikker shouldBe listOf(forventetJsonMetrikk, forventetXmlMetrikk)
        }
    })
