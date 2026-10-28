package no.nav.beregning.taskv2.domene

import java.util.Properties

/** Wrapper for metadata-Properties (kun callId i denne konteksten). Portet fra prosessering. */
class PropertiesWrapper(
    val properties: Properties = Properties(),
)
