package no.nav.sokos.dare.poc.oppdrag25

import java.time.LocalDate

data class LinjeStatus151(
    val kodeStatusLinje: TkodeStatusLinje,
    val datoStatusFom: LocalDate,
    val tidspktReg: String,
    val saksbehId: String,
)
