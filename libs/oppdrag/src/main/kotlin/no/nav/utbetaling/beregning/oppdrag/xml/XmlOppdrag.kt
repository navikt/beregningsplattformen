package no.nav.utbetaling.beregning.oppdrag.xml

import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty

internal data class XmlOppdrag(
    val mmel: no.nav.utbetaling.beregning.oppdrag.xml.Mmel? = null,
    @JacksonXmlProperty(localName = "oppdrag-110") val oppdrag110: no.nav.utbetaling.beregning.oppdrag.xml.Oppdrag110? = null,
    @JacksonXmlProperty(localName = "hent-oppdrag-195") val hentOppdrag195: no.nav.utbetaling.beregning.oppdrag.xml.HentOppdrag195? = null,
    @JacksonXmlProperty(localName = "simulerings-periode-300") val simuleringsPeriode300: no.nav.utbetaling.beregning.oppdrag.xml.SimuleringsPeriode300? = null,
    @JacksonXmlProperty(localName = "simulerings-resultat-301") val simuleringsResultat301: no.nav.utbetaling.beregning.oppdrag.xml.SimuleringsResultat301? = null,
    @JacksonXmlProperty(localName = "avstemming-610") val avstemming610: no.nav.utbetaling.beregning.oppdrag.xml.Avstemming610? = null,
)
