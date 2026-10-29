package no.nav.utbetaling.beregningsflyt.verifikasjon

data class Scenario(
    val navn: String,
    val kontekst: Kontekst,
    val oppdrag: List<Oppdrag>,
    val skatteKontekst: SkatteKontekst,
    val trekk: List<Trekkvedtak>,
    val resultat: Resultat,
    val raw: RåttInnhold,
) {
    companion object {
        val alle: List<Scenario> by lazy { Scenariolaster.lastAlle() }
    }
}

data class RåttInnhold(
    val kontekst: String,
    val oppdrag: String,
    val skatteKontekst: String,
    val trekk: String,
    val resultat: String,
)
