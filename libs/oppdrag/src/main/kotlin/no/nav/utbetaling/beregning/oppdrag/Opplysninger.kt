package no.nav.utbetaling.beregning.oppdrag

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@JvmInline
value class SaksbehandlerId(
    val id: String,
)

@JvmInline
value class OppdragId(
    val id: Long,
)

@JvmInline
value class Bilagstype(
    val type: String,
)

@JvmInline
value class Henvisning(
    val henvisning: String,
)

@JvmInline
value class StønadId(
    val id: String,
)

@JvmInline
value class Kidnummer(
    val nummer: String,
)

@JvmInline
value class Omposteringskode(
    val kode: String,
)

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

data class Opplysning<T>(
    val saksbehandlerId: SaksbehandlerId,
    val opplysning: T,
)

data class Ompostering(
    val kode: Omposteringskode,
    val registrert: Instant,
)

data class Bostedsenhet(
    val enhet: String,
)

data class BehandlendeEnhet(
    val enhet: String,
)

enum class Utbetalingsfrekvens {
    Daglig,
    Ukentlig,
    Månedlig,
    PerFjortendeDag,
    Engangsutbetaling,
}

enum class Status {
    Aktiv,
    Hvilende,
    Sperret,
    Opphørt,
}

sealed interface Avventingsårsak {
    data class Avregning(
        val til: LocalDate,
    ) : Avventingsårsak

    data class Refusjonskrav(
        val til: LocalDate?,
    ) : Avventingsårsak
}

data class Opplysninger(
    val fagområde: Fagområde,
    val fagsystemId: FagsystemId,
    val delytelseId: DelytelseId,
    val refDelytelseId: DelytelseId?,
    val sats: Opplysning<BigDecimal>,
    val oppdragId: Opplysning<OppdragId>?,
    val henvisning: Opplysning<Henvisning>?,
    val stønadId: Opplysning<StønadId>?,
    val forfall: Opplysning<LocalDate>?,
    val utbetalingsfrekvens: Opplysning<Utbetalingsfrekvens>?,
    val bilagstype: Opplysning<Bilagstype>?,
    val avventingsårsak: Opplysning<Avventingsårsak>?,
    val ompostering: Opplysning<Ompostering>?,
    val kidnummer: Opplysning<Kidnummer>?,
    val status: Opplysning<Status>,
    val bostedsenhet: Opplysning<Bostedsenhet>,
    internal val nullableBehandlendeEnhet: Opplysning<BehandlendeEnhet>?,
) {
    val behandlendeEnhet = nullableBehandlendeEnhet ?: Opplysning(bostedsenhet.saksbehandlerId, BehandlendeEnhet(bostedsenhet.opplysning.enhet))
}
