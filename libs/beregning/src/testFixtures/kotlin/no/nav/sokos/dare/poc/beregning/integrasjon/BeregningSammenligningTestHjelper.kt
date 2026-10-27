package no.nav.sokos.dare.poc.beregning.integrasjon

import java.io.File

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

import no.nav.sokos.dare.poc.beregning.Beregning
import no.nav.sokos.dare.poc.beregning.mismatch.BeregningAvvik
import no.nav.sokos.dare.poc.beregning.mismatch.BeregningSammenligner
import no.nav.sokos.dare.poc.beregning.mismatch.BeregningSammenligningsResultat

object BeregningSammenligningTestHjelper {
    /**
     * Sammenligner en liste med beregninger mot en referansefil lagret av BeregningFileRepository.
     * Alle avvik rapporteres samlet via kotest assertSoftly.
     */
    fun sammenlignMedFil(
        faktisk: List<Beregning>,
        filnavn: String,
    ) {
        val forventet = BeregningFileRepository.les(filnavn)
        sammenlign(faktisk, forventet)
    }

    fun sammenlignMedFil(
        faktisk: List<Beregning>,
        fil: File,
    ) {
        val forventet = BeregningFileRepository.les(fil)
        sammenlign(faktisk, forventet)
    }

    fun sammenlign(
        faktisk: List<Beregning>,
        forventet: List<Beregning>,
    ) {
        val sammenligning = BeregningSammenligner.sammenlign(forventet, faktisk)

        assertSoftly {
            sammenligning.avvik.forEach { avvik ->
                withClue(avvikClue(avvik)) {
                    avvik.forventet shouldBe avvik.faktisk
                }
            }
            withClue(oppsummeringClue(sammenligning)) {
                sammenligning.erLik shouldBe true
            }
        }
    }

    private fun avvikClue(avvik: BeregningAvvik): String = "path=${avvik.sti}, type=${avvik.type}, expected=${avvik.forventet}, actual=${avvik.faktisk}"

    private fun oppsummeringClue(sammenligning: BeregningSammenligningsResultat): String =
        "comparison mismatch: differences=${sammenligning.avvik.size}, " +
            "added=${sammenligning.lagtTilBeregninger.size}, " +
            "removed=${sammenligning.fjernedeBeregninger.size}, " +
            "unresolved=${sammenligning.uloesteTvetydigheter.size}"
}
