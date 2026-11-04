package no.nav.utbetaling.beregningsflyt


data class Scenario(
    val navn: String,
    val kontekst: Kontekst,
    val oppdrag: String,
    val skatteKontekst: String,
    val trekk: String,
    val resultat: String
) {
    companion object {
        val alle: List<Scenario> by lazy { Scenariolaster.lastAlle() }
    }
}
