package no.nav.utbetaling.beregning.oppdrag.intern

@JvmInline
value class Fagområde(
    val fagområde: String,
)

@JvmInline
value class FagsystemId(
    val id: String,
)

@JvmInline
value class DelytelseId(
    val id: String,
)

data class Kjede(
    val fagområde: Fagområde,
    val fagsystemId: FagsystemId,
    val delytelseIder: Set<DelytelseId>,
)
