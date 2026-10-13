package no.nav.utbetaling.beregning.oppdrag.xml

import java.time.LocalDate

internal data class UtbetalesTil155(
    val utbetalesTilId: String,
    val datoUtbetalesTilIdFom: LocalDate,
    val tidspktReg: String,
    val saksbehId: String,
)
