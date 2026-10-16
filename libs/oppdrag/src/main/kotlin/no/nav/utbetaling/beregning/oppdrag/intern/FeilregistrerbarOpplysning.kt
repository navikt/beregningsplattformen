package no.nav.utbetaling.beregning.oppdrag.intern

import no.nav.utbetaling.beregning.oppdrag.Opplysning
import no.nav.utbetaling.beregning.tid.Tidslinje

/**
 *  Alle opplysningene i XML-formatet hvor man har mulighet til å sette "feilreg" for å fjerne en opplysning vi har fått tidligere.
 *  Om den er feilregistert må nåsituasjonen være at opplysningen er registrert, ellers får man en feilmelding.
 *  Eksempler; Avventing & Tekst
 */
internal sealed interface FeilregistrerbarOpplysning<out T> {
    data class Registrert<T>(
        val opplysning: Opplysning<T>,
    ) : FeilregistrerbarOpplysning<T>

    object Feilregistrert : FeilregistrerbarOpplysning<Nothing>

    companion object {
        private val FEILREGISTRERT_VERDIER = setOf("j", "ja", "y", "yes")

        private fun String?.erFeilregistrert() = mapVerdi { it.lowercase() in FEILREGISTRERT_VERDIER } == true

        fun <T> FeilregistrerbarOpplysning<T>?.gjeldende() = (this as? Registrert)?.opplysning

        fun <T> ingenEndringer() = Tidslinje.Builder<FeilregistrerbarOpplysning<T>>().build()

        fun <T> velg(
            eksisterende: FeilregistrerbarOpplysning<T>?,
            ny: FeilregistrerbarOpplysning<T>?,
        ) = when (ny) {
            Feilregistrert -> {
                require(eksisterende is Registrert) { "Kan ikke feilregistrere en endring som ikke finnes." }
                null
            }
            is Registrert -> ny
            else -> eksisterende
        }

        fun <T> fra(
            feilreg: String?,
            opplysning: Opplysning<T>,
        ): FeilregistrerbarOpplysning<T> =
            when (feilreg.erFeilregistrert()) {
                true -> Feilregistrert
                false -> Registrert(opplysning)
            }
    }
}
