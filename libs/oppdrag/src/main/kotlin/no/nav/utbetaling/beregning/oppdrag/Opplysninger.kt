package no.nav.utbetaling.beregning.oppdrag

import java.math.BigDecimal
import java.time.LocalDate

@JvmInline
value class Personidentifikator(
    val identifikator: String,
)

@JvmInline
value class Organisasjonsnummer(
    val nummer: String,
)

@JvmInline
value class SamhandlerId(
    val tssId: String,
)

@JvmInline
value class SaksbehandlerId(
    val id: String,
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

data class Bostedsenhet(
    val enhet: String,
)

data class BehandlendeEnhet(
    val enhet: String,
)

sealed interface UtbetalesTil {
    data class Person(
        val personidentifikator: Personidentifikator,
    ) : UtbetalesTil

    data class Organisasjon(
        val organisasjonsnummer: Organisasjonsnummer,
    ) : UtbetalesTil

    data class Samhandler(
        val samhandlerId: SamhandlerId,
    ) : UtbetalesTil
}

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
    val gjelderId: Opplysning<Personidentifikator>?,
    val utbetalesTil: Opplysning<UtbetalesTil>?,
    val sats: Opplysning<BigDecimal>,
    val henvisning: Opplysning<Henvisning>?,
    val stønadId: Opplysning<StønadId>?,
    val forfall: Opplysning<LocalDate>?,
    val utbetalingsfrekvens: Opplysning<Utbetalingsfrekvens>?,
    val bilagstype: Opplysning<Bilagstype>?,
    val avventingsårsak: Opplysning<Avventingsårsak>?,
    val kidnummer: Opplysning<Kidnummer>?,
    val status: Opplysning<Status>,
    val bostedsenhet: Opplysning<Bostedsenhet>?,
    val behandlendeEnhet: Opplysning<BehandlendeEnhet>?,
    /** TODO: Eksempelvis gjelderId, utbetalesTil, bostedsenhet må ikke være satt alle innsendinger,
     *  men når man har kombinert alle innsendinger til en tidslinje, da må disse verdiene være satt.
     *  Burde man introdusert noe som en SammenslåtteOpplysninger el.lig. som håndterer dette?
     */
)
