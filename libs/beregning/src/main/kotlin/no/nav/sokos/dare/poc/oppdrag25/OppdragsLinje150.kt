package no.nav.sokos.dare.poc.oppdrag25

import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty

data class OppdragsLinje150(
    val kodeEndringLinje: String,
    val kodeStatusLinje: TkodeStatusLinje? = null,
    val datoStatusFom: LocalDate? = null,
    val vedtakId: String? = null,
    val delytelseId: String? = null,
    val linjeId: BigInteger? = null,
    val kodeKlassifik: String,
    val datoKlassifikFom: LocalDate? = null,
    val datoVedtakFom: LocalDate,
    val datoVedtakTom: LocalDate? = null,
    val sats: BigDecimal,
    val fradragTillegg: TfradragTillegg,
    val typeSats: String,
    val skyldnerId: String? = null,
    val datoSkyldnerFom: LocalDate? = null,
    /**
     * Feltene beholdes for kompatibilitet med Oppdrag 2.5-kontrakten.
     * De brukes eller lagres ikke av Dare.
     */
    @Deprecated("Steinar sier denne ikke er i bruk") val kravhaverId: String? = null,
    @Deprecated("Steinar sier denne ikke er i bruk") val datoKravhaverFom: LocalDate? = null,
    val kid: String? = null,
    val datoKidFom: LocalDate? = null,
    val brukKjoreplan: String? = null,
    val saksbehId: String,
    val utbetalesTilId: String? = null,
    val datoUtbetalesTilIdFom: LocalDate? = null,
    @Deprecated("Steinar sier denne ikke er i bruk") val kodeArbeidsgiver: TkodeArbeidsgiver? = null,
    val henvisning: String? = null,
    val typeSoknad: String? = null,
    val refFagsystemId: String? = null,
    val refOppdragsId: Long? = null,
    val refDelytelseId: String? = null,
    val refLinjeId: BigInteger? = null,
    @JacksonXmlProperty(localName = "linje-status-151") val linjeStatus151: List<LinjeStatus151> = emptyList(),
    @JacksonXmlProperty(localName = "klassifikasjon-152") val klassifikasjon152: List<Klassifikasjon152> = emptyList(),
    @JacksonXmlProperty(localName = "skyldner-153") val skyldner153: List<Skyldner153> = emptyList(),
    @JacksonXmlProperty(localName = "kid-154") val kid154: List<Kid154> = emptyList(),
    @JacksonXmlProperty(localName = "utbetales-til-155") val utbetalesTil155: List<UtbetalesTil155> = emptyList(),
    @JacksonXmlProperty(localName = "refusjonsinfo-156") val refusjonsinfo156: Refusjonsinfo156? = null,
    @JacksonXmlProperty(localName = "vedtakssats-157") val vedtakssats157: Vedtakssats157? = null,
    @JacksonXmlProperty(localName = "linje-tekst-158") val linjeTekst158: List<LinjeTekst158> = emptyList(),
    @JacksonXmlProperty(localName = "linje-enhet-160") val linjeEnhet160: List<LinjeEnhet160> = emptyList(),
    @JacksonXmlProperty(localName = "grad-170") val grad170: List<Grad170> = emptyList(),
    @JacksonXmlProperty(localName = "attestant-180") val attestant180: List<Attestant180> = emptyList(),
    @Deprecated("Steinar sier denne ikke er i bruk") @JacksonXmlProperty(localName = "valuta-190") val valuta190: List<Valuta190> = emptyList(),
)
