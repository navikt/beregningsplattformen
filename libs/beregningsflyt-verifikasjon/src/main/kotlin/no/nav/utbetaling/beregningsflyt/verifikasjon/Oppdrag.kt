package no.nav.utbetaling.beregningsflyt.verifikasjon

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class Oppdrag(
    val kodeAksjon: String,
    val kodeEndring: String,
    val kodeStatus: String? = null,
    val datoStatusFom: String? = null,
    val kodeFagomraade: String,
    val fagsystemId: String,
    val oppdragsId: String? = null,
    val utbetFrekvens: String? = null,
    val datoForfall: String? = null,
    val stonadId: String? = null,
    val oppdragGjelderId: String,
    val datoOppdragGjelderFom: String,
    val saksbehId: String,
    val bilagstype113: String? = null,
    val avstemming115: Avstemming115? = null,
    val ompostering116: String? = null,
    val tilknyttetMikrofrontend: String? = null,
    val oppdragsStatus111: List<JsonElement> = emptyList(),
    val oppdragGjelder112: List<JsonElement> = emptyList(),
    val avvent118: List<Avvent118> = emptyList(),
    val oppdragsEnhet120: List<OppdragsEnhet120> = emptyList(),
    val belopsGrense130: List<JsonElement> = emptyList(),
    val tekst140: List<JsonElement> = emptyList(),
    val oppdragsLinje150: List<OppdragsLinje150> = emptyList(),
)

@Serializable
data class Avstemming115(
    val kodeKomponent: String,
    val nokkelAvstemming: String,
    val tidspktMelding: String,
)

@Serializable
data class OppdragsEnhet120(
    val typeEnhet: String,
    val enhet: String,
    val datoEnhetFom: String,
)

@Serializable
data class Avvent118(
    val datoAvventFom: String? = null,
    val datoAvventTom: String? = null,
    val datoOverfores: String? = null,
    val kodeArsak: String? = null,
)

@Serializable
data class OppdragsLinje150(
    val kodeEndringLinje: String,
    val kodeStatusLinje: String? = null,
    val datoStatusFom: String? = null,
    val vedtakId: String? = null,
    val delytelseId: String,
    val linjeId: String? = null,
    val kodeKlassifik: String,
    val datoKlassifikFom: String? = null,
    val datoVedtakFom: String,
    val datoVedtakTom: String? = null,
    val sats: Int,
    val fradragTillegg: String,
    val typeSats: String,
    val skyldnerId: String? = null,
    val datoSkyldnerFom: String? = null,
    val kravhaverId: String? = null,
    val datoKravhaverFom: String? = null,
    val kid: String? = null,
    val datoKidFom: String? = null,
    val brukKjoreplan: String? = null,
    val saksbehId: String,
    val utbetalesTilId: String? = null,
    val datoUtbetalesTilIdFom: String? = null,
    val kodeArbeidsgiver: String? = null,
    val henvisning: String? = null,
    val typeSoknad: String? = null,
    val refFagsystemId: String? = null,
    val refOppdragsId: String? = null,
    val refDelytelseId: String? = null,
    val refLinjeId: String? = null,
    val datoRefFagsystemIdFom: String? = null,
    val refusjonsinfo156: String? = null,
    val utbetalingsgrad150: String? = null,
    val vedtakssats157: Vedtakssats157? = null,
    val linjeStatus151: List<JsonElement> = emptyList(),
    val klassifikasjon152: List<JsonElement> = emptyList(),
    val skyldner153: List<JsonElement> = emptyList(),
    val kid154: List<JsonElement> = emptyList(),
    val utbetalesTil155: List<JsonElement> = emptyList(),
    val linjeTekst158: List<JsonElement> = emptyList(),
    val linjeEnhet160: List<JsonElement> = emptyList(),
    val tekst160: List<JsonElement> = emptyList(),
    val valuta160: List<JsonElement> = emptyList(),
    val grad170: List<JsonElement> = emptyList(),
    val attestant180: List<Attestant180> = emptyList(),
    val valuta190: List<JsonElement> = emptyList(),
)

@Serializable
data class Vedtakssats157(
    val vedtakssats: Int,
)

@Serializable
data class Attestant180(
    val attestantId: String,
    val datoUgyldigFom: String? = null,
)
