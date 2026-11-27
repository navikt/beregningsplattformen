package no.nav.utbetaling.beregning.beregningsmottak

import no.nav.utbetaling.beregning.beregningsmottak.intern.BeregningConsumer

class Beregningsmottak(
    config: Map<String, String> = System.getenv(),
    beregningslyttere: List<Beregningslytter>,
) {
    private val beregningConsumer = BeregningConsumer(config, beregningslyttere)

    // TODO: Hva skal vi spørre på?
    fun hentBeregninger(): List<Stormaskinberegning> = emptyList()

    fun start() {
        beregningConsumer.start()
    }

    fun stop() {
        beregningConsumer.close()
    }
}
