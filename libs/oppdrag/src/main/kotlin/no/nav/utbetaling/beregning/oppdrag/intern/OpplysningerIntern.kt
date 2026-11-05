package no.nav.utbetaling.beregning.oppdrag.intern

import java.time.LocalDate

import no.nav.utbetaling.beregning.oppdrag.Avventingsårsak
import no.nav.utbetaling.beregning.oppdrag.BehandlendeEnhet
import no.nav.utbetaling.beregning.oppdrag.Bilagstype
import no.nav.utbetaling.beregning.oppdrag.Bostedsenhet
import no.nav.utbetaling.beregning.oppdrag.BrukKjøreplan
import no.nav.utbetaling.beregning.oppdrag.DelytelseId
import no.nav.utbetaling.beregning.oppdrag.Fagområde
import no.nav.utbetaling.beregning.oppdrag.FagsystemId
import no.nav.utbetaling.beregning.oppdrag.Gjelder
import no.nav.utbetaling.beregning.oppdrag.Henvisning
import no.nav.utbetaling.beregning.oppdrag.Kidnummer
import no.nav.utbetaling.beregning.oppdrag.Ompostering
import no.nav.utbetaling.beregning.oppdrag.Opplysning
import no.nav.utbetaling.beregning.oppdrag.RefunderesTil
import no.nav.utbetaling.beregning.oppdrag.SaksbehandlerId
import no.nav.utbetaling.beregning.oppdrag.Sats
import no.nav.utbetaling.beregning.oppdrag.Skyldner
import no.nav.utbetaling.beregning.oppdrag.Status
import no.nav.utbetaling.beregning.oppdrag.StønadId
import no.nav.utbetaling.beregning.oppdrag.Søknadstype
import no.nav.utbetaling.beregning.oppdrag.UtbetalesTil
import no.nav.utbetaling.beregning.oppdrag.Utbetalingsfrekvens
import no.nav.utbetaling.beregning.oppdrag.VedtakId
import no.nav.utbetaling.beregning.oppdrag.YtelseKlassekode
import no.nav.utbetaling.beregning.oppdrag.intern.FeilregistrerbarOpplysning.Companion.gjeldende
import no.nav.utbetaling.beregning.tid.Periode
import no.nav.utbetaling.beregning.tid.Tidslinje

