package no.nav.utbetaling.beregning.oppdrag.input_for_beregning

import java.time.LocalDate
import java.time.LocalDateTime

@JvmInline
value class OppdragId(
    val id: Long,
)

enum class FagomraadeKode {
    IRRELEVANT,
}

data class DatoIntervall(
    val fom: LocalDate,
    val tom: LocalDate? = null,
)

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

data class Oppdrag(
    val id: OppdragId? = null,
    val fagsystemId: String,
    val fagomraade: FagomraadeKode,
    val datoForfall: LocalDate? = null,
    val gjelderId: AktoerId? = null,
    val datoTom: LocalDate? = null,
    val bilagsType: String? = null,
    val bruker: String,
    val tidspktReg: LocalDateTime,
    val oppdragslinjekjeder: List<Oppdragslinjekjede> = emptyList(),
    val avventPerioder: List<Avvent> = emptyList(),
)
