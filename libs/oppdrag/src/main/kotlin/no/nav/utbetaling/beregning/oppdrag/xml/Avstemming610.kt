package no.nav.utbetaling.beregning.oppdrag.xml

import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty

internal data class Avstemming610(
    val kodeAksjon: String,
    val kodeFagomraade: String? = null,
    val avstemmingId: String? = null,
    @JacksonXmlProperty(localName = "oppdrag-110") val oppdrag110: no.nav.utbetaling.beregning.oppdrag.xml.Oppdrag110? = null,
)
