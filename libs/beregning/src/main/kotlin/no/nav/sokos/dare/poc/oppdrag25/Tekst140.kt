package no.nav.sokos.dare.poc.oppdrag25

import java.math.BigInteger
import java.time.LocalDate

data class Tekst140(
    val tekstLnr: BigInteger,
    val kodeTekst: String? = null,
    val tekst: String? = null,
    val datoTekstFom: LocalDate,
    val datoTekstTom: LocalDate? = null,
    val feilreg: String? = null,
)
