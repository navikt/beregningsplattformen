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
import no.nav.utbetaling.beregning.oppdrag.xml.TfradragTillegg
import no.nav.utbetaling.beregning.oppdrag.xml.TkodeStatus
import no.nav.utbetaling.beregning.oppdrag.xml.TkodeStatusLinje
import no.nav.utbetaling.beregning.tid.mars

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
    enheter: List<OppdragsEnhet120> = emptyList(),
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
    enheter: List<OppdragsEnhet120> = emptyList(),
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
) = OppdragsLinje150(
    kodeEndringLinje = "NY",
    delytelseId = delytelseId,
    refDelytelseId = refDelytelseId,
    kodeKlassifik = "AAP",
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
)

internal fun avventing(
    fom: LocalDate,
    tom: LocalDate,
    feilreg: String? = null,
    kodeArsak: String? = "AVAV",
    datoOverføres: LocalDate = 1.mars,
) = Avvent118(datoAvventFom = fom, datoAvventTom = tom, kodeArsak = kodeArsak, datoOverfores = datoOverføres, feilreg = feilreg)

internal fun ompostering(
    fom: LocalDate? = null,
    kode: String = "J", // TODO: sjekk om dette er riktig
    feilreg: String? = null,
    registrert: String = "2024-01-15-12.34.56.123456",
    saksbehandlerId: String = "Z999999",
) = Ompostering116(omPostering = kode, datoOmposterFom = fom, feilreg = feilreg, tidspktReg = registrert, saksbehId = saksbehandlerId)
