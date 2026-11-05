package no.nav.utbetaling.beregning.oppdrag.intern

import no.nav.utbetaling.beregning.oppdrag.RefunderesTil
import no.nav.utbetaling.beregning.tid.Periode
import no.nav.utbetaling.beregning.tid.Tidslinje

internal object RefunderesTilIder {
    fun fra(linjer: Linjer): Tidslinje<RefunderesTil> {
        val builder = Tidslinje.Builder<RefunderesTil>()

        linjer.aktive.forEach linje@{ (_, linje) ->
            val refusjonsinfo156 = linje.refusjonsinfo156 ?: return@linje
            val refunderesId = refusjonsinfo156.refunderesId ?: return@linje

            val refunderesTil =
                refunderesId.organisasjonsnummer()
                    ?: error("refunderesId på linje ${linje.delytelseId} er ikke organisasjonsnummer")

            builder.periode(
                periode = Periode.ÅpenPeriode(refusjonsinfo156.datoFom),
                verdi = refunderesTil,
            )
        }
        return builder.build()
    }
}
