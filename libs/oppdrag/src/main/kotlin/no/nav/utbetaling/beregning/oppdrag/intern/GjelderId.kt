package no.nav.utbetaling.beregning.oppdrag.intern

import no.nav.utbetaling.beregning.oppdrag.Oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.Opplysning
import no.nav.utbetaling.beregning.oppdrag.Personidentifikator
import no.nav.utbetaling.beregning.oppdrag.xml.Oppdrag110
import no.nav.utbetaling.beregning.tid.Periode
import no.nav.utbetaling.beregning.tid.Tidslinje

internal object GjelderId {
    fun fra(oppdrag110: Oppdrag110): Tidslinje<Personidentifikator> {
        val builder = Tidslinje.Builder<Personidentifikator>()

        val gjelderId = oppdrag110.oppdragGjelderId.mapVerdi { Personidentifikator(it) } ?: return builder.build()

        return builder
            .periode(Periode.ÅpenPeriode(oppdrag110.datoOppdragGjelderFom), gjelderId)
            .build()
    }

    fun fra(oppdragstidslinje: Oppdragstidslinje): Tidslinje<Opplysning<Personidentifikator>> {
        val builder = Tidslinje.Builder<Opplysning<Personidentifikator>>()

        oppdragstidslinje.perioderIterator().forEach periode@{ (periode, opplysninger) ->
            val gjelder = opplysninger.lastOrNull { it.gjelderId != null }?.gjelderId ?: return@periode
            builder.periode(Periode.ÅpenPeriode(periode.fom), gjelder)
        }
        return builder.build()
    }
}
