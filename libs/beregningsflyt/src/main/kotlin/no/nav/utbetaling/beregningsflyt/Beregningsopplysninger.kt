package no.nav.utbetaling.beregningsflyt

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.Month.DECEMBER
import java.time.Month.JANUARY
import java.time.Year
import kotlin.collections.map
import no.nav.sokos.dare.poc.beregning.Beregning
import no.nav.sokos.dare.poc.beregning.Delberegning
import no.nav.sokos.dare.poc.beregning.DelberegningType
import no.nav.sokos.dare.poc.beregning.Delberegningslinje
import no.nav.sokos.dare.poc.trekk.SatsType
import no.nav.sokos.dare.poc.trekk.Trekkperiode
import no.nav.sokos.dare.poc.trekk.Trekkvedtak

import no.nav.utbetaling.beregning.oppdrag.Opplysninger
import no.nav.utbetaling.beregning.tid.Periode
import no.nav.utbetaling.beregning.tid.Tidslinje
import no.nav.utbetaling.beregning.tid.til

data class Beregningsopplysninger(
    val oppdragopplysninger: Opplysninger,
    private val skatteperiode: Any, // TODO: Hadde det vært mulig å legge inn skattereglene som er grunnen til at vi splitter opp?
    private val år: Year? = null,
    private val relaterteBeregninger: List<Delberegning> = emptyList(),
    private val trekk: List<Trekkperiode>? = null,
    private val måned: Month? = null
)

fun beregningstidslinje(
    beregningsperiode: Periode.LukketPeriode,
    tidslinje: Tidslinje<Opplysninger>,
    beregningstidspunkt: LocalDateTime,
    trekk: List<Trekkvedtak>,
    relaterteBeregninger: List<Beregning>,
): Tidslinje<Beregningsopplysninger> =
    // TODO finne splittpunkter basert på skattekontekst se beregningsfordeler.
    tidslinje
        .begrensetTil(beregningsperiode)
        .leftJoin(skattekortOppdeling(beregningstidspunkt)) { _, oppdragsopplysninger, skatteperiode ->
            Beregningsopplysninger(oppdragsopplysninger, skatteperiode ?: Any())
        }.leftJoin(årsoppdeling(beregningsperiode)) { _, beregningsopplysninger, år ->
            beregningsopplysninger.copy(år = checkNotNull(år))
        }

    private fun trekkOppdeling(trekk: List<Trekkvedtak>) {
        trekk.map { trekk ->
            val builder = Tidslinje.Builder<Trekkperiode>()
            trekk.trekkperioder.map { trekkperiode ->
                val builder1 = if (trekkperiode.satstype == SatsType.M) {

                    trekkperiode.periode.months()

                    builder.periode(trekkperiode.periode.fom til trekkperiode.periode.tom!!, trekkperiode)
                } else {
                    builder.periode(trekkperiode.periode.fom til trekkperiode.periode.tom!!, trekkperiode)
                }
                builder1
            }
        }
    }

/*
private fun relaterteBeregningerOppdeling(relaterteBeregninger: List<Beregning>): List<Tidslinje<Delberegning>> {
    // TODO: Her må vi filtrere på oppdragId
    relaterteBeregninger.flatMap { beregning: Beregning -> beregning.delberegning }.filter { delberegning -> delberegning.linjer.any { it: Delberegningslinje -> it.type == DelberegningType.YTELSE } }
        .map { delberegning: Delberegning ->
            Tidslinje.Builder<Any>()
                .periode(delberegning.datoIntervall.fom til delberegning.datoIntervall.tom!!, delberegning).build()

        }
}*/


private fun skattekortOppdeling(beregningsDato: LocalDateTime) =
    Tidslinje
        .Builder<Any>()
        // Om perioden inneholder desember i fjor er det viktig at den perioden blir splittet opp
        .periode(LocalDate.of(beregningsDato.year - 1, DECEMBER, 1) til LocalDate.of(beregningsDato.year - 1, DECEMBER, 31), Any())
        // Om januar inneholder januar i år er det viktig at den perioden blir splittet opp
        .periode(LocalDate.of(beregningsDato.year, JANUARY, 1) til LocalDate.of(beregningsDato.year, JANUARY, 31), Any())
        .build()

// Oppdeling av beregningsperioden i år er viktig for å kunne beregne skatt korrekt for å vite hva slags skatt som skal legges til grunn.
private fun årsoppdeling(beregningsperiode: Periode.LukketPeriode): Tidslinje<Year> {
    val år = (beregningsperiode.fom.year..beregningsperiode.tom.year).map { Year.of(it) }
    return år
        .fold(Tidslinje.Builder<Year>()) { builder, year ->
            builder.periode(Periode.LukketPeriode(LocalDate.of(year.value, JANUARY, 1), LocalDate.of(year.value, DECEMBER, 31)), year)
        }.build()
}
