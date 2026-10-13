package no.nav.utbetaling.beregning.oppdrag.xml

import java.time.LocalDate

internal data class OppdragGjelder112(
    val oppdragGjelderId: String,
    val datoOppdragGjelderFom: LocalDate,
    val tidspktReg: String,
    val saksbehId: String,
)
