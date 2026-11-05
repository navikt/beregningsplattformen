package no.nav.utbetaling.beregning.oppdrag.intern

import no.nav.utbetaling.beregning.oppdrag.Opplysning
import no.nav.utbetaling.beregning.tid.Periode
import no.nav.utbetaling.beregning.tid.Tidslinje

internal class Videreføring<T> private constructor(
    private val felt: (OpplysningerIntern) -> Opplysning<T>?,
    private val sett: OpplysningerIntern.(Opplysning<T>) -> OpplysningerIntern,
) {
    private fun videreførFra(
        eksisterende: Tidslinje<OpplysningerIntern>,
        tidslinje: Tidslinje<OpplysningerIntern>,
    ) = tidslinje.leftJoin(eksisterende.sisteKjente(felt)) { _, opplysninger, videreført ->
        when {
            videreført == null -> opplysninger
            felt(opplysninger) != null -> opplysninger
            else -> opplysninger.sett(videreført)
        }
    }

    companion object {
        /**
         * Felter som ikke nødvendigvis er satt i en ny innsending,
         * men om de ikke er satt betyr det ikke nødvendigvis at de skal nulles ut,
         * men de skal videreføres fra opplysninger vi har mottatt tidligere (om det finnes)
         */
        private val felter =
            listOf(
                Videreføring(OpplysningerIntern::gjelder) { copy(gjelder = it) },
                Videreføring(OpplysningerIntern::utbetalesTil) { copy(utbetalesTil = it) },
                Videreføring(OpplysningerIntern::skyldner) { copy(skyldner = it) },
                Videreføring(OpplysningerIntern::refunderesTil) { copy(refunderesTil = it) },
                Videreføring(OpplysningerIntern::maksdato) { copy(maksdato = it) },
            )

        fun Tidslinje<OpplysningerIntern>.videreførManglendeFra(eksisterende: Tidslinje<OpplysningerIntern>) = felter.fold(this) { tidslinje, felt -> felt.videreførFra(eksisterende, tidslinje) }

        private fun <T> Tidslinje<OpplysningerIntern>.sisteKjente(felt: (OpplysningerIntern) -> Opplysning<T>?): Tidslinje<Opplysning<T>> {
            val builder = Tidslinje.Builder<Opplysning<T>>()
            val (fraOgMed, verdi) =
                lastOrNull { (_, opplysninger) -> felt(opplysninger) != null }
                    ?.let { (periode, opplysninger) -> periode.fom to checkNotNull(felt(opplysninger)) }
                    ?: return builder.build()

            return builder
                .periode(Periode.ÅpenPeriode(fraOgMed), verdi)
                .build()
        }
    }
}
