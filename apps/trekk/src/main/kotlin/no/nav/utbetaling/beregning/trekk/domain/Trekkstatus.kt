package no.nav.utbetaling.beregning.trekk.domain

enum class Trekkstatus(
    val beskrivelse: String,
) {
    AVSL("Avsluttet"),
    FULL("Fullført"),
    HVIL("Hvilende"),
    KANS("Kansellert"),
    LOPE("Aktivt"),
    ;

    companion object {
        fun fromString(status: String): Trekkstatus = entries.first { it.name == status }
    }
}
