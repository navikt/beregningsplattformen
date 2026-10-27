package no.nav.sokos.dare.poc.oppdrag25

import java.time.LocalDate

data class OppdragGjelder112(
    val oppdragGjelderId: String,
    val datoOppdragGjelderFom: LocalDate,
    val tidspktReg: String,
    val saksbehId: String,
)
