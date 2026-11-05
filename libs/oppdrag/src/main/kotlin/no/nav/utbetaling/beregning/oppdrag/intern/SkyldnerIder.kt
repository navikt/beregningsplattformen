package no.nav.utbetaling.beregning.oppdrag.intern

import no.nav.utbetaling.beregning.oppdrag.Skyldner
import no.nav.utbetaling.beregning.tid.Periode
import no.nav.utbetaling.beregning.tid.Tidslinje

internal object SkyldnerIder {
    fun fra(linjer: Linjer): Tidslinje<Skyldner> {
        val builder = Tidslinje.Builder<Skyldner>()

        linjer.aktive.forEach linje@{ (_, linje) ->
            val skyldnerId = linje.skyldnerId ?: return@linje
            val skyldnerFom = linje.datoSkyldnerFom ?: linje.datoVedtakFom

            val skyldner =
                skyldnerId.samhandlerId()
                    ?: error("skyldnerId på linje ${linje.delytelseId} er ikke samhandlerId")

            builder.periode(
                periode = Periode.ÅpenPeriode(skyldnerFom),
                verdi = skyldner,
            )
        }
        return builder.build()
    }
}
