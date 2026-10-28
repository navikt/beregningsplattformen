package no.nav.beregning.task

import java.time.LocalDateTime

/**
 * Det som legges i kø. Payload er ren tekst (JSON eller XML) og tolkes ikke av biblioteket.
 *
 * Merk: [toString] logger aldri payload-innhold. Payload kan inneholde fritekst vi ikke vil
 * ha i logger, så bare metadata eksponeres. Denne invarianten er en del av sikkerhetskontrakten.
 */
data class TaskInput(
    val type: String,
    val payload: String,
    val prioritet: Int = Prioritet.NORMAL,
    val nesteKjoring: LocalDateTime = LocalDateTime.now(),
    val parameters: String? = null,
) {
    override fun toString(): String =
        "TaskInput(type=$type, prioritet=$prioritet)"
}

/**
 * En task lest fra basen, med teknisk tilstand plukke-/retry-logikken trenger.
 */
data class TaskRecord(
    val id: Long,
    val type: String,
    val status: TaskStatus,
    val payload: String,
    val prioritet: Int,
    val antallForsok: Int,
    val nesteKjoring: LocalDateTime,
    val parameters: String?,
) {
    fun toInput(): TaskInput =
        TaskInput(type, payload, prioritet, nesteKjoring, parameters)

    override fun toString(): String =
        "TaskRecord(id=$id, type=$type, status=$status, antallForsok=$antallForsok)"
}
