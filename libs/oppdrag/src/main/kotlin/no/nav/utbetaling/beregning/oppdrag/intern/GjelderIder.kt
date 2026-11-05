package no.nav.utbetaling.beregning.oppdrag.intern

import no.nav.utbetaling.beregning.oppdrag.Personidentifikator
import no.nav.utbetaling.beregning.oppdrag.xml.Oppdrag110
import no.nav.utbetaling.beregning.tid.Periode
import no.nav.utbetaling.beregning.tid.Tidslinje

internal object GjelderIder {
    fun fra(oppdrag110: Oppdrag110): Tidslinje<Personidentifikator> {
        val builder = Tidslinje.Builder<Personidentifikator>()
        val oppdragGjelderId = oppdrag110.oppdragGjelderId

        val gjelderId =
            oppdragGjelderId.personidentifikator()
                ?: error("oppdragGjelderId var ikke en personidentifikator")

        return builder
            .periode(Periode.ÅpenPeriode(oppdrag110.datoOppdragGjelderFom), gjelderId)
            .build()
    }
}
