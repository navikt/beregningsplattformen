package no.nav.utbetaling.beregning.oppdrag.xml

import java.math.BigDecimal
import java.time.LocalDate

internal data class SimuleringsResultat(
    val belop: BigDecimal,
    val datoKjores: LocalDate,
    val gjelderId: String,
    val gjelderNavn: String,
    val kodeFaggruppe: String,
    val simuleringsPerioder: List<no.nav.utbetaling.beregning.oppdrag.xml.SimuleringsPerioder> = emptyList(),
)