internal data class OpplysningerIntern(
    val delytelseId: DelytelseId,
    val refDelytelseId: DelytelseId?,
    val sats: Opplysning<Sats>,
    val vedtakssats: Opplysning<Sats>?,
    val skyldner: Opplysning<Skyldner>?,
    val refunderesTil: Opplysning<RefunderesTil>?,
    val maksdato: Opplysning<LocalDate>?,
    val søknadstype: Opplysning<Søknadstype>?,
    val henvisning: Opplysning<Henvisning>?,
    val kidnummer: Opplysning<Kidnummer>?,
    val status: Opplysning<Status>,
    val bostedsenhet: Opplysning<Bostedsenhet>?,
    val gjelder: Opplysning<Gjelder>?,
    val utbetalesTil: Opplysning<UtbetalesTil>?,
    val vedtakId: Opplysning<VedtakId>?,
    val brukKjøreplan: Opplysning<BrukKjøreplan>,
    val ytelseKlasse: Opplysning<YtelseKlassekode>,
    private val behandlendeEnhetEndring: FeilregistrerbarOpplysning<BehandlendeEnhet>?,
    private val avventingsårsakEndring: FeilregistrerbarOpplysning<Avventingsårsak>?,
    private val omposteringEndring: FeilregistrerbarOpplysning<Ompostering>?,
) {
    val avventingsårsak = avventingsårsakEndring?.gjeldende()
    val behandlendeEnhet = behandlendeEnhetEndring?.gjeldende()
    val ompostering = omposteringEndring?.gjeldende()

    private fun medNyeVerdierFra(ny: OpplysningerIntern) =
        OpplysningerIntern(
            delytelseId = ny.delytelseId,
            refDelytelseId = ny.refDelytelseId,
            sats = velgOpplysning(ny.sats, sats),
            // TODO: Avklar om en tidligere vedtakssats skal videreføres når en ny linje mangler feltet.
            vedtakssats = ny.vedtakssats,
            skyldner = velgNullableOpplysning(ny.skyldner, this.skyldner),
            refunderesTil = velgNullableOpplysning(ny.refunderesTil, this.refunderesTil),
            maksdato = velgNullableOpplysning(ny.maksdato, this.maksdato),
            søknadstype = velgNullableOpplysning(ny.søknadstype, this.søknadstype),
            henvisning = velgNullableOpplysning(ny.henvisning, henvisning),
            kidnummer = velgNullableOpplysning(ny.kidnummer, kidnummer),
            avventingsårsakEndring = FeilregistrerbarOpplysning.velg(this.avventingsårsakEndring, ny.avventingsårsakEndring),
            status = velgOpplysning(ny.status, this.status),
            bostedsenhet = velgNullableOpplysning(ny.bostedsenhet, this.bostedsenhet),
            behandlendeEnhetEndring = FeilregistrerbarOpplysning.velg(this.behandlendeEnhetEndring, ny.behandlendeEnhetEndring),
            gjelder = velgNullableOpplysning(ny.gjelder, this.gjelder),
            utbetalesTil = velgNullableOpplysning(ny.utbetalesTil, this.utbetalesTil),
            vedtakId = velgNullableOpplysning(ny.vedtakId, this.vedtakId),
            brukKjøreplan = velgOpplysning(ny.brukKjøreplan, this.brukKjøreplan),
            omposteringEndring = FeilregistrerbarOpplysning.velg(this.omposteringEndring, ny.omposteringEndring),
            ytelseKlasse = velgOpplysning(ny.ytelseKlasse, this.ytelseKlasse),
        )

    data class Builder(
        private val delytelseId: DelytelseId,
        private val linjeSaksbehandlerId: SaksbehandlerId,
        private val oppdragSaksbehandlerId: SaksbehandlerId,
        private val refDelytelseId: DelytelseId?,
        private val henvisning: Henvisning?,
        private val sats: Sats,
        private val vedtakssats: Sats? = null,
        private val skyldner: Skyldner? = null,
        private val refunderesTil: RefunderesTil? = null,
        private val maksdato: LocalDate? = null,
        private val søknadstype: Søknadstype? = null,
        private val kidnummer: Kidnummer? = null,
        private val status: Status? = null,
        private val bostedsenhet: Bostedsenhet? = null,
        private val gjelder: Gjelder? = null,
        private val utbetalesTil: UtbetalesTil? = null,
        private val vedtakId: VedtakId? = null,
        private val brukKjøreplan: BrukKjøreplan? = null,
        private val ytelseKlassekode: YtelseKlassekode,
        private val behandlendeEnhet: FeilregistrerbarOpplysning<BehandlendeEnhet>? = null,
        private val avventingsårsakEndring: FeilregistrerbarOpplysning<Avventingsårsak>? = null,
        private val omposteringEndring: FeilregistrerbarOpplysning<Ompostering>? = null,
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
                brukKjøreplan = Opplysning(linjeSaksbehandlerId, brukKjøreplan ?: BrukKjøreplan.Ja),
                sats = Opplysning(linjeSaksbehandlerId, sats),
                vedtakssats = vedtakssats?.let { Opplysning(linjeSaksbehandlerId, it) },
                skyldner = skyldner?.let { Opplysning(linjeSaksbehandlerId, it) },
                refunderesTil = refunderesTil?.let { Opplysning(linjeSaksbehandlerId, it) },
                maksdato = maksdato?.let { Opplysning(linjeSaksbehandlerId, it) },
                søknadstype = søknadstype?.let { Opplysning(linjeSaksbehandlerId, it) },
                henvisning = henvisning?.let { Opplysning(linjeSaksbehandlerId, it) },
                kidnummer = kidnummer?.let { Opplysning(linjeSaksbehandlerId, it) },
                avventingsårsakEndring = avventingsårsakEndring,
                status = Opplysning(linjeSaksbehandlerId, (status ?: Status.Aktiv)),
                bostedsenhet = bostedsenhet?.let { Opplysning(oppdragSaksbehandlerId, it) },
                behandlendeEnhetEndring = behandlendeEnhet,
                gjelder = gjelder?.let { Opplysning(oppdragSaksbehandlerId, it) },
                utbetalesTil = utbetalesTil?.let { Opplysning(linjeSaksbehandlerId, it) },
                vedtakId = vedtakId?.let { Opplysning(linjeSaksbehandlerId, it) },
                omposteringEndring = omposteringEndring,
                ytelseKlasse = Opplysning(linjeSaksbehandlerId, ytelseKlassekode),
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
            oppdragsdag: OpplysningerIntern,
            status: Opplysning<Status>?,
            unntatt: Set<DelytelseId> = emptySet(),
        ) = when {
            status == null -> oppdragsdag
            oppdragsdag.delytelseId in unntatt -> oppdragsdag
            else -> oppdragsdag.copy(status = velgOpplysning(status, oppdragsdag.status))
        }

        fun velgStatus(
            oppdragsdag: OpplysningerIntern,
            statusendring: Statusendring?,
        ) = velgStatus(oppdragsdag, statusendring?.status, statusendring?.unntatt ?: emptySet())

        fun videreførGjelder(
            oppdragsdag: OpplysningerIntern,
            gjelder: Opplysning<Gjelder>?,
        ) = when {
            gjelder == null -> oppdragsdag
            // Om det er satt en gjelder eksplisitt brukes den
            oppdragsdag.gjelder != null -> oppdragsdag
            // Om det mangler så viderefører vi gjelder
            else -> oppdragsdag.copy(gjelder = gjelder)
        }

        fun videreførUtbetalesTil(
            oppdragsdag: OpplysningerIntern,
            utbetalesTil: Opplysning<UtbetalesTil>?,
        ) = when {
            utbetalesTil == null -> oppdragsdag
            // Om det er satt en utbetalesTil eksplisitt brukes den
            oppdragsdag.utbetalesTil != null -> oppdragsdag
            // Om det mangler så viderefører vi utbetalesTIl
            else -> oppdragsdag.copy(utbetalesTil = utbetalesTil)
        }

        fun videreførSkyldner(
            oppdragsdag: OpplysningerIntern,
            skyldner: Opplysning<Skyldner>?,
        ) = when {
            skyldner == null -> oppdragsdag
            // Om det er satt en skyldner eksplisitt brukes den
            oppdragsdag.skyldner != null -> oppdragsdag
            // Om det mangler så viderefører vi skyldner
            else -> oppdragsdag.copy(skyldner = skyldner)
        }

        fun videreførRefunderesTil(
            oppdragsdag: OpplysningerIntern,
            refunderesTil: Opplysning<RefunderesTil>?,
        ) = when {
            refunderesTil == null -> oppdragsdag
            // Om det er satt en refunderesTil eksplisitt brukes den
            oppdragsdag.refunderesTil != null -> oppdragsdag
            // Om det mangler så viderefører vi refunderesTIl
            else -> oppdragsdag.copy(refunderesTil = refunderesTil)
        }

        fun videreførMaksdato(
            oppdragsdag: OpplysningerIntern,
            maksdato: Opplysning<LocalDate>?,
        ) = when {
            maksdato == null -> oppdragsdag
            // Om det er satt en maksdato eksplisitt brukes den
            oppdragsdag.maksdato != null -> oppdragsdag
            // Om det mangler så viderefører vi maksdato
            else -> oppdragsdag.copy(maksdato = maksdato)
        }

        fun <T> Tidslinje<OpplysningerIntern>.viderefør(selector: (OpplysningerIntern) -> Opplysning<T>?): Tidslinje<Opplysning<T>> {
            val builder = Tidslinje.Builder<Opplysning<T>>()
            val (fraOgMed, verdi) =
                lastOrNull { (_, opplysninger) -> selector(opplysninger) != null }
                    ?.let { (periode, opplysninger) -> periode.fom to checkNotNull(selector(opplysninger)) }
                    ?: return builder.build()

            return builder
                .periode(Periode.ÅpenPeriode(fraOgMed), verdi)
                .build()
        }
    }
}

internal data class Kjedeendringer(
    val sluttdato: LocalDate?,
    val kjedestatus: Tidslinje<Statusendring>,
)

internal data class OppdragId(
    val fagområde: Fagområde,
    val fagsystemId: FagsystemId,
)

internal data class Oppdragsendringer(
    val oppdragstatus: Tidslinje<Statusendring>,
)

internal data class Oppdragsverdier(
    val stønadId: Opplysning<StønadId>?,
    val utbetalingsfrekvens: Opplysning<Utbetalingsfrekvens>?,
    val bilagstype: Opplysning<Bilagstype>?,
    val forfall: Opplysning<LocalDate>?,
) {
    fun medNyeVerdierFra(ny: Oppdragsverdier) =
        Oppdragsverdier(
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
