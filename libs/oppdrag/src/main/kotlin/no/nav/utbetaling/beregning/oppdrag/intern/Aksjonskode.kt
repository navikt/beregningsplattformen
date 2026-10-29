package no.nav.utbetaling.beregning.oppdrag.intern

internal enum class Aksjonskode(
    private val xmlVerdier: Set<String>,
) {
    Utbetaling(setOf("1", "O")),
    Simulering(setOf("3", "S")),
    ;

    companion object {
        fun fraXmlOrNull(fraXml: String) = entries.firstOrNull { fraXml in it.xmlVerdier }

        fun fraXml(fraXml: String) = fraXmlOrNull(fraXml) ?: error("Ukjent aksjonskode: $fraXml. Gyldige verdier er: ${entries.flatMap { it.xmlVerdier }}")
    }
}
