package no.nav.utbetaling.beregning.oppdrag.inputForBeregning

import java.time.LocalDateTime

@JvmInline
value class OppdragslinjekjedeId(
    val id: Long,
)

data class Oppdragslinjekjede(
    val id: OppdragslinjekjedeId? = null,
    val oppdragId: OppdragId,
    val tidspktReg: LocalDateTime,
    val linjer: List<Oppdragslinje> = emptyList(),
)
