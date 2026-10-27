package no.nav.sokos.dare.poc.beregning.ekstern

import kotlinx.serialization.Serializable

@Serializable
enum class BeregningsKilde {
    OPPDRAG_Z,
    POC,
}
