package no.nav.sokos.dare.poc.oppdrag25

import java.math.BigInteger
import java.time.LocalDate

data class LinjeTekst158(
    val tekstLnr: BigInteger,
    val tekstKode: String? = null,
    val tekst: String? = null,
    val datoTekstFom: LocalDate,
    val datoTekstTom: LocalDate? = null,
    val feilreg: String? = null,
)
