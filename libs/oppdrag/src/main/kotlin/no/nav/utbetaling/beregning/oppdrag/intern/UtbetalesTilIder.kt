package no.nav.utbetaling.beregning.oppdrag.intern

import no.nav.utbetaling.beregning.oppdrag.Opplysning
import no.nav.utbetaling.beregning.oppdrag.Organisasjonsnummer
import no.nav.utbetaling.beregning.oppdrag.Personidentifikator
import no.nav.utbetaling.beregning.oppdrag.SamhandlerId
import no.nav.utbetaling.beregning.oppdrag.UtbetalesTil
import no.nav.utbetaling.beregning.tid.Periode
import no.nav.utbetaling.beregning.tid.Tidslinje

internal object UtbetalesTilIder {
    fun fra(linjer: Linjer): Tidslinje<UtbetalesTil> {
        val builder = Tidslinje.Builder<UtbetalesTil>()

        linjer.aktive.forEach linje@{ (periode, linje) ->
            val utbetalesTilId = linje.utbetalesTilId ?: return@linje
            val utbetalesTilFom = linje.datoUtbetalesTilIdFom ?: linje.datoVedtakFom

            val utbetalesTil =
                when {
                    utbetalesTilId.matches(Regex("8\\d{10}")) -> UtbetalesTil.Samhandler(SamhandlerId(utbetalesTilId))
                    utbetalesTilId.matches(Regex("\\d{11}")) -> UtbetalesTil.Person(Personidentifikator(utbetalesTilId))
                    utbetalesTilId.matches(Regex("\\d{9}")) -> UtbetalesTil.Organisasjon(Organisasjonsnummer(utbetalesTilId))
                    else -> error("Ugyldig utbetalesTilId: $utbetalesTilId")
                }

            builder.periode(
                periode = Periode.ÅpenPeriode(utbetalesTilFom),
                verdi = utbetalesTil,
            )
        }
        return builder.build()
    }

    fun fra(tidslinje: Tidslinje<OpplysningerIntern>): Tidslinje<Opplysning<UtbetalesTil>> {
        val builder = Tidslinje.Builder<Opplysning<UtbetalesTil>>()

        val (periode, utbetalesTil) =
            tidslinje
                .lastOrNull { (_, opplysninger) -> opplysninger.utbetalesTil != null }
                ?.let { (periode, opplysninger) -> periode to checkNotNull(opplysninger.utbetalesTil) }
                ?: return builder.build()

        return builder
            .periode(Periode.ÅpenPeriode(periode.fom), utbetalesTil)
            .build()
    }
}
