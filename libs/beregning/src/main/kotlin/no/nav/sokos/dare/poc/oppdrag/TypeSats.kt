package no.nav.sokos.dare.poc.oppdrag

enum class TypeSats(
    private val satsNavn: String,
) {
    DAG("DAG"),
    UKE("UKE"), // På vei ut
    MND("MND"),
    FJORTEN_DAGER("14D"), // På vei ut
    AAR("AAR"), // på vei ut
    ENGANGS("ENG"),
    AKTO("AKTO"), // A-konto utbetaling (aldri brukt)
    ;

    companion object {
        fun fromValue(value: String): TypeSats = entries.first { it.satsNavn == value }
    }
}
