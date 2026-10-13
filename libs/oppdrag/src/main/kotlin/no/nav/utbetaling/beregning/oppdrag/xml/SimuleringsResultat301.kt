package no.nav.utbetaling.beregning.oppdrag.xml

import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty

internal data class SimuleringsResultat301(
    @JacksonXmlProperty(localName = "simulerings-resultat-linje-301") val simuleringsResultatLinje301: List<no.nav.utbetaling.beregning.oppdrag.xml.SimuleringsResultatLinje301> = emptyList(),
    val simuleringsResultat: List<no.nav.utbetaling.beregning.oppdrag.xml.SimuleringsResultat> = emptyList(),
)
