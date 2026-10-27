package no.nav.sokos.dare.poc.oppdrag25

import java.time.LocalDate

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty

data class Oppdrag110(
    val kodeAksjon: String,
    val kodeEndring: String,
    val kodeStatus: TkodeStatus? = null,
    val datoStatusFom: LocalDate? = null,
    val kodeFagomraade: String,
    val fagsystemId: String? = null,
    val oppdragsId: Long? = null,
    val utbetFrekvens: String? = null,
    val datoForfall: LocalDate? = null,
    val stonadId: String? = null,
    val oppdragGjelderId: String,
    val datoOppdragGjelderFom: LocalDate,
    val saksbehId: String,
    @JacksonXmlProperty(localName = "oppdrags-status-111") val oppdragsStatus111: List<OppdragsStatus111> = emptyList(),
    @JacksonXmlProperty(localName = "oppdrag-gjelder-112") val oppdragGjelder112: List<OppdragGjelder112> = emptyList(),
    @JacksonXmlProperty(localName = "bilagstype-113") val bilagstype113: Bilagstype113? = null,
    @JacksonXmlProperty(localName = "avstemming-115") val avstemming115: Avstemming115? = null,
    @JacksonXmlProperty(localName = "ompostering-116") val ompostering116: Ompostering116? = null,
    @JacksonXmlProperty(localName = "avvent-118") val avvent118: List<Avvent118> = emptyList(),
    @JacksonXmlProperty(localName = "oppdrags-enhet-120") val oppdragsEnhet120: List<OppdragsEnhet120> = emptyList(),
    @Deprecated("Steinar sier denne ikke er i bruk") @JacksonXmlProperty(localName = "belops-grense-130") val belopsGrense130: List<BelopsGrense130> = emptyList(),
    @JacksonXmlProperty(localName = "tekst-140") val tekst140: List<Tekst140> = emptyList(),
    @JacksonXmlProperty(localName = "oppdrags-linje-150") val oppdragsLinje150: List<OppdragsLinje150> = emptyList(),
)
