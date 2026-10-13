package no.nav.utbetaling.beregning.oppdrag.xml

import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate

import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty

internal data class OppdragsLinje150(
    val kodeEndringLinje: String,
    val kodeStatusLinje: no.nav.utbetaling.beregning.oppdrag.xml.TkodeStatusLinje? = null,
    val datoStatusFom: LocalDate? = null,
    val vedtakId: String? = null,
    val delytelseId: String? = null,
    val linjeId: BigInteger? = null,
    val kodeKlassifik: String,
    val datoKlassifikFom: LocalDate? = null,
    val datoVedtakFom: LocalDate,
    val datoVedtakTom: LocalDate? = null,
    val sats: BigDecimal,
    val fradragTillegg: no.nav.utbetaling.beregning.oppdrag.xml.TfradragTillegg,
    val typeSats: String,
    val skyldnerId: String? = null,
    val datoSkyldnerFom: LocalDate? = null,
    val kravhaverId: String? = null,
    val datoKravhaverFom: LocalDate? = null,
    val kid: String? = null,
    val datoKidFom: LocalDate? = null,
    val brukKjoreplan: String? = null,
    val saksbehId: String,
    val utbetalesTilId: String? = null,
    val datoUtbetalesTilIdFom: LocalDate? = null,
    val kodeArbeidsgiver: no.nav.utbetaling.beregning.oppdrag.xml.TkodeArbeidsgiver? = null,
    val henvisning: String? = null,
    val typeSoknad: String? = null,
    val refFagsystemId: String? = null,
    val refOppdragsId: Long? = null,
    val refDelytelseId: String? = null,
    val refLinjeId: BigInteger? = null,
    @JacksonXmlProperty(localName = "linje-status-151") val linjeStatus151: List<no.nav.utbetaling.beregning.oppdrag.xml.LinjeStatus151> = emptyList(),
    @JacksonXmlProperty(localName = "klassifikasjon-152") val klassifikasjon152: List<no.nav.utbetaling.beregning.oppdrag.xml.Klassifikasjon152> = emptyList(),
    @JacksonXmlProperty(localName = "skyldner-153") val skyldner153: List<no.nav.utbetaling.beregning.oppdrag.xml.Skyldner153> = emptyList(),
    @JacksonXmlProperty(localName = "kid-154") val kid154: List<no.nav.utbetaling.beregning.oppdrag.xml.Kid154> = emptyList(),
    @JacksonXmlProperty(localName = "utbetales-til-155") val utbetalesTil155: List<no.nav.utbetaling.beregning.oppdrag.xml.UtbetalesTil155> = emptyList(),
    @JacksonXmlProperty(localName = "refusjonsinfo-156") val refusjonsinfo156: no.nav.utbetaling.beregning.oppdrag.xml.Refusjonsinfo156? = null,
    @JacksonXmlProperty(localName = "vedtakssats-157") val vedtakssats157: no.nav.utbetaling.beregning.oppdrag.xml.Vedtakssats157? = null,
    @JacksonXmlProperty(localName = "linje-tekst-158") val linjeTekst158: List<no.nav.utbetaling.beregning.oppdrag.xml.LinjeTekst158> = emptyList(),
    @JacksonXmlProperty(localName = "linje-enhet-160") val linjeEnhet160: List<no.nav.utbetaling.beregning.oppdrag.xml.LinjeEnhet160> = emptyList(),
    @JacksonXmlProperty(localName = "grad-170") val grad170: List<no.nav.utbetaling.beregning.oppdrag.xml.Grad170> = emptyList(),
    @JacksonXmlProperty(localName = "attestant-180") val attestant180: List<no.nav.utbetaling.beregning.oppdrag.xml.Attestant180> = emptyList(),
    @JacksonXmlProperty(localName = "valuta-190") val valuta190: List<no.nav.utbetaling.beregning.oppdrag.xml.Valuta190> = emptyList(),
)
