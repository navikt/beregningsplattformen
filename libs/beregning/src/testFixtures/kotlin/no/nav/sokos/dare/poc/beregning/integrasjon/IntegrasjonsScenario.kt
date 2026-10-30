package no.nav.sokos.dare.poc.beregning.integrasjon

import java.io.File

import no.nav.sokos.dare.poc.beregning.Beregning
import no.nav.sokos.dare.poc.beregning.dto.BeregningsmotorKontekstPresentasjon
import no.nav.sokos.dare.poc.beregning.fordeler.SkatteKontekst
import no.nav.sokos.dare.poc.trekk.Trekkvedtak

data class IntegrasjonsScenario(
    val navn: String,
    val mappe: File,
) {
    fun les(): IntegrasjonsInndata {
        val oppdrag = paakrevdFil("oppdrag.json")
        val kontekst = paakrevdFil("kontekst.json")
        val trekk = paakrevdFil("trekk.json")
        val skatteKontekst = paakrevdFil("skatteKontekst.json")
        val forventet = paakrevdFil("resultat.json")
        val relaterteBeregninger = File(mappe, "relaterteBeregninger.json")
        return IntegrasjonsInndata(
            scenario = this,
            oppdragJson = oppdrag.readText(),
            kontekst = TestKontekstFileRepository.les(kontekst),
            trekk = TrekkFileRepository.les(trekk),
            skatteKontekst = SkatteKontekstFileRepository.les(skatteKontekst),
            relaterteBeregninger =
                if (relaterteBeregninger.exists()) BeregningFileRepository.les(relaterteBeregninger) else emptyList(),
            forventet = BeregningsmotorKontekstFileRepository.les(forventet),
        )
    }

    private fun paakrevdFil(navn: String): File =
        File(mappe, navn).also {
            require(it.isFile) { "Scenario ${this.navn} mangler påkrevd fil: ${it.absolutePath}" }
        }

    companion object {
        fun alle(klasseLaster: ClassLoader = IntegrasjonsScenario::class.java.classLoader): List<IntegrasjonsScenario> {
            val ressurs = "testsupport/integrasjon"
            val url = requireNotNull(klasseLaster.getResource(ressurs)) { "Ressursmappe $ressurs finnes ikke" }
            require(url.protocol == "file") { "Ressursmappe $ressurs må være en filsystemmappe, ikke $url" }
            val rot = File(url.toURI())
            require(rot.isDirectory) { "Ressursmappe $ressurs er ikke en mappe: $rot" }
            val scenarioer =
                rot
                    .listFiles { fil -> fil.isDirectory }
                    .orEmpty()
                    .sorted()
                    .flatMap { kategori ->
                        kategori
                            .listFiles { fil -> fil.isDirectory && File(fil, "kontekst.json").exists() }
                            .orEmpty()
                            .sorted()
                            .map { mappe -> IntegrasjonsScenario("${kategori.name}/${mappe.name}", mappe) }
                    }
            require(scenarioer.isNotEmpty()) { "Ressursmappe $ressurs inneholder ingen scenarioer" }
            return scenarioer
        }
    }
}

data class IntegrasjonsInndata(
    val scenario: IntegrasjonsScenario,
    val oppdragJson: String,
    val kontekst: TestKontekst,
    val trekk: List<Trekkvedtak>,
    val skatteKontekst: SkatteKontekst,
    val relaterteBeregninger: List<Beregning>,
    val forventet: BeregningsmotorKontekstPresentasjon,
)
