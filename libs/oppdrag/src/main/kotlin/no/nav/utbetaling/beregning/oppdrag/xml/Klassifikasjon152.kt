package no.nav.utbetaling.beregning.oppdrag.xml

import java.time.LocalDate

internal data class Klassifikasjon152(
    val kodeKlassifik: String,
    val datoKlassifikFom: LocalDate,
    val tidspktReg: String,
    val saksbehId: String,
)
