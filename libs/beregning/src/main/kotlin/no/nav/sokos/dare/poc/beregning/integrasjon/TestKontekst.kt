package no.nav.sokos.dare.poc.beregning.integrasjon

import java.time.LocalDate
import java.time.LocalDateTime

import no.nav.sokos.dare.poc.aktoer.AktoerId
import no.nav.sokos.dare.poc.beregning.DatoIntervall

data class TestKontekst(
    val gjelder: AktoerId,
    val utbetalesTil: AktoerId?,
    val periode: DatoIntervall,
    val beregnet: LocalDateTime,
    val overfoeringsdato: LocalDate,
    val ledigFrikortBeloep: java.math.BigDecimal? = null,
)
