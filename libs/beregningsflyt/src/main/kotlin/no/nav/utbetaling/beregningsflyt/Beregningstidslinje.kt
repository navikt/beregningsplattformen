package no.nav.utbetaling.beregningsflyt

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month.DECEMBER
import java.time.Month.JANUARY
import java.time.Year
import no.nav.utbetaling.beregning.oppdrag.Opplysninger
import no.nav.utbetaling.beregning.tid.Periode
import no.nav.utbetaling.beregning.tid.Tidslinje
import no.nav.utbetaling.beregning.tid.til

data class Beregningsopplysninger(
    val oppdragopplysninger: Opplysninger,
    private val skatteperiode: Any, // TODO: Hadde det vært mulig å legge inn skattereglene som er grunnen til at vi splitter opp?
    private val år: Year? = null
)

fun beregningstidslinje(beregningsperiode: Periode.LukketPeriode, tidslinje: Tidslinje<Opplysninger>, beregningsDato: LocalDateTime): Tidslinje<Beregningsopplysninger> {
     return tidslinje.begrensetTil(beregningsperiode)
         .kombinerMed(skattekortOppdeling(beregningsDato)) { _, oppdragsopplysninger, skatteperiode ->
             oppdragsopplysninger?.let { Beregningsopplysninger(it, skatteperiode ?: Any()) }
        }.kombinerMed(årsoppdeling(beregningsperiode)) { _, beregningsopplysninger, år ->
            beregningsopplysninger?.copy(år = checkNotNull(år))
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

// Oppdeling av beregningsperioden i år er viktig for å kunne beregne skatt korrekt for å vite hva slags skatt som skal legges til grunn.
private fun årsoppdeling(beregningsperiode: Periode.LukketPeriode): Tidslinje<Year> {
    val år = (beregningsperiode.fom.year .. beregningsperiode.tom.year).map { Year.of(it) }
    return år.fold(Tidslinje.Builder<Year>()) { builder, year ->
        builder.periode(Periode.LukketPeriode(LocalDate.of(year.value, JANUARY, 1), LocalDate.of(year.value, DECEMBER, 31)), year)
    }.build()
}