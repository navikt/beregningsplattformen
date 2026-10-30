package no.nav.utbetaling.beregning.oppdrag.intern

import no.nav.utbetaling.beregning.oppdrag.Part
import no.nav.utbetaling.beregning.oppdrag.UtbetalesTil
import no.nav.utbetaling.beregning.tid.Periode
import no.nav.utbetaling.beregning.tid.Tidslinje

internal object UtbetalesTilIder {
    fun fra(linjer: Linjer): Tidslinje<UtbetalesTil> {
        val builder = Tidslinje.Builder<UtbetalesTil>()

        linjer.aktive.forEach linje@{ (_, linje) ->
            val utbetalesTilId = linje.utbetalesTilId ?: return@linje
            val utbetalesTilFom = linje.datoUtbetalesTilIdFom ?: linje.datoVedtakFom

            val utbetalesTil =
                when (val part = Partklassifisering.fra(utbetalesTilId)) {
                    is Part.Samhandler -> UtbetalesTil.Samhandler(part.samhandlerId)
                    is Part.Person -> UtbetalesTil.Person(part.personidentifikator)
                    is Part.Organisasjon -> UtbetalesTil.Organisasjon(part.organisasjonsnummer)
                }

            builder.periode(
                periode = Periode.ÅpenPeriode(utbetalesTilFom),
                verdi = utbetalesTil,
            )
        }
        return builder.build()
    }
}
