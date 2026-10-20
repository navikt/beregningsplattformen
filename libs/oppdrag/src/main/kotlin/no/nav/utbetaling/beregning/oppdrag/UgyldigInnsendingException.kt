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
            if (value) return
            throw UgyldigInnsendingException(feilmelding())
        }

        inline fun <T> validerIkkeNull(
            value: T?,
            feilmelding: () -> String,
        ) = valider(value != null, feilmelding).let { value!! }

        fun ugyldigInnsending(
            feilmelding: String,
            throwable: Throwable? = null,
        ): Nothing = throw UgyldigInnsendingException(feilmelding, throwable)
    }
}
