package no.nav.utbetaling.beregning.oppdrag

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/** Reglene på hva slags typer disse kan være
 er nå basert på hva vi tror fagsystemene sender inn,
 ettersom det er det oppdragstidslinjen er tenkt til å representere.
 Dette regelsettet er ikke rett om man ser på beregning i stort.
 (og kan åpenbart ha mangler ettersom vi ikke kjenner til hva alle fagystemer sender ennå)
*/
sealed interface Gjelder

sealed interface UtbetalesTil

sealed interface RefunderesTil

sealed interface Skyldner

@JvmInline
value class Personidentifikator(
    val identifikator: String,
) : Gjelder,
    UtbetalesTil

@JvmInline
value class Organisasjonsnummer(
    val nummer: String,
) : UtbetalesTil,
    RefunderesTil

@JvmInline
value class SamhandlerId(
    val tssId: String,
) : Skyldner

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

@JvmInline
value class VedtakId(
    val id: String,
)

@JvmInline
value class Bostedsenhet(
    val enhet: String,
)

@JvmInline
value class BehandlendeEnhet(
    val enhet: String,
)

@JvmInline
value class YtelseKlassekode(
    val kode: String,
)

data class Opplysning<T>(
    val saksbehandlerId: SaksbehandlerId,
    val opplysning: T,
)

@JvmInline
value class Søknadstype(
    val type: String,
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

enum class BrukKjøreplan {
    Ja,
    Nei,
}

sealed interface Avventingsårsak {
    data class Avregning(
        val til: LocalDate,
    ) : Avventingsårsak

    data class Refusjonskrav(
        val til: LocalDate?,
    ) : Avventingsårsak
}

data class Ompostering(
    val registrert: Instant,
)

data class Sats(
    val verdi: BigDecimal,
    val satsType: String, // TODO gjør om til enum se TypeSats
)

data class Opplysninger(
    val fagområde: Fagområde,
    val fagsystemId: FagsystemId,
    val delytelseId: DelytelseId,
    val refDelytelseId: DelytelseId?,
    val gjelder: Opplysning<Gjelder>?,
    val utbetalesTil: Opplysning<UtbetalesTil>?,
    val sats: Opplysning<Sats>,
    val vedtakssats: Opplysning<Sats>?,
    val skyldner: Opplysning<Skyldner>?,
    val refunderesTil: Opplysning<RefunderesTil>?,
    val maksdato: Opplysning<LocalDate>?,
    val søknadstype: Opplysning<Søknadstype>?,
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
    val vedtakId: Opplysning<VedtakId>?,
    val brukKjøreplan: Opplysning<BrukKjøreplan>,
    val ompostering: Opplysning<Ompostering>?,
    val ytelseKlassekode: Opplysning<YtelseKlassekode>,
    /** TODO: Eksempelvis gjelderId, utbetalesTil, bostedsenhet må ikke være satt alle innsendinger,
     *  men når man har kombinert alle innsendinger til en tidslinje, da må disse verdiene være satt.
     *  Burde man introdusert noe som en SammenslåtteOpplysninger el.lig. som håndterer dette?
     */
)
