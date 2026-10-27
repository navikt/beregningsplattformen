package no.nav.sokos.dare.poc.oppdrag25

import java.time.LocalDate

data class UtbetalesTil155(
    val utbetalesTilId: String,
    val datoUtbetalesTilIdFom: LocalDate,
    val tidspktReg: String,
    val saksbehId: String,
)
