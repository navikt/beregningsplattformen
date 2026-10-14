package no.nav.utbetaling.beregning.oppdrag.intern

import no.nav.utbetaling.beregning.oppdrag.DelytelseId
import no.nav.utbetaling.beregning.oppdrag.Fagområde
import no.nav.utbetaling.beregning.oppdrag.FagsystemId

internal data class Kjede(
    val fagområde: Fagområde,
    val fagsystemId: FagsystemId,
    val delytelseIder: Set<DelytelseId>,
) {
    fun hørerTil(other: Kjede) = fagområde == other.fagområde && fagsystemId == other.fagsystemId && delytelseIder.intersect(other.delytelseIder).isNotEmpty()

    operator fun plus(other: Kjede) = copy(delytelseIder = delytelseIder + other.delytelseIder)

    companion object {
        fun <T> Iterable<Pair<Kjede, T>>.grupperPåKjede(): Map<Kjede, List<T>> {
            val grupperteKjeder = mutableMapOf<Kjede, List<T>>()
            forEach { (kjede, verdi) ->
                val treff = grupperteKjeder.entries.filter { (eksisterendeKjede, _) -> eksisterendeKjede.hørerTil(kjede) }
                if (treff.isEmpty()) {
                    grupperteKjeder[kjede] = listOf(verdi)
                    return@forEach
                }
                val treffKjeder = treff.map { it.key }
                val samletKjede = treffKjeder.reduce { sammenslått, kjede -> sammenslått + kjede } + kjede
                val samletVerdi = treff.flatMap { it.value } + verdi
                treffKjeder.forEach { grupperteKjeder.remove(it) }
                grupperteKjeder[samletKjede] = samletVerdi
            }
            return grupperteKjeder.toMap()
        }
    }
}
