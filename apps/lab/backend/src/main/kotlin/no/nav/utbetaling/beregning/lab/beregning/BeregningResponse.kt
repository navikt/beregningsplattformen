package no.nav.utbetaling.beregning.lab.beregning

import kotlinx.serialization.Serializable

@Serializable
data class BeregningResponse(
    val oppdragstidslinje: OppdragstidslinjeDTO,
    val beregningstidslinje: BeregningstidslinjeDTO,
)
