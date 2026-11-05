package no.nav.utbetaling.beregning.oppdrag.intern

import java.time.LocalDate

import no.nav.utbetaling.beregning.tid.Periode
import no.nav.utbetaling.beregning.tid.Tidslinje

internal object Maksdatoer {
    fun fra(linjer: Linjer): Tidslinje<LocalDate> {
        val builder = Tidslinje.Builder<LocalDate>()

        linjer.aktive.forEach linje@{ (_, linje) ->
            val refusjonsinfo156 = linje.refusjonsinfo156 ?: return@linje
            val maksdato = refusjonsinfo156.maksDato ?: return@linje

            builder.periode(
                periode = Periode.ÅpenPeriode(refusjonsinfo156.datoFom),
                verdi = maksdato,
            )
        }
        return builder.build()
    }
}
