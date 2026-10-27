package no.nav.sokos.dare.poc.oppdrag

enum class OppdragEndringskode(
    val value: String,
) {
    NY("NY"),
    ENDR("ENDR"),
    UENDR("UENDR"),
    ;

    companion object {
        fun fromValue(value: String): OppdragEndringskode =
            values().firstOrNull { it.value == value }
                ?: throw NoSuchElementException("Ukjent OppdragEndringskode: $value")
    }
}
