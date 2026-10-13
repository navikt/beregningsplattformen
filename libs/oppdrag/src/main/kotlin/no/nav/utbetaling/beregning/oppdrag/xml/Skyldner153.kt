package no.nav.utbetaling.beregning.oppdrag.xml

import java.time.LocalDate

internal data class Skyldner153(
    val skyldnerId: String,
    val datoSkyldnerFom: LocalDate,
    val tidspktReg: String,
    val saksbehId: String,
)
