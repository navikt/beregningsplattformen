package no.nav.utbetaling.beregning.oppdrag.xml

import java.math.BigInteger
import java.time.LocalDate

internal data class Tekst140(
    val tekstLnr: BigInteger,
    val kodeTekst: String? = null,
    val tekst: String? = null,
    val datoTekstFom: LocalDate,
    val datoTekstTom: LocalDate? = null,
    val feilreg: String? = null,
)
