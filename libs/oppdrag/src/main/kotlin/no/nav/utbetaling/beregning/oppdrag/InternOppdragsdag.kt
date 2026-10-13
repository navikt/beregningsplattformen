package no.nav.utbetaling.beregning.oppdrag

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDate.EPOCH
import no.nav.utbetaling.beregning.oppdrag.Endring.Companion.gjeldende
import no.nav.utbetaling.beregning.oppdrag.Endring.Companion.ingenEndringer
import no.nav.utbetaling.beregning.oppdrag.xml.Oppdrag110
import no.nav.utbetaling.beregning.tid.Periode
import no.nav.utbetaling.beregning.tid.Periode.LukketPeriode.Companion.til
import no.nav.utbetaling.beregning.tid.Periode.ÅpenPeriode
import no.nav.utbetaling.beregning.tid.Tidslinje

internal fun Kjede.hørerTil(other: Kjede) = fagområde == other.fagområde && fagsystemId == other.fagsystemId && delytelseIder.intersect(other.delytelseIder).isNotEmpty()

private operator fun Kjede.plus(other: Kjede) = copy(delytelseIder = delytelseIder + other.delytelseIder)

internal data class Kjedbar<T>(
    val kjede: Kjede,
    val verdi: T,
) {
    companion object {
        fun <T> List<Kjedbar<T>>.grupperPåKjede(): Map<Kjede, List<T>> {
            val kjeder = mutableMapOf<Kjede, List<T>>()
            forEach { kjedbar ->
                val treff = kjeder.entries.filter { (kjede, _) -> kjede.hørerTil(kjedbar.kjede) }
                if (treff.isEmpty()) {
                    kjeder[kjedbar.kjede] = listOf(kjedbar.verdi)
                    return@forEach
                }
                val treffKjeder = treff.map { it.key }
                val samletKjede = treffKjeder.reduce { sammenslått, kjede -> sammenslått + kjede } + kjedbar.kjede
                val samletVerdi = treff.flatMap { it.value } + kjedbar.verdi
                treffKjeder.forEach { kjeder.remove(it) }
                kjeder[samletKjede] = samletVerdi
            }
            return kjeder.toMap()
        }
    }
}

internal data class Statusendring(
    val status: Opplysning<Status>,
    val unntatt: Set<DelytelseId>,
)

internal data class Kjedeendringer(
    val sluttdato: LocalDate?,
    val kjedestatus: Tidslinje<Statusendring>,
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
    fun medNyeVedierFra(ny: Oppdragsverdier) =
        Oppdragsverdier(
            oppdragId = velgNullableOpplysning(ny.oppdragId, oppdragId),
            stønadId = velgNullableOpplysning(ny.stønadId, stønadId),
            utbetalingsfrekvens = velgNullableOpplysning(ny.utbetalingsfrekvens, utbetalingsfrekvens),
            bilagstype = velgNullableOpplysning(ny.bilagstype, bilagstype),
            forfall = velgNullableOpplysning(ny.forfall, forfall),
        )
}

internal data class InternOppdragId(
    val fagområde: Fagområde,
    val fagsystemId: FagsystemId,
)

internal val Kjede.internOppdragId get() = InternOppdragId(fagområde, fagsystemId)

internal data class InternOppdragsdag(
    val delytelseId: DelytelseId,
    val refDelytelseId: DelytelseId?,
    val sats: Opplysning<BigDecimal>,
    val henvisning: Opplysning<Henvisning>?,
    val kidnummer: Opplysning<Kidnummer>?,
    val status: Opplysning<Status>,
    private val avventingsårsakEndring: Endring<Avventingsårsak>?,
    private val omposteringEndring: Endring<Ompostering>?,
) {
    val avventingsårsak = avventingsårsakEndring?.gjeldende()
    val ompostering = omposteringEndring?.gjeldende()

    private fun medNyeVerdierFra(ny: InternOppdragsdag) =
        InternOppdragsdag(
            delytelseId = ny.delytelseId,
            refDelytelseId = ny.refDelytelseId,
            sats = velgOpplysning(ny.sats, sats),
            henvisning = velgNullableOpplysning(ny.henvisning, henvisning),
            kidnummer = velgNullableOpplysning(ny.kidnummer, kidnummer),
            avventingsårsakEndring = Endring.velg(this.avventingsårsakEndring, ny.avventingsårsakEndring),
            omposteringEndring = Endring.velg(this.omposteringEndring, ny.omposteringEndring),
            status = velgOpplysning(ny.status, this.status),
        )

    data class Builder(
        private val delytelseId: DelytelseId,
        private val linjeSaksbehandlerId: SaksbehandlerId,
        private val refDelytelseId: DelytelseId?,
        private val henvisning: Henvisning?,
        private val sats: BigDecimal,
        private val kidnummer: Kidnummer?,
        private val avventingsårsakEndring: Endring<Avventingsårsak>? = null,
        private val omposteringEndring: Endring<Ompostering>? = null,
        private val status: Status? = null,
    ) {
        fun medStatus(statusendring: Statusendring?): Builder =
            apply {
                if (statusendring == null) return this
                if (delytelseId in statusendring.unntatt) return this
                return copy(status = statusendring.status.opplysning)
            }

        fun build() =
            InternOppdragsdag(
                delytelseId = delytelseId,
                refDelytelseId = refDelytelseId,
                sats = Opplysning(linjeSaksbehandlerId, sats),
                henvisning = henvisning?.let { Opplysning(linjeSaksbehandlerId, it) },
                kidnummer = kidnummer?.let { Opplysning(linjeSaksbehandlerId, it) },
                avventingsårsakEndring = avventingsårsakEndring,
                omposteringEndring = omposteringEndring,
                status = Opplysning(linjeSaksbehandlerId, (status ?: Status.Aktiv)),
            )
    }

    companion object {
        fun velg(
            eksisterende: InternOppdragsdag?,
            ny: InternOppdragsdag?,
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
            oppdragsdag: InternOppdragsdag?,
            status: Opplysning<Status>?,
            unntatt: Set<DelytelseId> = emptySet(),
        ) = when {
            status == null || oppdragsdag == null -> oppdragsdag
            oppdragsdag.delytelseId in unntatt -> oppdragsdag
            else -> oppdragsdag.copy(status = velgOpplysning(status, oppdragsdag.status))
        }

        fun velgStatus(
            oppdragsdag: InternOppdragsdag?,
            statusendring: Statusendring?,
        ) = velgStatus(oppdragsdag, statusendring?.status, statusendring?.unntatt ?: emptySet())
    }
}

