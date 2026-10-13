package no.nav.utbetaling.beregning.oppdrag.xml

import java.time.LocalDate

internal data class SimuleringsLinjer(
    val antallSats: Double = 0.0,
    val behandlingsKode: String,
    val belop: Double = 0.0,
    val bostedsenhet: String,
    val delytelseId: String,
    val faktiskFom: LocalDate,
    val faktiskTom: LocalDate,
    val funksjonaerId: String,
    val kontostreng: String,
    val korrigering: String,
    val kravhaverId: String,
    val linjeId: Int = 0,
    val sats: Double = 0.0,
    val skyldnerId: String,
    val stonadId: String,
    val tilbakeforing: String,
    val trekkvedtakId: Long = 0,
    val typeSats: String,
    val uforeGrad: String,
)
