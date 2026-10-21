package no.nav.utbetaling.beregning.lab.api

import kotlinx.serialization.Serializable

@Serializable
class BeregningRequest(
    val xml: List<String>,
)
