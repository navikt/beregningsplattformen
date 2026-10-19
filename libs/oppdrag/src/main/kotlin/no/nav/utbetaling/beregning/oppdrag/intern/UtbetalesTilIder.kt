package no.nav.utbetaling.beregning.oppdrag.intern

import no.nav.utbetaling.beregning.oppdrag.Organisasjonsnummer
import no.nav.utbetaling.beregning.oppdrag.Personidentifikator
import no.nav.utbetaling.beregning.oppdrag.SamhandlerId
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
                when {
                    utbetalesTilId.matches(samhandlerRegex) -> UtbetalesTil.Samhandler(SamhandlerId(tssId = utbetalesTilId))
                    utbetalesTilId.matches(personidentifikatorRegex) -> UtbetalesTil.Person(Personidentifikator(identifikator = utbetalesTilId))
                    utbetalesTilId.matches(organisasjonsnummerRegex) -> UtbetalesTil.Organisasjon(Organisasjonsnummer(nummer = utbetalesTilId))
                    else -> error("Uventet utbetalesTilId på linje ${linje.delytelseId}")
                }

            builder.periode(
                periode = Periode.ÅpenPeriode(utbetalesTilFom),
                verdi = utbetalesTil,
            )
        }
        return builder.build()
    }

    private val samhandlerRegex = Regex("8\\d{10}")
    private val personidentifikatorRegex = Regex("\\d{11}")
    private val organisasjonsnummerRegex = Regex("\\d{9}")
}
