package no.nav.utbetaling.beregning.oppdrag.xml

import java.time.LocalDate

internal data class OppdragsStatus111(
    val kodeStatus: TkodeStatus,
    val datoStatusFom: LocalDate,
    val tidspktReg: String,
    val saksbehId: String,
)
