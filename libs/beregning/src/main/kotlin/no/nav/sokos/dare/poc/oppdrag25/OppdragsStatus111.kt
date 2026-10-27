package no.nav.sokos.dare.poc.oppdrag25

import java.time.LocalDate

data class OppdragsStatus111(
    val kodeStatus: TkodeStatus,
    val datoStatusFom: LocalDate,
    val tidspktReg: String,
    val saksbehId: String,
)
