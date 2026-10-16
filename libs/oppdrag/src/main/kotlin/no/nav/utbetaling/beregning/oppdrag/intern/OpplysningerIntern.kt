package no.nav.utbetaling.beregning.oppdrag.intern

import java.math.BigDecimal
import java.time.LocalDate

import no.nav.utbetaling.beregning.oppdrag.Avventingsårsak
import no.nav.utbetaling.beregning.oppdrag.BehandlendeEnhet
import no.nav.utbetaling.beregning.oppdrag.Bilagstype
import no.nav.utbetaling.beregning.oppdrag.Bostedsenhet
import no.nav.utbetaling.beregning.oppdrag.DelytelseId
import no.nav.utbetaling.beregning.oppdrag.Fagområde
import no.nav.utbetaling.beregning.oppdrag.FagsystemId
import no.nav.utbetaling.beregning.oppdrag.Henvisning
import no.nav.utbetaling.beregning.oppdrag.Kidnummer
import no.nav.utbetaling.beregning.oppdrag.Ompostering
import no.nav.utbetaling.beregning.oppdrag.OppdragId
import no.nav.utbetaling.beregning.oppdrag.Opplysning
import no.nav.utbetaling.beregning.oppdrag.SaksbehandlerId
import no.nav.utbetaling.beregning.oppdrag.Status
import no.nav.utbetaling.beregning.oppdrag.StønadId
import no.nav.utbetaling.beregning.oppdrag.Utbetalingsfrekvens
import no.nav.utbetaling.beregning.oppdrag.intern.FeilregistrerbarOpplysning.Companion.gjeldende
import no.nav.utbetaling.beregning.tid.Periode
import no.nav.utbetaling.beregning.tid.Tidslinje

internal data class OpplysningerIntern(
    val delytelseId: DelytelseId,
    val refDelytelseId: DelytelseId?,
    val sats: Opplysning<BigDecimal>,
    val henvisning: Opplysning<Henvisning>?,
    val kidnummer: Opplysning<Kidnummer>?,
    val status: Opplysning<Status>,
    val bostedsenhet: Opplysning<Bostedsenhet>?,
    val behandlendeEnhet: Opplysning<BehandlendeEnhet>?,
    private val avventingsårsakEndring: FeilregistrerbarOpplysning<Avventingsårsak>?,
    private val omposteringEndring: FeilregistrerbarOpplysning<Ompostering>?,
) {
    val avventingsårsak = avventingsårsakEndring?.gjeldende()
    val ompostering = omposteringEndring?.gjeldende()

    private fun medNyeVerdierFra(ny: OpplysningerIntern) =
        OpplysningerIntern(
            delytelseId = ny.delytelseId,
            refDelytelseId = ny.refDelytelseId,
            sats = velgOpplysning(ny.sats, sats),
            henvisning = velgNullableOpplysning(ny.henvisning, henvisning),
            kidnummer = velgNullableOpplysning(ny.kidnummer, kidnummer),
            avventingsårsakEndring = FeilregistrerbarOpplysning.velg(this.avventingsårsakEndring, ny.avventingsårsakEndring),
            omposteringEndring = FeilregistrerbarOpplysning.velg(this.omposteringEndring, ny.omposteringEndring),
            status = velgOpplysning(ny.status, this.status),
            bostedsenhet = velgNullableOpplysning(ny.bostedsenhet, this.bostedsenhet),
            behandlendeEnhet = velgNullableOpplysning(ny.behandlendeEnhet, this.behandlendeEnhet),
        )

    data class Builder(
        private val delytelseId: DelytelseId,
        private val linjeSaksbehandlerId: SaksbehandlerId,
        private val refDelytelseId: DelytelseId?,
        private val henvisning: Henvisning?,
        private val sats: BigDecimal,
        private val kidnummer: Kidnummer?,
        private val avventingsårsakEndring: FeilregistrerbarOpplysning<Avventingsårsak>? = null,
        private val omposteringEndring: FeilregistrerbarOpplysning<Ompostering>? = null,
        private val status: Status? = null,
        private val bostedsenhet: Opplysning<Bostedsenhet>? = null,
        private val behandlendeEnhet: Opplysning<BehandlendeEnhet>? = null,
    ) {
        fun medStatus(statusendring: Statusendring?): Builder =
            when {
                statusendring == null -> this
                delytelseId in statusendring.unntatt -> this
                else -> copy(status = statusendring.status.opplysning)
            }

        fun build() =
            OpplysningerIntern(
                delytelseId = delytelseId,
                refDelytelseId = refDelytelseId,
                sats = Opplysning(linjeSaksbehandlerId, sats),
                henvisning = henvisning?.let { Opplysning(linjeSaksbehandlerId, it) },
                kidnummer = kidnummer?.let { Opplysning(linjeSaksbehandlerId, it) },
                avventingsårsakEndring = avventingsårsakEndring,
                omposteringEndring = omposteringEndring,
                status = Opplysning(linjeSaksbehandlerId, (status ?: Status.Aktiv)),
                bostedsenhet = bostedsenhet,
                behandlendeEnhet = behandlendeEnhet,
            )
    }

    companion object {
        fun velg(
            eksisterende: OpplysningerIntern?,
            ny: OpplysningerIntern?,
            periode: Periode,
            kjedeendringsperiode: Periode,
        ) = when {
            // Om vi ikke har noe fra fær skal vi alltid velge ny
            eksisterende == null -> ny
            // Om den nye kjeden inneholder "hull" er skal det tolkes som at hullene skal fjernes, ikke videreføres fra eksisterende
            ny == null && periode in kjedeendringsperiode -> null
            ny == null -> eksisterende
            else -> eksisterende.medNyeVerdierFra(ny)
        }

        private fun velgStatus(
            oppdragsdag: OpplysningerIntern?,
            status: Opplysning<Status>?,
            unntatt: Set<DelytelseId> = emptySet(),
        ) = when {
            status == null || oppdragsdag == null -> oppdragsdag
            oppdragsdag.delytelseId in unntatt -> oppdragsdag
            else -> oppdragsdag.copy(status = velgOpplysning(status, oppdragsdag.status))
        }

        fun velgStatus(
            oppdragsdag: OpplysningerIntern?,
            statusendring: Statusendring?,
        ) = velgStatus(oppdragsdag, statusendring?.status, statusendring?.unntatt ?: emptySet())
    }
}

