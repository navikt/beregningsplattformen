package no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner

import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

import no.nav.utbetaling.beregning.oppdrag.intern.FraXmlFormat
import no.nav.utbetaling.beregning.oppdrag.xml.Avvent118
import no.nav.utbetaling.beregning.oppdrag.xml.Bilagstype113
import no.nav.utbetaling.beregning.oppdrag.xml.LinjeEnhet160
import no.nav.utbetaling.beregning.oppdrag.xml.Ompostering116
import no.nav.utbetaling.beregning.oppdrag.xml.Oppdrag110
import no.nav.utbetaling.beregning.oppdrag.xml.OppdragsEnhet120
import no.nav.utbetaling.beregning.oppdrag.xml.OppdragsLinje150
import no.nav.utbetaling.beregning.oppdrag.xml.Refusjonsinfo156
import no.nav.utbetaling.beregning.oppdrag.xml.TfradragTillegg
import no.nav.utbetaling.beregning.oppdrag.xml.TkodeStatus
import no.nav.utbetaling.beregning.oppdrag.xml.TkodeStatusLinje

internal val defaultBostedsenhet = listOf(OppdragsEnhet120(typeEnhet = "BOS", enhet = "9999", datoEnhetFom = LocalDate.EPOCH))

internal fun oppdragstidslinje(
    kodeFagområde: String = "AAP",
    fagsystemId: String = "${UUID.randomUUID()}",
    kodeAksjon: String = "1",
    kodeEndring: String = "NY",
    oppdragGjelderId: String = "11111111111",
    oppdragGjelderFom: LocalDate = LocalDate.EPOCH,
    saksbehandlerId: String = "Z999999",
    linjer: List<OppdragsLinje150> = emptyList(),
    avventinger: List<Avvent118> = emptyList(),
    bilagstype: String? = null,
    forfall: LocalDate? = null,
    ompostering: Ompostering116? = null,
    oppdragsId: Long? = null,
    stønadId: String? = null,
    utbetalingsfrekvens: String? = null,
    kodeStatus: TkodeStatus? = null,
    datoStatusFom: LocalDate? = null,
    enheter: List<OppdragsEnhet120> = defaultBostedsenhet,
) = FraXmlFormat.opprett(
    oppdrag110(
        kodeFagområde = kodeFagområde,
        fagsystemId = fagsystemId,
        kodeAksjon = kodeAksjon,
        kodeEndring = kodeEndring,
        oppdragGjelderId = oppdragGjelderId,
        oppdragGjelderFom = oppdragGjelderFom,
        saksbehandlerId = saksbehandlerId,
        linjer = linjer,
        avventinger = avventinger,
        bilagstype = bilagstype,
        forfall = forfall,
        ompostering = ompostering,
        oppdragsId = oppdragsId,
        stønadId = stønadId,
        utbetalingsfrekvens = utbetalingsfrekvens,
        kodeStatus = kodeStatus,
        datoStatusFom = datoStatusFom,
        enheter = enheter,
    ),
)

internal fun oppdrag110(
    kodeFagområde: String = "AAP",
    fagsystemId: String = "${UUID.randomUUID()}",
    kodeAksjon: String = "1",
    kodeEndring: String = "NY",
    oppdragGjelderId: String = "11111111111",
    oppdragGjelderFom: LocalDate = LocalDate.EPOCH,
    saksbehandlerId: String = "Z999999",
    linjer: List<OppdragsLinje150> = emptyList(),
    avventinger: List<Avvent118> = emptyList(),
    bilagstype: String? = null,
    forfall: LocalDate? = null,
    ompostering: Ompostering116? = null,
    oppdragsId: Long? = null,
    stønadId: String? = null,
    utbetalingsfrekvens: String? = null,
    kodeStatus: TkodeStatus? = null,
    datoStatusFom: LocalDate? = null,
    enheter: List<OppdragsEnhet120> = defaultBostedsenhet,
) = Oppdrag110(
    kodeAksjon = kodeAksjon,
    kodeEndring = kodeEndring,
    kodeFagomraade = kodeFagområde,
    fagsystemId = fagsystemId,
    oppdragGjelderId = oppdragGjelderId,
    datoOppdragGjelderFom = oppdragGjelderFom,
    saksbehId = saksbehandlerId,
    oppdragsLinje150 = linjer,
    avvent118 = avventinger,
    bilagstype113 = bilagstype?.let { Bilagstype113(it) },
    datoForfall = forfall,
    ompostering116 = ompostering,
    oppdragsId = oppdragsId,
    stonadId = stønadId,
    utbetFrekvens = utbetalingsfrekvens,
    kodeStatus = kodeStatus,
    datoStatusFom = datoStatusFom,
    oppdragsEnhet120 = enheter,
)

internal fun linje(
    delytelseId: String = "${UUID.randomUUID()}",
    refDelytelseId: String? = null,
    fom: LocalDate,
    tom: LocalDate?,
    sats: Long? = null,
    henvisning: String? = null,
    kid: String? = null,
    datoKidFom: LocalDate? = null,
    kodeStatusLinje150: TkodeStatusLinje? = null,
    datoStatusFom: LocalDate? = null,
    enheter: List<LinjeEnhet160> = emptyList(),
    utbetalesTilId: String? = null,
    datoUtbetalesTilIdFom: LocalDate? = null,
    vedtakId: String? = null,
    brukKjøreplan: String? = null,
    kodeKlassifik: String = "AAPUAA",
    datoKlassifikFom: LocalDate? = null,
    skyldnerId: String? = null,
    datoSkyldnerFom: LocalDate? = null,
    typeSoknad: String? = null,
    refunderesId: String? = null,
    refusjonMaksDato: LocalDate? = null,
    refusjonDatoFom: LocalDate? = null,
) = OppdragsLinje150(
    kodeEndringLinje = "NY",
    delytelseId = delytelseId,
    refDelytelseId = refDelytelseId,
    kodeKlassifik = kodeKlassifik,
    datoKlassifikFom = datoKlassifikFom,
    datoVedtakFom = fom,
    datoVedtakTom = tom,
    henvisning = henvisning,
    sats = sats?.let { BigDecimal.valueOf(it) } ?: BigDecimal.TEN,
    fradragTillegg = TfradragTillegg.T,
    typeSats = "DAG",
    saksbehId = "Z999999",
    kid = kid,
    datoKidFom = datoKidFom,
    kodeStatusLinje = kodeStatusLinje150,
    datoStatusFom = datoStatusFom,
    linjeEnhet160 = enheter,
    utbetalesTilId = utbetalesTilId,
    datoUtbetalesTilIdFom = datoUtbetalesTilIdFom,
    vedtakId = vedtakId,
    brukKjoreplan = brukKjøreplan,
    skyldnerId = skyldnerId,
    datoSkyldnerFom = datoSkyldnerFom,
    typeSoknad = typeSoknad,
    refusjonsinfo156 =
        if (refunderesId != null || refusjonMaksDato != null || refusjonDatoFom != null) {
            Refusjonsinfo156(
                maksDato = refusjonMaksDato,
                refunderesId = refunderesId,
                datoFom = refusjonDatoFom ?: fom,
            )
        } else {
            null
        },
)
