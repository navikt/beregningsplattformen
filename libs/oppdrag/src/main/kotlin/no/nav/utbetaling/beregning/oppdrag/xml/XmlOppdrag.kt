package no.nav.utbetaling.beregning.oppdrag.xml

import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty

internal data class XmlOppdrag(
    val mmel: Mmel? = null,
    @JacksonXmlProperty(localName = "oppdrag-110") val oppdrag110: Oppdrag110? = null,
    @JacksonXmlProperty(localName = "hent-oppdrag-195") val hentOppdrag195: HentOppdrag195? = null,
    @JacksonXmlProperty(localName = "simulerings-periode-300") val simuleringsPeriode300: SimuleringsPeriode300? = null,
    @JacksonXmlProperty(localName = "simulerings-resultat-301") val simuleringsResultat301: SimuleringsResultat301? = null,
    @JacksonXmlProperty(localName = "avstemming-610") val avstemming610: Avstemming610? = null,
)