internal data class Kjedeendringer(
    val sluttdato: LocalDate?,
    val kjedestatus: Tidslinje<Statusendring>,
)

internal data class InternOppdragId(
    val fagområde: Fagområde,
    val fagsystemId: FagsystemId,
)

internal data class Oppdragsendringer(
    val oppdragstatus: Tidslinje<Statusendring>,
)

internal data class Oppdragsverdier(
    val oppdragId: Opplysning<OppdragId>?,
    val stønadId: Opplysning<StønadId>?,
    val utbetalingsfrekvens: Opplysning<Utbetalingsfrekvens>?,
    val bilagstype: Opplysning<Bilagstype>?,
    val forfall: Opplysning<LocalDate>?,
) {
    fun medNyeVerdierFra(ny: Oppdragsverdier) =
        Oppdragsverdier(
            oppdragId = velgNullableOpplysning(ny.oppdragId, oppdragId),
            stønadId = velgNullableOpplysning(ny.stønadId, stønadId),
            utbetalingsfrekvens = velgNullableOpplysning(ny.utbetalingsfrekvens, utbetalingsfrekvens),
            bilagstype = velgNullableOpplysning(ny.bilagstype, bilagstype),
            forfall = velgNullableOpplysning(ny.forfall, forfall),
        )
}

internal fun <T> velgNullableOpplysning(
    ny: Opplysning<T>?,
    eksisterende: Opplysning<T>?,
) = when {
    ny != null && eksisterende != null -> velgOpplysning(ny, eksisterende)
    else -> ny ?: eksisterende
}

private fun <T> velgOpplysning(
    ny: Opplysning<T>,
    gammel: Opplysning<T>,
) = ny.takeUnless { it.opplysning == gammel.opplysning } ?: gammel

internal fun <T> String?.mapVerdi(block: (verdi: String) -> T) =
    when {
        isNullOrBlank() -> null
        else -> block(trim())
    }
