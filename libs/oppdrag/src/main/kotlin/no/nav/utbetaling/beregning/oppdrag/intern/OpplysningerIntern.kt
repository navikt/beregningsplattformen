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
import no.nav.utbetaling.beregning.oppdrag.Henvisning
import no.nav.utbetaling.beregning.oppdrag.Kidnummer
import no.nav.utbetaling.beregning.oppdrag.Ompostering
import no.nav.utbetaling.beregning.oppdrag.Opplysning
import no.nav.utbetaling.beregning.oppdrag.Part
import no.nav.utbetaling.beregning.oppdrag.Personidentifikator
import no.nav.utbetaling.beregning.oppdrag.Refusjonsinfo
import no.nav.utbetaling.beregning.oppdrag.SaksbehandlerId
import no.nav.utbetaling.beregning.oppdrag.Sats
import no.nav.utbetaling.beregning.oppdrag.Status
import no.nav.utbetaling.beregning.oppdrag.StønadId
import no.nav.utbetaling.beregning.oppdrag.TypeSøknad
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
    val vedtaksSats: Opplysning<Sats>?,
    val skyldner: Opplysning<Part>?,
    val refusjonsinfo: Opplysning<Refusjonsinfo>?,
    val typeSøknad: Opplysning<TypeSøknad>?,
    val henvisning: Opplysning<Henvisning>?,
    val kidnummer: Opplysning<Kidnummer>?,
    val status: Opplysning<Status>,
    val bostedsenhet: Opplysning<Bostedsenhet>?,
    val gjelderId: Opplysning<Personidentifikator>?,
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
            vedtaksSats = velgNullableOpplysning(ny.vedtaksSats, vedtaksSats),
            skyldner = velgNullableOpplysning(ny.skyldner, this.skyldner),
            refusjonsinfo = velgNullableOpplysning(ny.refusjonsinfo, this.refusjonsinfo),
            typeSøknad = velgNullableOpplysning(ny.typeSøknad, this.typeSøknad),
            henvisning = velgNullableOpplysning(ny.henvisning, henvisning),
            kidnummer = velgNullableOpplysning(ny.kidnummer, kidnummer),
            avventingsårsakEndring = FeilregistrerbarOpplysning.velg(this.avventingsårsakEndring, ny.avventingsårsakEndring),
            status = velgOpplysning(ny.status, this.status),
            bostedsenhet = velgNullableOpplysning(ny.bostedsenhet, this.bostedsenhet),
            behandlendeEnhetEndring = FeilregistrerbarOpplysning.velg(this.behandlendeEnhetEndring, ny.behandlendeEnhetEndring),
            gjelderId = velgNullableOpplysning(ny.gjelderId, this.gjelderId),
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
        private val vedtaksSats: Sats? = null,
        private val skyldner: Part? = null,
        private val refusjonsinfo: Refusjonsinfo? = null,
        private val typeSøknad: TypeSøknad? = null,
        private val kidnummer: Kidnummer? = null,
        private val status: Status? = null,
        private val bostedsenhet: Bostedsenhet? = null,
        private val gjelderId: Personidentifikator? = null,
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
                vedtaksSats = vedtaksSats?.let { Opplysning(linjeSaksbehandlerId, it) },
                skyldner = skyldner?.let { Opplysning(linjeSaksbehandlerId, it) },
                refusjonsinfo = refusjonsinfo?.let { Opplysning(linjeSaksbehandlerId, it) },
                typeSøknad = typeSøknad?.let { Opplysning(linjeSaksbehandlerId, it) },
                henvisning = henvisning?.let { Opplysning(linjeSaksbehandlerId, it) },
                kidnummer = kidnummer?.let { Opplysning(linjeSaksbehandlerId, it) },
                avventingsårsakEndring = avventingsårsakEndring,
                status = Opplysning(linjeSaksbehandlerId, (status ?: Status.Aktiv)),
                bostedsenhet = bostedsenhet?.let { Opplysning(oppdragSaksbehandlerId, it) },
                behandlendeEnhetEndring = behandlendeEnhet,
                gjelderId = gjelderId?.let { Opplysning(oppdragSaksbehandlerId, it) },
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

        fun videreførGjelderId(
            oppdragsdag: OpplysningerIntern?,
            gjelderId: Opplysning<Personidentifikator>?,
        ) = when {
            oppdragsdag == null || gjelderId == null -> oppdragsdag
            // Om det er satt en gjelderId eksplisitt brukes den
            oppdragsdag.gjelderId != null -> oppdragsdag
            // Om det mangler så viderefører vi gjelderId
            else -> oppdragsdag.copy(gjelderId = gjelderId)
        }

        fun videreførUtbetalesTil(
            oppdragsdag: OpplysningerIntern?,
            utbetalesTil: Opplysning<UtbetalesTil>?,
        ) = when {
            oppdragsdag == null || utbetalesTil == null -> oppdragsdag
            // Om det er satt en utbetalesTil eksplisitt brukes den
            oppdragsdag.utbetalesTil != null -> oppdragsdag
            // Om det mangler så viderefører vi utbetalesTIl
            else -> oppdragsdag.copy(utbetalesTil = utbetalesTil)
        }

        fun videreførSkyldner(
            oppdragsdag: OpplysningerIntern?,
            skyldner: Opplysning<Part>?,
        ) = when {
            oppdragsdag == null || skyldner == null -> oppdragsdag
            // Om det er satt en skyldner eksplisitt brukes den
            oppdragsdag.skyldner != null -> oppdragsdag
            // Om det mangler så viderefører vi skyldner
            else -> oppdragsdag.copy(skyldner = skyldner)
        }

        fun videreførRefusjonsinfo(
            oppdragsdag: OpplysningerIntern?,
            refusjonsinfo: Opplysning<Refusjonsinfo>?,
        ) = when {
            oppdragsdag == null || refusjonsinfo == null -> oppdragsdag
            // Om det er satt en refusjonsinfo eksplisitt brukes den
            oppdragsdag.refusjonsinfo != null -> oppdragsdag
            // Om det mangler så viderefører vi refusjonsinfo
            else -> oppdragsdag.copy(refusjonsinfo = refusjonsinfo)
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
