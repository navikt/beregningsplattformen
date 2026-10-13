package no.nav.utbetaling.beregning.oppdrag.xml

import java.time.LocalDate

internal data class HentOppdrag195(
    val kodeAksjon: String,
    val kodeFagomraade: String? = null,
    val fagsystemId: String? = null,
    val oppdragsId: Long? = null,
    val oppdragGjelderId: String? = null,
    val datoFom: LocalDate? = null,
    val datoTom: LocalDate? = null,
)
