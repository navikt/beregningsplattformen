package no.nav.utbetaling.beregning.lab.beregning

import java.time.LocalDate

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
class BeregningRequest(
    @Contextual
    val fom: LocalDate? = null,
    @Contextual
    val tom: LocalDate? = null,
    @Contextual
    val beregningsTidspunkt: LocalDate? = null,
    val xml: List<String>,
    val skattekort: SkattekortDTO? = null,
    val trekk: List<TrekkDTO> = emptyList(),
)
