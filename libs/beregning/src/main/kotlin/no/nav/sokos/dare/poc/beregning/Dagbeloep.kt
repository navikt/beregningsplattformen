package no.nav.sokos.dare.poc.beregning

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

import no.nav.sokos.dare.poc.BigDecimalJson
import no.nav.sokos.dare.poc.LocalDateJson

// TODO: Vurdere å bytte navn på denne for å ikke forvirre med DagBeloepMap som er en ren kalkuleringsklasse
// Et dagbeløp representerer en mengde penger, positiv eller negativ, på en bestemt tag. Tilhører en delberegningslinje.
@Serializable
data class Dagbeloep(
    var id: Long? = null,
    @Contextual val dato: LocalDateJson,
    @Contextual val beloep: BigDecimalJson,
    val delberegningslinjeId: DelberegningslinjeId? = null,
) {
    operator fun unaryMinus() = copy(beloep = -beloep)
}

fun List<Dagbeloep>.sum() = this.map { it.beloep }.sumOf { it }
