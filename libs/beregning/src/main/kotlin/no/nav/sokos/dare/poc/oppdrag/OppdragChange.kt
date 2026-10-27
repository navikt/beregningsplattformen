package no.nav.sokos.dare.poc.oppdrag

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

import no.nav.sokos.dare.poc.aktoer.AktoerId
import no.nav.sokos.dare.poc.oppdrag25.Oppdrag110
import no.nav.sokos.dare.poc.oppdrag25.OppdragsLinje150
import no.nav.sokos.dare.poc.oppdrag25.TkodeStatusLinje
import no.nav.sokos.dare.poc.trekk.FagomraadeKode

data class OppdragChange(
    val fagsystemId: String,
    val fagomraade: String,
    val gjelderOffNr: String,
    val datoForfall: LocalDate?,
    val bilagsType: String,
    val bruker: String,
    val registrert: LocalDateTime,
    val linjer: List<OppdragslinjeGrunnlag>,
    val enheter: List<OppdragsenhetGrunnlag>,
    val endringstype: OppdragEndringskode,
) {
    fun toOppdrag(
        aktoerId: AktoerId,
        oppdragId: OppdragId? = null,
    ): Oppdrag =
        Oppdrag(
            id = oppdragId,
            fagsystemId = fagsystemId,
            fagomraade =
                try {
                    FagomraadeKode.fromValue(fagomraade)
                } catch (_: NoSuchElementException) {
                    throw IllegalArgumentException("Ukjent kodeFagomraade: $fagomraade")
                },
            datoForfall = datoForfall,
            bilagsType = bilagsType,
            bruker = bruker,
            tidspktReg = registrert,
            gjelderId = aktoerId,
        )

    companion object {
        fun from(source: Oppdrag110): OppdragChange {
            val fagsystemId =
                requireNotNull(source.fagsystemId?.takeIf(String::isNotBlank)) {
                    "Oppdrag mangler fagsystemId"
                }
            val fagomraade = source.kodeFagomraade
            val registrert = LocalDateTime.now()
            val linjer = source.oppdragsLinje150.map(OppdragslinjeGrunnlag::from)
            val delytelseIder = linjer.map(OppdragslinjeGrunnlag::delytelseId)
            require(delytelseIder.distinct().size == delytelseIder.size) {
                "Delytelse-ID må være unik i samme oppdragsendring"
            }
            val enheter =
                source.oppdragsEnhet120.map {
                    OppdragsenhetGrunnlag(
                        enhet = it.enhet.orEmpty(),
                        typeEnhet = it.typeEnhet,
                        datoFom = it.datoEnhetFom,
                        bruker = source.saksbehId,
                        registrert = registrert,
                    )
                }
            val endringstype = OppdragEndringskode.fromValue(source.kodeEndring)

            return OppdragChange(
                fagsystemId = fagsystemId,
                fagomraade = fagomraade,
                gjelderOffNr = source.oppdragGjelderId,
                datoForfall = source.datoForfall,
                bilagsType = source.bilagstype113?.bilagsType.orEmpty(),
                bruker = source.saksbehId,
                registrert = registrert,
                linjer = linjer,
                enheter = enheter,
                endringstype = endringstype,
            )
        }
    }
}

data class OppdragsenhetGrunnlag(
    val enhet: String,
    val typeEnhet: String,
    val datoFom: LocalDate,
    val bruker: String,
    val registrert: LocalDateTime,
) {
    fun toOppdragsenhet(oppdragId: OppdragId): Oppdragsenhet =
        Oppdragsenhet(
            oppdragsId = oppdragId,
            enhet = enhet,
            typeEnhet = typeEnhet,
            datoFom = datoFom,
            bruker = bruker,
            tidspktReg = registrert,
        )
}

data class OppdragslinjeGrunnlag(
    val endringskode: OppdragslinjeEndringskode,
    val linjestatus: TkodeStatusLinje? = null,
    val datoStatusFom: LocalDate? = null,
    val delytelseId: String,
    val sats: BigDecimal,
    val typeSats: String,
    val brukKjoreplan: String?,
    val datoVedtakFom: LocalDate,
    val datoVedtakTom: LocalDate?,
    val vedtakId: String,
    val henvisning: String?,
    val utbetalesTilOffNr: String,
    val kodeKlasse: YtelseKlasseKodeRef,
    val skyldnerOffNr: String?,
    val kid: String?,
    val typeSoknad: String?,
    val maksDato: LocalDate?,
    val refunderesId: String?,
    val bruker: String,
    val refDelytelseId: String?,
    val vedtakssats: BigDecimal?,
) {
    companion object {
        fun from(source: OppdragsLinje150): OppdragslinjeGrunnlag {
            val delytelseId =
                requireNotNull(source.delytelseId?.takeIf(String::isNotBlank)) {
                    "Oppdragslinje mangler delytelseId"
                }
            require(source.datoVedtakTom == null || !source.datoVedtakTom.isBefore(source.datoVedtakFom)) {
                "Oppdragslinje $delytelseId har datoVedtakTom før datoVedtakFom"
            }

            return OppdragslinjeGrunnlag(
                endringskode = OppdragslinjeEndringskode.fromValue(source.kodeEndringLinje),
                delytelseId = delytelseId,
                sats = source.sats,
                typeSats = source.typeSats,
                brukKjoreplan = source.brukKjoreplan,
                datoVedtakFom = source.datoVedtakFom,
                datoVedtakTom = source.datoVedtakTom,
                vedtakId = source.vedtakId.orEmpty(),
                henvisning = source.henvisning,
                utbetalesTilOffNr = requireNotNull(source.utbetalesTilId) { "utbetalesTilId must not be null" },
                kodeKlasse = YtelseKlasseKodeRef(source.kodeKlassifik),
                skyldnerOffNr = source.skyldnerId,
                kid = source.kid,
                typeSoknad = source.typeSoknad,
                maksDato = source.refusjonsinfo156?.maksDato,
                refunderesId = source.refusjonsinfo156?.refunderesId,
                bruker = source.saksbehId,
                refDelytelseId = source.refDelytelseId,
                vedtakssats = source.vedtakssats157?.vedtakssats,
            )
        }
    }
}

enum class OppdragslinjeEndringskode {
    NY,
    ENDR,
    ;

    companion object {
        fun fromValue(value: String): OppdragslinjeEndringskode =
            entries.firstOrNull { it.name == value }
                ?: throw IllegalArgumentException("Ukjent kodeEndringLinje: $value")
    }
}

data class OppdragslinjeEndring(
    val endringskode: OppdragslinjeEndringskode,
    val linjestatus: TkodeStatusLinje? = null,
    val datoStatusFom: LocalDate? = null,
    val linje: Oppdragslinje,
    val refDelytelseId: String?,
)

val inaktiveLinjeStatuser: Set<TkodeStatusLinje> =
    setOf(
        TkodeStatusLinje.OPPH,
        TkodeStatusLinje.HVIL,
        TkodeStatusLinje.SPER,
    )
