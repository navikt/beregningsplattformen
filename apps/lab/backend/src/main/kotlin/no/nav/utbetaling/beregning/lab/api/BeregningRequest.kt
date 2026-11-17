package no.nav.utbetaling.beregning.lab.api

import java.time.LocalDate
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
class BeregningRequest(
    @Contextual
    val fom: LocalDate? = null,
    @Contextual
    val tom: LocalDate? = null,
    val xml: List<String>,
)
