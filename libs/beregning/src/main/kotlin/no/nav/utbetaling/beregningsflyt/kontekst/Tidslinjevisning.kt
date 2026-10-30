package no.nav.utbetaling.beregningsflyt.kontekst

import java.util.UUID

import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.utbetaling.beregning.oppdrag.Opplysninger
import no.nav.utbetaling.beregning.tid.DefaultTidslinjesamling
import no.nav.utbetaling.beregning.tid.Periode.LukketPeriode
import no.nav.utbetaling.beregning.tid.Periode.ÅpenPeriode
import no.nav.utbetaling.beregning.tid.Tidslinjesamling

fun Tidslinjesamling<UUID, Opplysninger>.avgrensTil(intervall: DatoIntervall): Tidslinjesamling<UUID, Opplysninger> {
    val periode = intervall.tom?.let { LukketPeriode(intervall.fom, it) } ?: ÅpenPeriode(intervall.fom)
    return DefaultTidslinjesamling(
        tidslinjer
            .mapValues { (_, tidslinje) -> tidslinje.begrensetTil(periode) }
            .filterValues { !it.isEmpty() },
    )
}
