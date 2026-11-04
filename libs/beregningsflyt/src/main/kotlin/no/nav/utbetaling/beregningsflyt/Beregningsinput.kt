package no.nav.utbetaling.beregningsflyt

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month.DECEMBER
import java.time.Month.JANUARY
import no.nav.utbetaling.beregning.oppdrag.Opplysninger
import no.nav.utbetaling.beregning.tid.Periode
import no.nav.utbetaling.beregning.tid.Tidslinje
import no.nav.utbetaling.beregning.tid.til

data class Beregningsopplysninger(
    val oppdragopplysninger: Opplysninger,
    val skatteperiode: Any, // TODO: Hadde det vært mulig å legge inn skattereglene som er grunnen til at vi splitter opp?
)

fun beregningstidslinje(beregningsperiode: Periode.LukketPeriode, tidslinje: Tidslinje<Opplysninger>, beregningsDato: LocalDateTime): Tidslinje<Beregningsopplysninger> {
     return tidslinje.begrensetTil(beregningsperiode)
         .kombinerMed(skattekortOppdeling(beregningsDato)) { _, oppdragsopplysninger, skatteperiode ->
             oppdragsopplysninger?.let { Beregningsopplysninger(it, skatteperiode ?: Any()) }
        }
}

private fun skattekortOppdeling(
    beregningsDato: LocalDateTime,
) = Tidslinje.Builder<Any>()
    // Om perioden inneholder desember i fjor er det viktig at den perioden blir splittet opp
    .periode(LocalDate.of(beregningsDato.year - 1, DECEMBER, 1) til LocalDate.of(beregningsDato.year - 1, DECEMBER, 31), Any())
    // Om januar inneholder januar i år er det viktig at den perioden blir splittet opp
    .periode(LocalDate.of(beregningsDato.year, JANUARY, 1) til LocalDate.of(beregningsDato.year, JANUARY, 31), Any())
    .build()

