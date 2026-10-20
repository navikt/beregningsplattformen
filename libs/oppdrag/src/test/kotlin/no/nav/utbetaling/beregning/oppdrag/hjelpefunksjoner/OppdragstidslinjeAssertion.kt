package no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner

import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe

import no.nav.utbetaling.beregning.oppdrag.Oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.Opplysninger
import no.nav.utbetaling.beregning.oppdrag.intern.FraTidslinjer
import no.nav.utbetaling.beregning.oppdrag.intern.FraXmlFormat
import no.nav.utbetaling.beregning.oppdrag.xml.Oppdrag110
import no.nav.utbetaling.beregning.tid.Periode

fun <T> Oppdragstidslinje.perPeriode(velg: (Opplysninger) -> T) =
    associate { (periode, oppdragsdager) ->
        periode to
            oppdragsdager.values.map(velg).toSet()
    }

fun Oppdragstidslinje.satser() = perPeriode { it.sats.opplysning.toInt() }

fun Oppdragstidslinje.assertGjenskaptfraTidslinjer() {
    val fraTidslinjer = FraTidslinjer.opprett(tidslinjeIterator().asSequence().toList())
    fraTidslinjer shouldBeEqual this
}

infix fun <T> Map<Periode, T>.skalVæreLik(expected: Map<Periode, T>) {
    this.mapKeys { (periode, _) -> periode.toString() } shouldBe expected.mapKeys { (periode, _) -> periode.toString() }
}

internal fun assertSammeOppførselOmHverEnkeltLinjeKommerIEgenInnsending(
    oppdrag110: Oppdrag110,
    assertionBlock: (resultat: Oppdragstidslinje) -> Unit,
) {
    val oppdragstidslinjeFraEnInnsending = FraXmlFormat.opprett(oppdrag110)
    oppdragstidslinjeFraEnInnsending.assertGjenskaptfraTidslinjer()

    assertionBlock(oppdragstidslinjeFraEnInnsending)

    val oppdelt =
        oppdrag110.oppdragsLinje150.map { linje ->
            oppdrag110.copy(
                oppdragsLinje150 = listOf(linje),
            )
        }

    val oppdragstidslinjeFraOppdeltInnsending = FraXmlFormat.fraOppdrag110(oppdelt)
    oppdragstidslinjeFraOppdeltInnsending.assertGjenskaptfraTidslinjer()
    assertionBlock(oppdragstidslinjeFraOppdeltInnsending)

    oppdragstidslinjeFraEnInnsending shouldBeEqual oppdragstidslinjeFraOppdeltInnsending
}
