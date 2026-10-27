package no.nav.sokos.dare.poc.beregning.dataklasser

import kotlinx.serialization.Serializable

@Serializable
enum class BeregningsStatus(
    val erAktiv: Boolean,
) {
    BEREGNET(true),
    KORRIGERT(false),
    AVVENT(true),
    OVERFOERT(false),
    ;

    companion object {
        // En beregning som er i en aktiv tilstand kan settes til KORRIGERT og reberegnes uten å involvere reskontro.
        @Suppress("unused")
        val aktiveStatuser: Set<BeregningsStatus> = entries.filter { it.erAktiv }.toSet()

        // Beregninger som ikke er korrigerte og skal være med som relaterte beregninger i en ny beregning.
        val relaterbareStatuser: Set<BeregningsStatus> = entries.filter { it != KORRIGERT }.toSet()
    }
}
