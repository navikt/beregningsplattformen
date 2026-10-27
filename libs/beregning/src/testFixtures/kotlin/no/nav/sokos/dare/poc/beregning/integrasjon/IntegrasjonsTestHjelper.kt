package no.nav.sokos.dare.poc.beregning.integrasjon

import java.io.File

import no.nav.sokos.dare.poc.aktoer.AktoerId
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Beregningsmotor
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Skattemotor
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Trekkmotor
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Ytelsesmotor
import no.nav.sokos.dare.poc.beregning.dataklasser.BeregningsStatus
import no.nav.sokos.dare.poc.beregning.dto.BeregningsmotorKontekstPresentasjon
import no.nav.sokos.dare.poc.beregning.fordeler.BeregningsFordeler
import no.nav.sokos.dare.poc.beregning.fordeler.FordelingInput
import no.nav.sokos.dare.poc.oppdrag.Oppdrag
import no.nav.sokos.dare.poc.oppdrag.OppdragConverter.toOppdrag
import no.nav.sokos.dare.poc.oppdrag.OppdragConverter.toOppdragslinjer
import no.nav.sokos.dare.poc.oppdrag.OppdragId
import no.nav.sokos.dare.poc.oppdrag.YtelseKlasseKode
import no.nav.sokos.dare.poc.oppdrag25.Oppdrag110
import no.nav.sokos.dare.poc.oppdrag25.tilAvventPerioder

object IntegrasjonsTestHjelper {
    private val fordeler = BeregningsFordeler(Beregningsmotor(Ytelsesmotor(), Trekkmotor(), Skattemotor()))

    fun resourceDir(
        kategori: String,
        testDir: String,
    ): File {
        val url =
            IntegrasjonsTestHjelper::class.java.classLoader.getResource("testsupport/integrasjon/$kategori/$testDir")
                ?: throw IllegalStateException("Ressursmappe testsupport/integrasjon/$kategori/$testDir finnes ikke")
        return File(url.toURI())
    }

    fun outputDir(
        kategori: String,
        testDir: String,
    ): File = File("build/test-results/integrasjon/$kategori/$testDir")

    fun kjoerBeregning(
        kategori: String,
        testDir: String,
        klassekoder: List<YtelseKlasseKode> = emptyList(),
    ) {
        val inputDir = resourceDir(kategori, testDir)
        val outDir = outputDir(kategori, testDir)

        val oppdrag110List: List<Oppdrag110> = OppdragFileRepository.les(inputDir, "oppdrag.json")
        val klassekodeLookup = klassekoder.associateBy { it.kode }
        val oppdrag: List<Oppdrag> =
            oppdrag110List.map { oppdrag110 ->
                oppdrag110
                    .toOppdrag(AktoerId(1), OppdragId(0))
                    .copy(
                        avventPerioder = oppdrag110.tilAvventPerioder(),
                    ).medOppdragslinjer(oppdrag110.toOppdragslinjer(klassekodeLookup))
            }

        val trekk = TrekkFileRepository.les(inputDir, "trekk.json")
        val skatteKontekst = SkatteKontekstFileRepository.les(inputDir, "skatteKontekst.json")
        val testKontekst = TestKontekstFileRepository.les(inputDir, "kontekst.json")
        val relaterteBeregningerFil = File(inputDir, "relaterteBeregninger.json")
        val relaterteBeregninger =
            if (relaterteBeregningerFil.exists()) {
                BeregningFileRepository.les(relaterteBeregningerFil)
            } else {
                emptyList()
            }
        val kontekst =
            fordeler.fordel(
                input =
                    FordelingInput(
                        konto = "konto1",
                        gjelder = testKontekst.gjelder,
                        beregnet = testKontekst.beregnet,
                        utbetalesTil = testKontekst.utbetalesTil,
                        status = BeregningsStatus.BEREGNET,
                        ekstraInfo = mutableListOf(),
                        overfoeringsdato = testKontekst.overfoeringsdato,
                    ),
                periode = testKontekst.periode,
                trekk = trekk,
                relaterteBeregninger = relaterteBeregninger,
                skatteKontekst = skatteKontekst,
                oppdrag = oppdrag,
                ledigFrikortBeloep = testKontekst.ledigFrikortBeloep,
            )

        val presentasjon = BeregningsmotorKontekstPresentasjon(kontekst)

        OppsettDumper.dump(testKontekst, oppdrag, oppdrag110List, trekk, skatteKontekst, relaterteBeregninger, klassekoder, outDir, "oppsett.txt")
        BeregningsmotorKontekstDumper.dump(kontekst, outDir, "beregningsresultat.txt")
        BeregningsmotorKontekstFileRepository.lagre(presentasjon, outDir, "kontekst-resultat.json")

        val sourceDir = File("src/main/resources/testsupport/integrasjon/$kategori/$testDir")
        val resultatFil = File(sourceDir, "resultat.json")
        if (!resultatFil.exists()) {
            BeregningsmotorKontekstFileRepository.lagre(presentasjon, sourceDir, "resultat.json")
        }
        BeregningsmotorKontekstKomparator.sammenlignMedFil(presentasjon, resultatFil)
    }
}
