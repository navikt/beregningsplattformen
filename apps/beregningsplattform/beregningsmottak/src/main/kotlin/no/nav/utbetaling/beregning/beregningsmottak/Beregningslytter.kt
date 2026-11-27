package no.nav.utbetaling.beregning.beregningsmottak

interface Beregningslytter {
    fun nyBeregning(beregning: Stormaskinberegning)
}
