package no.nav.sokos.dare.poc.beregning.beregningsmotor

import no.nav.sokos.dare.poc.beregning.fordeler.BeregningsmotorKontekst

class Beregningsmotor(
    private val ytelser: Ytelsesmotor,
    private val trekk: Trekkmotor,
    private val skatt: Skattemotor,
) {
    fun calculate(kontekst: BeregningsmotorKontekst): BeregningsmotorKontekst {
        val beregnedeYtelser: BeregningsmotorKontekst = ytelser.beregnYtelse(kontekst)
        val medSkattereduserendeTrekk: BeregningsmotorKontekst = trekk.calculateSkattereduserendeTrekk(beregnedeYtelser)
        val medSkattetrekk: BeregningsmotorKontekst = skatt.beregnSkatteTrekk(medSkattereduserendeTrekk)
        val medTrekk: BeregningsmotorKontekst = trekk.calculateOevrigeTrekk(medSkattetrekk)
        return medTrekk
    }
}
