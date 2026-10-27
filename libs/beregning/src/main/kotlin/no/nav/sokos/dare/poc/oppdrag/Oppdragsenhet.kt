package no.nav.sokos.dare.poc.oppdrag

import java.time.LocalDate
import java.time.LocalDateTime

import kotlinx.serialization.Serializable

import no.nav.sokos.dare.poc.structural.IdValueClass

data class Oppdragsenhet(
    val id: OppdragsenhetId? = null,
    val oppdragsId: OppdragId,
    val enhet: String,
    val typeEnhet: String,
    val datoFom: LocalDate,
    val bruker: String,
    val tidspktReg: LocalDateTime,
)

@Serializable
@JvmInline
value class OppdragsenhetId(
    override val id: Long,
) : IdValueClass
