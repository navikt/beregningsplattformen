package no.nav.utbetaling.beregning.oppdrag.xml

import java.time.LocalDate

import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty

internal data class Oppdrag110(
    val kodeAksjon: String,
    val kodeEndring: String,
    val kodeStatus: no.nav.utbetaling.beregning.oppdrag.xml.TkodeStatus? = null,
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
    @JacksonXmlProperty(localName = "oppdrags-status-111") val oppdragsStatus111: List<no.nav.utbetaling.beregning.oppdrag.xml.OppdragsStatus111> = emptyList(),
    @JacksonXmlProperty(localName = "oppdrag-gjelder-112") val oppdragGjelder112: List<no.nav.utbetaling.beregning.oppdrag.xml.OppdragGjelder112> = emptyList(),
    @JacksonXmlProperty(localName = "bilagstype-113") val bilagstype113: no.nav.utbetaling.beregning.oppdrag.xml.Bilagstype113? = null,
    @JacksonXmlProperty(localName = "avstemming-115") val avstemming115: no.nav.utbetaling.beregning.oppdrag.xml.Avstemming115? = null,
    @JacksonXmlProperty(localName = "ompostering-116") val ompostering116: no.nav.utbetaling.beregning.oppdrag.xml.Ompostering116? = null,
    @JacksonXmlProperty(localName = "avvent-118") val avvent118: List<no.nav.utbetaling.beregning.oppdrag.xml.Avvent118> = emptyList(),
    @JacksonXmlProperty(localName = "oppdrags-enhet-120") val oppdragsEnhet120: List<no.nav.utbetaling.beregning.oppdrag.xml.OppdragsEnhet120> = emptyList(),
    @JacksonXmlProperty(localName = "belops-grense-130") val belopsGrense130: List<no.nav.utbetaling.beregning.oppdrag.xml.BelopsGrense130> = emptyList(),
    @JacksonXmlProperty(localName = "tekst-140") val tekst140: List<no.nav.utbetaling.beregning.oppdrag.xml.Tekst140> = emptyList(),
    @JacksonXmlProperty(localName = "oppdrags-linje-150") val oppdragsLinje150: List<no.nav.utbetaling.beregning.oppdrag.xml.OppdragsLinje150> = emptyList(),
)
