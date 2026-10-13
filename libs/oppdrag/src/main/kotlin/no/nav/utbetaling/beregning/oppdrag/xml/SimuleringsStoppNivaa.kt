package no.nav.utbetaling.beregning.oppdrag.xml

import java.time.LocalDate

internal data class SimuleringsStoppNivaa(
    val behandlendeEnhet: String,
    val bilagsType: String,
    val fagsystemId: String,
    val feilkonto: String,
    val forfall: LocalDate,
    val kid: String,
    val kodeFagomraade: String,
    val simuleringsLinjer: List<no.nav.utbetaling.beregning.oppdrag.xml.SimuleringsLinjer> = emptyList(),
    val oppdragsId: Long = 0,
    val stoppnivaId: Int = 0,
    val utbetalesTilId: String,
    val utbetalesTilNavn: String,
)
