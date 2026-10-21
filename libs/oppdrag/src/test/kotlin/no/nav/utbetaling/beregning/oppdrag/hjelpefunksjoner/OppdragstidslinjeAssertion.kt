package no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner

import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe

import no.nav.utbetaling.beregning.oppdrag.Oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.Opplysninger
import no.nav.utbetaling.beregning.oppdrag.intern.FraTidslinjesamling
import no.nav.utbetaling.beregning.oppdrag.intern.FraXmlFormat
import no.nav.utbetaling.beregning.oppdrag.xml.Oppdrag110
import no.nav.utbetaling.beregning.tid.DefaultTidslinjesamling
import no.nav.utbetaling.beregning.tid.Periode

fun <T> Oppdragstidslinje.perPeriode(velg: (Opplysninger) -> T) =
    associate { (periode, oppdragsdager) ->
        periode to
            oppdragsdager.values.map(velg).toSet()
    }

fun Oppdragstidslinje.satser() = perPeriode { it.sats.opplysning.toInt() }

fun Oppdragstidslinje.assertGjenskaptfraTidslinjer() {
    val fraTidslinjesamling = FraTidslinjesamling.opprett(DefaultTidslinjesamling(tidslinjer))
    fraTidslinjesamling shouldBeEqual this
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

    oppdragstidslinjeFraEnInnsending shouldBeEqual oppdragstidslinjeFraOppdeltInnsending.medKjedeIderFra(oppdragstidslinjeFraEnInnsending)
}

/**
 * Ettersom kjedeIder genereres runtime vil det genereres unike ider når vi tester med oppdelt innsending.
 * Bruker denne for å bruke idene fra den samlede innsendingen slik at vi kan sammenligne at de funksjonelle
 * tolkningene er like uavhengig av hvor mange innsendinger som er brukt.
 */
private fun Oppdragstidslinje.medKjedeIderFra(oppdragstidslinje: Oppdragstidslinje): Oppdragstidslinje {
    val kjedeIder =
        oppdragstidslinje.tidslinjer.entries
            .mapIndexed { index, (kjedeId) -> index to kjedeId }
            .toMap()

    val tidslinjesamling =
        DefaultTidslinjesamling(
            this.tidslinjer.entries
                .mapIndexed { index, (_, tidslinje) ->
                    kjedeIder.getValue(index) to tidslinje
                }.toMap(),
        )
    return FraTidslinjesamling.opprett(tidslinjesamling)
}
