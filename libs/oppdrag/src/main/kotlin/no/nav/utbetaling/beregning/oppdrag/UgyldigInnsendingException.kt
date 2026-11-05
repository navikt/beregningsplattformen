package no.nav.utbetaling.beregning.oppdrag

internal class UgyldigInnsendingException private constructor(
    melding: String,
    throwable: Throwable? = null,
) : RuntimeException(melding, throwable) {
    companion object {
        inline fun valider(
            value: Boolean,
            feilmelding: () -> String,
        ) {
            if (!value) {
                ugyldigInnsending(feilmelding())
            }
        }

        inline fun <T : Any> validerIkkeNull(
            value: T?,
            feilmelding: () -> String,
        ): T = value ?: ugyldigInnsending(feilmelding())

        fun ugyldigInnsending(
            feilmelding: String,
            throwable: Throwable? = null,
        ): Nothing = throw UgyldigInnsendingException(feilmelding, throwable)
    }
}
