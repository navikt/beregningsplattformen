package no.nav.utbetaling.beregning.oppdrag.intern

import no.nav.utbetaling.beregning.oppdrag.Kidnummer
import no.nav.utbetaling.beregning.tid.Periode
import no.nav.utbetaling.beregning.tid.Tidslinje

internal object Kidnumre {
    internal fun fra(linjer: Linjer): Tidslinje<Kidnummer> {
        return linjer.aktive
            .fold(Tidslinje.Builder<Kidnummer>()) kid@{ builder, (periode, linje) ->
                val kid = linje.kid?.mapVerdi { Kidnummer(it) } ?: return@kid builder
                val fom = linje.datoKidFom ?: periode.fom
                builder.periode(Periode.ÅpenPeriode(fom), kid)
            }.build()
    }
}
