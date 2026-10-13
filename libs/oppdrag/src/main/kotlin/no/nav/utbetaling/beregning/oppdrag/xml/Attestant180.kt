package no.nav.utbetaling.beregning.oppdrag.xml

import java.time.LocalDate

internal data class Attestant180(
    val attestantId: String,
    val datoUgyldigFom: LocalDate? = null,
)
