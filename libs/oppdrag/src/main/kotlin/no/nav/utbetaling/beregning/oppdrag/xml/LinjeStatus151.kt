package no.nav.utbetaling.beregning.oppdrag.xml

import java.time.LocalDate

internal data class LinjeStatus151(
    val kodeStatusLinje: TkodeStatusLinje,
    val datoStatusFom: LocalDate,
    val tidspktReg: String,
    val saksbehId: String,
)
