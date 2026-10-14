package no.nav.utbetaling.beregning.oppdrag.intern

import no.nav.utbetaling.beregning.oppdrag.Opplysning
import no.nav.utbetaling.beregning.tid.Tidslinje

/**
 *  Alle opplysningene i XML-formatet hvor man har mulighet til å sette "feilreg" for å fjerne en opplysning vi har fått tidligere.
 *  Om den er feilregistert må nåsituasjonen være at opplysningen er registrert, ellers får man en feilmelding.
 *  Eksempler; Avventing, Ompostering & Tekst
 */
internal sealed interface Endring<T> {
    val opplysning: Opplysning<T>

    data class Registrert<T>(
        override val opplysning: Opplysning<T>,
    ) : Endring<T>

    data class Feilregistrert<T>(
        override val opplysning: Opplysning<T>,
    ) : Endring<T>

    companion object {
        private val FEILREGISTRERT_VERDIER = setOf("j", "ja", "y", "yes")

        private fun String?.erFeilregistrert() = mapVerdi { it.lowercase() in FEILREGISTRERT_VERDIER } == true

        fun <T> Endring<T>?.gjeldende() = (this as? Registrert)?.opplysning

        fun <T> ingenEndringer() = Tidslinje.Builder<Endring<T>>().build()

        fun <T> velg(
            eksisterende: Endring<T>?,
            ny: Endring<T>?,
        ) = when (ny) {
            is Feilregistrert -> {
                require(eksisterende is Registrert) { "Kan ikke feilregistrere en endring som ikke finnes." }
                null
            }
            is Registrert -> ny
            else -> eksisterende
        }

        fun <T> fra(
            feilreg: String?,
            opplysning: Opplysning<T>,
        ): Endring<T> =
            when (feilreg.erFeilregistrert()) {
                true -> Feilregistrert(opplysning)
                false -> Registrert(opplysning)
            }
    }
}