internal fun <T> velgNullableOpplysning(
    ny: Opplysning<T>?,
    eksisterende: Opplysning<T>?,
) = when {
    ny == null && eksisterende == null -> null
    ny != null && eksisterende != null -> velgOpplysning(ny, eksisterende)
    else -> ny ?: eksisterende
}

private fun <T> velgOpplysning(
    ny: Opplysning<T>,
    gammel: Opplysning<T>,
) = ny.takeUnless { it.opplysning == gammel.opplysning } ?: gammel

internal sealed interface Endring<T> {
    val opplysning: Opplysning<T>

    data class Satt<T>(
        override val opplysning: Opplysning<T>,
    ) : Endring<T>

    data class Opphevet<T>(
        override val opplysning: Opplysning<T>,
    ) : Endring<T>

    companion object {
        private val FEILREGISTRERT_VERDIER = setOf("j", "ja", "y", "yes")

        private fun String?.erFeilregistrert() = mapVerdi { it.lowercase() in FEILREGISTRERT_VERDIER } == true

        fun <T> Endring<T>?.gjeldende() = (this as? Satt)?.opplysning

        fun <T> ingenEndringer() = Tidslinje.Builder<Endring<T>>().build()

        fun <T> velg(
            eksisterende: Endring<T>?,
            ny: Endring<T>?,
        ) = when (ny) {
            is Opphevet -> null
            is Satt -> ny
            else -> eksisterende
        }

        fun <T> fra(
            feilreg: String?,
            opplysning: Opplysning<T>,
        ): Endring<T> =
            when (feilreg.erFeilregistrert()) {
                true -> Opphevet(opplysning)
                false -> Satt(opplysning)
            }
    }
}

// Lager en tidslinje av endringer på Avventingsårsaker
internal fun avventingerFra(oppdrag110: Oppdrag110): Tidslinje<Endring<Avventingsårsak>> {
    val builder = Tidslinje.Builder<Endring<Avventingsårsak>>()
    val saksbehandlerId = oppdrag110.saksbehId.mapVerdi { SaksbehandlerId(it) } ?: error("Mangler saksbehandler på oppdragNivå")
    oppdrag110.avvent118.forEach { avvent118 ->
        val fom = checkNotNull(avvent118.datoAvventFom) { "datoAvventFom må være satt på avventing" }
        val tom = checkNotNull(avvent118.datoAvventTom) { "datoAvventTom må være satt på avventing" }
        val årsak =
            when (avvent118.kodeArsak?.uppercase()) {
                "AVAV" -> Avventingsårsak.Avregning(til = checkNotNull(avvent118.datoOverfores) { "datoOverfores må være satt for avventing pga. avregning" })
                "AVRK" -> Avventingsårsak.Refusjonskrav(avvent118.datoOverfores)
                else -> error("Kjenner ikke til avventingsårsak ${avvent118.kodeArsak}")
            }
        builder.periode(fom til tom, Endring.fra(avvent118.feilreg, Opplysning(saksbehandlerId, årsak)))
    }
    return builder.build()
}

// Lager en tidslinje av endringer på Ompostering
internal fun omposteringerFra(oppdrag110: Oppdrag110): Tidslinje<Endring<Ompostering>> {
    val ompostering116 = oppdrag110.ompostering116 ?: return ingenEndringer()
    val ompostering =
        Ompostering(
            kode = Omposteringskode(ompostering116.omPostering),
            registrert = Tidspunkt.fra(ompostering116.tidspktReg),
        )
    val saksbehandlerId = ompostering116.saksbehId.mapVerdi { SaksbehandlerId(it) } ?: error("Mangler saksbehId på ompostering")
    return Tidslinje
        .Builder<Endring<Ompostering>>()
        .periode(ÅpenPeriode(ompostering116.datoOmposterFom ?: EPOCH), Endring.fra(ompostering116.feilreg, Opplysning(saksbehandlerId, ompostering)))
        .build()
}

internal fun <T> String?.mapVerdi(block: (verdi: String) -> T) =
    when {
        isNullOrBlank() -> null
        else -> block(trim())
    }
