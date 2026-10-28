package no.nav.beregning.taskv2.domene

import java.time.LocalDateTime
import java.util.Properties
import java.util.UUID

const val MDC_CALL_ID = "callId"

/**
 * En task. Portet fra familie-prosessering, men metadata holder kun callId (ingen fagsak/behandling),
 * og `versjon` driver optimistisk låsing manuelt (prosessering fikk det gratis fra Spring Data @Version).
 *
 * toString utelater payload og metadata-verdier bevisst, så de ikke havner i logger.
 */
data class Task(
    val id: Long = 0L,
    val payload: String,
    val type: String,
    val status: Status = Status.UBEHANDLET,
    val opprettetTid: LocalDateTime = LocalDateTime.now(),
    val triggerTid: LocalDateTime = LocalDateTime.now(),
    val metadataWrapper: PropertiesWrapper =
        PropertiesWrapper(
            Properties().apply { this[MDC_CALL_ID] = UUID.randomUUID().toString() },
        ),
    val versjon: Long = 0L,
) {
    val metadata: Properties get() = metadataWrapper.properties

    val callId: String get() = metadata.getProperty(MDC_CALL_ID)

    fun medTriggerTid(tid: LocalDateTime): Task = copy(triggerTid = tid)

    override fun toString(): String =
        "Task(id=$id, type=$type, status=$status, versjon=$versjon, triggerTid=$triggerTid)"
}
