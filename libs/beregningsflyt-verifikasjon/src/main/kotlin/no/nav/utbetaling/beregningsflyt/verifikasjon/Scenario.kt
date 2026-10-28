package no.nav.utbetaling.beregningsflyt.verifikasjon

data class Scenario(
    val navn: String,
    val kontekst: Kontekst,
    val oppdrag: List<Oppdrag>,
    val skatteKontekst: SkatteKontekst,
    val trekk: List<Trekkvedtak>,
    val resultat: Resultat,
) {
    companion object {
        val alle: List<Scenario> by lazy { Scenariolaster.lastAlle() }
    }
}
