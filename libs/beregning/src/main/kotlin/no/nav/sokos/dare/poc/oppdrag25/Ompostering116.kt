package no.nav.sokos.dare.poc.oppdrag25

import java.time.LocalDate

data class Ompostering116(
    val omPostering: String,
    val datoOmposterFom: LocalDate? = null,
    val feilreg: String? = null,
    val tidspktReg: String,
    val saksbehId: String,
)
