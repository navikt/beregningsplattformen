package no.nav.utbetaling.beregning.lab.beregning

import java.math.BigDecimal
import java.time.LocalDate

import no.nav.sokos.dare.poc.beregning.fordeler.SkatteKontekst
import no.nav.utbetaling.beregning.log.Log
import no.nav.utbetaling.beregning.oppdrag.Oppdragstidslinje
import no.nav.utbetaling.beregning.tid.Periode
import no.nav.utbetaling.beregningsflyt.Beregningsflyt
import no.nav.utbetaling.beregningsflyt.Beregningsgrunnlag

internal object BeregningstidslinjeDTO {
    fun fra(
        oppdragstidslinje: Oppdragstidslinje,
        fom: LocalDate?,
        tom: LocalDate?,
    ) {
        val beregningFom = fom ?: oppdragstidslinje.firstOrNull()?.first?.fom ?: error("Oppdragstidslinje er tom, da må beregningsperiode settes")
        val beregningTom =
            tom ?: oppdragstidslinje
                .lastOrNull()
                ?.first
                ?.let { it as? Periode.LukketPeriode }
                ?.tom ?: error("Oppdragstidslinje er tom/ åpen ende, da må beregningsperiode settes")

        val grunnlag =
            Beregningsgrunnlag(
                beregningsperiode = Periode.LukketPeriode(beregningFom, beregningTom),
                oppdragstidslinje = oppdragstidslinje,
                skatteKontekst = SkatteKontekst(null, emptyMap()),
                trekkVedtak = emptyList(),
                overføres = LocalDate.now(),
                relaterteBeregninger = emptyList(),
                tilgjengeligFrikortbeløp = BigDecimal.ZERO,
            )

        val (_, beregninger) = Beregningsflyt().beregn(grunnlag)

        Log.info("Beregninger: $beregninger")
    }
}
