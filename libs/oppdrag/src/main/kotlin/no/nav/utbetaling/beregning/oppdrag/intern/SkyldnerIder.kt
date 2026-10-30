package no.nav.utbetaling.beregning.oppdrag.intern

import no.nav.utbetaling.beregning.oppdrag.Part
import no.nav.utbetaling.beregning.tid.Periode
import no.nav.utbetaling.beregning.tid.Tidslinje

internal object SkyldnerIder {
    fun fra(linjer: Linjer): Tidslinje<Part> {
        val builder = Tidslinje.Builder<Part>()

        linjer.aktive.forEach linje@{ (_, linje) ->
            val skyldnerId = linje.skyldnerId ?: return@linje
            val skyldnerFom = linje.datoSkyldnerFom ?: linje.datoVedtakFom

            builder.periode(
                periode = Periode.ÅpenPeriode(skyldnerFom),
                verdi = Partklassifisering.fra(skyldnerId),
            )
        }
        return builder.build()
    }
}
