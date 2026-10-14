package no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner

import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe

import no.nav.utbetaling.beregning.oppdrag.Omposteringskode
import no.nav.utbetaling.beregning.oppdrag.Oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.Opplysninger
import no.nav.utbetaling.beregning.tid.Periode

fun <T> Oppdragstidslinje.perPeriode(velg: (Opplysninger) -> T) =
    associate { (periode, oppdragsdager) ->
        periode to
            oppdragsdager.values.map(velg).toSet()
    }

fun Oppdragstidslinje.satser() = perPeriode { it.sats.opplysning.toInt() }

fun Oppdragstidslinje.satserPerFagsystemId() = perPeriode { it.fagsystemId.id to it.sats.opplysning.toInt() }

fun Oppdragstidslinje.avventinger() = perPeriode { it.avventingsårsak }

fun Oppdragstidslinje.satserMedAvventing() = perPeriode { it.sats.opplysning.toInt() to it.avventingsårsak?.opplysning }

fun Oppdragstidslinje.satserMedBilagstype() = perPeriode { it.sats.opplysning.toInt() to it.bilagstype?.opplysning }

fun Oppdragstidslinje.satserMedHenvisning() = perPeriode { it.sats.opplysning.toInt() to it.henvisning?.opplysning }

fun Oppdragstidslinje.satserMedForfall() = perPeriode { it.sats.opplysning.toInt() to it.forfall?.opplysning }

fun Oppdragstidslinje.satserMedOppdragId() = perPeriode { it.sats.opplysning.toInt() to it.oppdragId?.opplysning }

fun Oppdragstidslinje.satserMedStønadId() = perPeriode { it.sats.opplysning.toInt() to it.stønadId?.opplysning }

fun Oppdragstidslinje.satserMedUtbetalingsfrekvens() = perPeriode { it.sats.opplysning.toInt() to it.utbetalingsfrekvens?.opplysning }

fun Oppdragstidslinje.satserMedOmpostering() = perPeriode { it.sats.opplysning.toInt() to it.ompostering?.opplysning?.kode }

fun Oppdragstidslinje.satserMedKid() = perPeriode { it.sats.opplysning.toInt() to it.kidnummer?.opplysning?.nummer }

fun Oppdragstidslinje.statuser() = perPeriode { it.status.opplysning }

fun Oppdragstidslinje.delytelseIdMedStatus() = perPeriode { it.delytelseId.id to it.status.opplysning }

fun Oppdragstidslinje.assertGjenskaptfraTidslinjer() {
    val fraTidslinjer = Oppdragstidslinje.fraTidslinjer(tidslinjeIterator().asSequence().toList())
    fraTidslinjer shouldBeEqual this
}

infix fun <T> Map<Periode, T>.skalVæreLik(expected: Map<Periode, T>) {
    this.mapKeys { (periode, _) -> periode.toString() } shouldBe expected.mapKeys { (periode, _) -> periode.toString() }
}

val OMPOSTERINGSKODE = Omposteringskode("J")
