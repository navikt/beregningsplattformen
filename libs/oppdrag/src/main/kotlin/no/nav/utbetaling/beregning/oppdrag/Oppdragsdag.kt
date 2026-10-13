package no.nav.utbetaling.beregning.oppdrag

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@JvmInline
value class SaksbehandlerId(
    val id: String,
)

data class Opplysning<T>(
    val saksbehandlerId: SaksbehandlerId,
    val opplysning: T,
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

enum class Utbetalingsfrekvens {
    Daglig,
    Ukentlig,
    Månedlig,
    PerFjortendeDag,
    Engangsutbetaling,
}

sealed interface Avventingsårsak {
    data class Avregning(
        val til: LocalDate,
    ) : Avventingsårsak

    data class Refusjonskrav(
        val til: LocalDate?,
    ) : Avventingsårsak
}

@JvmInline
value class Omposteringskode(
    val kode: String,
)

data class Ompostering(
    val kode: Omposteringskode,
    val registrert: Instant,
)

enum class Status {
    Aktiv,
    Hvilende,
    Sperret,
    Opphørt,
}

data class Oppdragsdag(
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
)
