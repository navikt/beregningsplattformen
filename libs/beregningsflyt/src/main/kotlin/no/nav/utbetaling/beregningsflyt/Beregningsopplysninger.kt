package no.nav.utbetaling.beregningsflyt

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.Month.DECEMBER
import java.time.Month.JANUARY
import java.time.Year
import java.time.YearMonth

import kotlin.collections.contains
import kotlin.collections.map

import no.nav.sokos.dare.poc.beregning.Beregning
import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.beregning.DelberegningType.YTELSE
import no.nav.sokos.dare.poc.oppdrag.OppdragId
import no.nav.sokos.dare.poc.trekk.SatsType
import no.nav.sokos.dare.poc.trekk.Trekkperiode
import no.nav.sokos.dare.poc.trekk.Trekkvedtak
import no.nav.utbetaling.beregning.oppdrag.Opplysninger
import no.nav.utbetaling.beregning.tid.Periode
import no.nav.utbetaling.beregning.tid.Tidslinje
import no.nav.utbetaling.beregning.tid.til

internal data class Trekkperiodedel(
    val måned: Month?,
    val trekkperiode: Trekkperiode,
)

internal data class Beregningsopplysninger(
    val oppdragopplysninger: Map<Oppdragsidentifikator, List<Opplysninger>>,
    private val skatteperiode: Any, // TODO: Hadde det vært mulig å legge inn skattereglene som er grunnen til at vi splitter opp?
    private val år: Year? = null,
    val tidligereYtelsesberegninger: List<Beregning> = emptyList(),
    private val trekkperiodedel: List<Trekkperiodedel> = emptyList(),
)

internal fun beregningstidslinje(
    beregningsperiode: Periode.LukketPeriode,
    tidslinje: Tidslinje<Map<Oppdragsidentifikator, List<Opplysninger>>>,
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
        }.leftJoin(trekktidslinje(trekk, beregningsperiode)) { _, beregningsopplysnigner, trekkperiodedeler ->
            beregningsopplysnigner.copy(trekkperiodedel = trekkperiodedeler ?: emptyList())
        }.leftJoin(tidligereYtelsesberegninger(relaterteBeregninger)) { _, beregningsopplysninger, tidligereYtelsesberegnigner ->
            beregningsopplysninger.copy(tidligereYtelsesberegninger = tidligereYtelsesberegnigner ?: emptyList())
        }

private fun trekktidslinje(
    trekk: List<Trekkvedtak>,
    beregningsperiode: Periode.LukketPeriode,
): Tidslinje<List<Trekkperiodedel>> {
    val tidslinjePerTrekkvedtak =
        trekk.map { trekkvedtak ->
            val builder = Tidslinje.Builder<Trekkperiodedel>()
            trekkvedtak.trekkperioder.forEach trekkperiode@{ trekkperiode ->
                val periode = trekkperiode.periode.somLukketPeriode(beregningsperiode.tom) ?: return@trekkperiode
                if (trekkperiode.satstype == SatsType.M) {
                    periode.delOppVedNyMåned().forEach { månedDel ->
                        builder.periode(månedDel, Trekkperiodedel(månedDel.start.month, trekkperiode))
                    }
                } else {
                    builder.periode(periode, Trekkperiodedel(null, trekkperiode))
                }
            }
            builder.build()
        }

    return tidslinjePerTrekkvedtak.fold(Tidslinje.Builder<List<Trekkperiodedel>>().build()) { samletTidslinje, tidslinjeForEtTrekkvedtak ->
        samletTidslinje.fullJoin(tidslinjeForEtTrekkvedtak) { _, eksiterenede, ny ->
            eksiterenede.orEmpty() + listOfNotNull(ny)
        }
    }
}

private fun tidligereYtelsesberegninger(relaterteBeregninger: List<Beregning>): Tidslinje<List<Beregning>> {
    val oppdragIderSomBeregnesNå = setOf(OppdragId(0L)) // TODO: Må bli input

    val tidligereYtelsesberegninger =
        relaterteBeregninger
            .filter { it.delberegning.any { delberegning -> delberegning.oppdragId in oppdragIderSomBeregnesNå } }
            .filter { it.delberegning.any { delberegning -> delberegning.linjer.any { linje -> linje.type == YTELSE } } }

    val delberegningsperioder =
        tidligereYtelsesberegninger
            .flatMap { it.delberegning.map { delberegning -> delberegning.datoIntervall } }
            .toSet()

    return delberegningsperioder
        .fold(Tidslinje.Builder<List<Beregning>>()) { builder, periode ->
            val overlappendeBeregninger =
                tidligereYtelsesberegninger.mapNotNull { beregning ->
                    val overlapp = beregning.delberegning.filter { it.datoIntervall.overlaps(periode) }
                    if (overlapp.isEmpty()) {
                        null
                    } else {
                        beregning.copy(delberegning = overlapp)
                    }
                }
            builder.periode(periode.fom til periode.tom!!, overlappendeBeregninger)
        }.build()
}

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

private fun DatoIntervall.somLukketPeriode(maksTilOgMed: LocalDate): Periode.LukketPeriode? {
    if (fom > maksTilOgMed) return null
    return when (val tilOgMed = tom) {
        null -> Periode.LukketPeriode(fom, maksTilOgMed)
        else -> Periode.LukketPeriode(fom, minOf(tilOgMed, maksTilOgMed))
    }
}

private fun Periode.LukketPeriode.delOppVedNyMåned(): List<Periode.LukketPeriode> =
    generateSequence(YearMonth.from(fom)) { it.plusMonths(1) }
        .takeWhile { it <= YearMonth.from(tom) }
        .map { måned -> maxOf(måned.atDay(1), fom) til minOf(måned.atEndOfMonth(), tom) }
        .toList()
