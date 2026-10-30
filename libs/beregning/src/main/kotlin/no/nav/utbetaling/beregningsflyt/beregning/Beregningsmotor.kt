package no.nav.utbetaling.beregningsflyt.beregning

import no.nav.sokos.dare.poc.beregning.beregningsmotor.Skattemotor
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Trekkmotor
import no.nav.utbetaling.beregningsflyt.kontekst.BeregningsmotorKontekst

class Beregningsmotor(
    private val ytelser: Ytelsesmotor,
    private val trekk: Trekkmotor = Trekkmotor(),
    private val skatt: Skattemotor = Skattemotor(),
) {
    fun calculate(kontekst: BeregningsmotorKontekst): BeregningsmotorKontekst {
        ytelser.beregnYtelse(kontekst)
        trekk.calculateSkattereduserendeTrekkFelles(kontekst)
        skatt.beregnSkatteTrekkFelles(kontekst)
        trekk.calculateOevrigeTrekkFelles(kontekst)
        return kontekst
    }
}
