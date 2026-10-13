package no.nav.utbetaling.beregning.oppdrag.xml

import java.time.LocalDate

internal data class Ompostering116(
    val omPostering: String,
    val datoOmposterFom: LocalDate? = null,
    val feilreg: String? = null,
    val tidspktReg: String,
    val saksbehId: String,
)
