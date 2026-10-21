package no.nav.utbetaling.beregning.oppdrag.intern

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

import no.nav.utbetaling.beregning.oppdrag.Oppdrag
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.Filhjelper

class MetrikkerTest :
    FunSpec({

        test("Metrikker fra XML & JSON") {
            val xml = Filhjelper.lesFil("xml/NyttOppdragMedSkyldner.xml")
            val json = Filhjelper.lesFil("json/oppdrag_med_komplisert_kjeding.json")

            val jsonMetrikker = Metrikker.fra(FraXmlFormat.oppdrag110FraJson(json).keys)
            val xmlMetrikker = Metrikker.fra(FraXmlFormat.oppdrag110FraXml(xml).keys)

            val metrikker = jsonMetrikker + xmlMetrikker

            val forventet =
                listOf(
                    Oppdrag.Metrikk(felt = "oppdrag_110_kodeAksjon", tags = mapOf("kodeFagomraade" to "AAP", "verdi" to "1")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_kodeEndring", tags = mapOf("kodeFagomraade" to "AAP", "verdi" to "ENDR")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_fagsystemId", tags = mapOf("kodeFagomraade" to "AAP", "verdi" to "skjult")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_utbetFrekvens", tags = mapOf("kodeFagomraade" to "AAP", "verdi" to "MND")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_oppdragGjelderId", tags = mapOf("kodeFagomraade" to "AAP", "verdi" to "skjult")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_datoOppdragGjelderFom", tags = mapOf("kodeFagomraade" to "AAP", "verdi" to "skjult")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_saksbehId", tags = mapOf("kodeFagomraade" to "AAP", "verdi" to "skjult")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_avstemming_115_kodeKomponent", tags = mapOf("kodeFagomraade" to "AAP", "verdi" to "AAP")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_avstemming_115_nokkelAvstemming", tags = mapOf("kodeFagomraade" to "AAP", "verdi" to "skjult")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_avstemming_115_tidspktMelding", tags = mapOf("kodeFagomraade" to "AAP", "verdi" to "skjult")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_oppdrags_enhet_120_typeEnhet", tags = mapOf("kodeFagomraade" to "AAP", "verdi" to "BOS")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_oppdrags_enhet_120_enhet", tags = mapOf("kodeFagomraade" to "AAP", "verdi" to "skjult")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_oppdrags_linje_150_kodeEndringLinje", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "NY")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_oppdrags_linje_150_vedtakId", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "skjult")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_oppdrags_linje_150_delytelseId", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "skjult")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_oppdrags_linje_150_datoVedtakFom", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "skjult")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_oppdrags_linje_150_datoVedtakTom", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "skjult")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_oppdrags_linje_150_sats", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "skjult")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_oppdrags_linje_150_fradragTillegg", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "T")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_oppdrags_linje_150_typeSats", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "DAG")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_oppdrags_linje_150_brukKjoreplan", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "N")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_oppdrags_linje_150_saksbehId", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "skjult")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_oppdrags_linje_150_utbetalesTilId", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "skjult")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_oppdrags_linje_150_henvisning", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "skjult")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_oppdrags_linje_150_refFagsystemId", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "skjult")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_oppdrags_linje_150_vedtakssats_157_vedtakssats", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "skjult")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_oppdrags_linje_150_attestant_180_attestantId", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "skjult")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_oppdrags_linje_150_refDelytelseId", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "skjult")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_kodeAksjon", tags = mapOf("kodeFagomraade" to "AAP", "verdi" to "1")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_kodeEndring", tags = mapOf("kodeFagomraade" to "AAP", "verdi" to "NY")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_fagsystemId", tags = mapOf("kodeFagomraade" to "AAP", "verdi" to "skjult")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_utbetFrekvens", tags = mapOf("kodeFagomraade" to "AAP", "verdi" to "MND")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_oppdragGjelderId", tags = mapOf("kodeFagomraade" to "AAP", "verdi" to "skjult")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_datoOppdragGjelderFom", tags = mapOf("kodeFagomraade" to "AAP", "verdi" to "skjult")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_saksbehId", tags = mapOf("kodeFagomraade" to "AAP", "verdi" to "skjult")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_avstemming_115_kodeKomponent", tags = mapOf("kodeFagomraade" to "AAP", "verdi" to "AAP")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_avstemming_115_nokkelAvstemming", tags = mapOf("kodeFagomraade" to "AAP", "verdi" to "skjult")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_avstemming_115_tidspktMelding", tags = mapOf("kodeFagomraade" to "AAP", "verdi" to "skjult")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_oppdrags_enhet_120_typeEnhet", tags = mapOf("kodeFagomraade" to "AAP", "verdi" to "BOS")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_oppdrags_enhet_120_enhet", tags = mapOf("kodeFagomraade" to "AAP", "verdi" to "skjult")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_oppdrags_linje_150_kodeEndringLinje", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "NY")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_oppdrags_linje_150_vedtakId", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "skjult")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_oppdrags_linje_150_delytelseId", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "skjult")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_oppdrags_linje_150_datoVedtakFom", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "skjult")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_oppdrags_linje_150_datoVedtakTom", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "skjult")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_oppdrags_linje_150_sats", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "skjult")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_oppdrags_linje_150_fradragTillegg", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "T")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_oppdrags_linje_150_typeSats", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "DAG")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_oppdrags_linje_150_skyldnerId", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "skjult")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_oppdrags_linje_150_brukKjoreplan", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "N")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_oppdrags_linje_150_saksbehId", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "skjult")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_oppdrags_linje_150_utbetalesTilId", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "skjult")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_oppdrags_linje_150_henvisning", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "skjult")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_oppdrags_linje_150_refFagsystemId", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "skjult")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_oppdrags_linje_150_refDelytelseId", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "skjult")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_oppdrags_linje_150_vedtakssats_157_vedtakssats", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "skjult")),
                    Oppdrag.Metrikk(felt = "oppdrag_110_oppdrags_linje_150_attestant_180_attestantId", tags = mapOf("kodeFagomraade" to "AAP", "kodeKlassifik" to "AAPUAA", "verdi" to "skjult")),
                )

            metrikker shouldBe forventet
        }
    })
