package no.nav.sokos.dare.poc.avvent

import java.time.LocalDate
import java.time.LocalDateTime

import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.oppdrag.OppdragId

data class Avvent(
    val id: Long? = null,
    val oppdragId: OppdragId,
    val periode: DatoIntervall,
    val kodeArsak: String,
    val datoOverfores: LocalDate?,
    val kansellert: Boolean = false,
    val bruker: String,
    val tidspktReg: LocalDateTime = LocalDateTime.now(),
)
