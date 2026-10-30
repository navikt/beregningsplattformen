package no.nav.utbetaling.beregning.oppdrag.intern

import no.nav.utbetaling.beregning.oppdrag.Refusjonsinfo
import no.nav.utbetaling.beregning.tid.Periode
import no.nav.utbetaling.beregning.tid.Tidslinje

internal object Refusjonsinfoer {
    fun fra(linjer: Linjer): Tidslinje<Refusjonsinfo> {
        val builder = Tidslinje.Builder<Refusjonsinfo>()

        linjer.aktive.forEach linje@{ (_, linje) ->
            val refusjonsinfo156 = linje.refusjonsinfo156 ?: return@linje

            builder.periode(
                periode = Periode.ÅpenPeriode(refusjonsinfo156.datoFom),
                verdi =
                    Refusjonsinfo(
                        maksDato = refusjonsinfo156.maksDato,
                        refunderes = refusjonsinfo156.refunderesId?.let { Partklassifisering.fra(it) },
                        datoFom = refusjonsinfo156.datoFom,
                    ),
            )
        }
        return builder.build()
    }
}
