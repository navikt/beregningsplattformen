package no.nav.beregning.taskv2

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.beregning.taskv2.domene.PropertiesWrapper
import no.nav.beregning.taskv2.domene.Status
import no.nav.beregning.taskv2.domene.Task
import org.jdbi.v3.core.Handle
import java.time.LocalDateTime
import java.util.Properties

/** Kastes når en optimistisk versjonssjekk treffer 0 rader (en annen pod vant kappløpet). */
class OptimistiskLasException(taskId: Long, versjon: Long) :
    RuntimeException("Optimistisk låsing feilet for task=$taskId versjon=$versjon")

/**
 * 🔴 RØD SONE — manuell optimistisk låsing i JDBI. Hjertet i v2.
 *
 * Spring Data ga prosessering @Version gratis: hver save() ble UPDATE ... WHERE versjon=? og kastet
 * ved 0 treff. Her gjør vi det eksplisitt. Enhver statusendring MÅ gå via [oppdaterMedVersjon], som
 * sjekker versjon, teller oppdaterte rader og kaster [OptimistiskLasException] ved konflikt. Glemmes
 * versjonssjekken, får du tapt oppdatering og dobbeltkjøring.
 */
class TaskRepository(private val handle: Handle) {
    private val mapper = ObjectMapper().registerKotlinModule()

    fun opprett(task: Task): Task {
        val id =
            handle.createUpdate(
                """
                INSERT INTO task (payload, type, status, trigger_tid, metadata, versjon)
                VALUES (:payload, :type, :status, :triggerTid, :metadata, 0)
                """.trimIndent(),
            )
                .bind("payload", task.payload)
                .bind("type", task.type)
                .bind("status", task.status.name)
                .bind("triggerTid", task.triggerTid)
                .bind("metadata", serialiserMetadata(task.metadata))
                .executeAndReturnGeneratedKeys("id")
                .mapTo(Long::class.java)
                .one()
        return task.copy(id = id, versjon = 0)
    }

    fun finnById(id: Long): Task? =
        handle.createQuery("SELECT * FROM task WHERE id = :id")
            .bind("id", id)
            .map(::mapRad)
            .findOne()
            .orElse(null)

    /**
     * Hent en side klare, modne tasks. Flere podder kjører denne samtidig uten radlås; kappløpet
     * avgjøres først når hver pod forsøker [oppdaterMedVersjon] for å plukke.
     */
    fun finnKlareForProsessering(antall: Int): List<Task> =
        handle.createQuery(
            """
            SELECT * FROM task
            WHERE status IN ('UBEHANDLET', 'KLAR_TIL_PLUKK') AND trigger_tid <= now()
            ORDER BY opprettet_tid
            LIMIT :antall
            """.trimIndent(),
        )
            .bind("antall", antall)
            .map(::mapRad)
            .list()

    fun finnMedStatus(status: Status): List<Task> =
        handle.createQuery("SELECT * FROM task WHERE status = :status")
            .bind("status", status.name)
            .map(::mapRad)
            .list()

    fun finnPlukketFor(tid: LocalDateTime): List<Task> =
        handle.createQuery(
            "SELECT * FROM task WHERE status = 'PLUKKET' AND trigger_tid < :tid",
        )
            .bind("tid", tid)
            .map(::mapRad)
            .list()

    fun finnFerdigeEldreEnn(tid: LocalDateTime, antall: Int): List<Long> =
        handle.createQuery(
            "SELECT id FROM task WHERE status = 'FERDIG' AND trigger_tid < :tid LIMIT :antall",
        )
            .bind("tid", tid)
            .bind("antall", antall)
            .mapTo(Long::class.java)
            .list()

    fun slett(ids: List<Long>) {
        if (ids.isEmpty()) return
        handle.createUpdate("DELETE FROM task WHERE id IN (<ids>)").bindList("ids", ids).execute()
    }

    /**
     * Oppdater status (og evt. trigger_tid) med optimistisk versjonssjekk. Returnerer tasken med
     * økt versjon. Kaster [OptimistiskLasException] hvis versjonen ikke matcher lenger.
     */
    fun oppdaterMedVersjon(task: Task, nyStatus: Status, nyTriggerTid: LocalDateTime? = null): Task {
        val oppdaterte =
            handle.createUpdate(
                """
                UPDATE task
                SET status = :status,
                    trigger_tid = COALESCE(:triggerTid, trigger_tid),
                    versjon = versjon + 1
                WHERE id = :id AND versjon = :versjon
                """.trimIndent(),
            )
                .bind("status", nyStatus.name)
                .bind("triggerTid", nyTriggerTid)
                .bind("id", task.id)
                .bind("versjon", task.versjon)
                .execute()

        if (oppdaterte == 0) throw OptimistiskLasException(task.id, task.versjon)

        return task.copy(
            status = nyStatus,
            versjon = task.versjon + 1,
            triggerTid = nyTriggerTid ?: task.triggerTid,
        )
    }

    private fun mapRad(rs: java.sql.ResultSet, ctx: org.jdbi.v3.core.statement.StatementContext): Task =
        Task(
            id = rs.getLong("id"),
            payload = rs.getString("payload"),
            type = rs.getString("type"),
            status = Status.valueOf(rs.getString("status")),
            opprettetTid = rs.getTimestamp("opprettet_tid").toLocalDateTime(),
            triggerTid = rs.getTimestamp("trigger_tid").toLocalDateTime(),
            metadataWrapper = PropertiesWrapper(deserialiserMetadata(rs.getString("metadata"))),
            versjon = rs.getLong("versjon"),
        )

    private fun serialiserMetadata(props: Properties): String {
        val map = props.entries.associate { it.key.toString() to it.value.toString() }
        return mapper.writeValueAsString(map)
    }

    private fun deserialiserMetadata(json: String): Properties {
        @Suppress("UNCHECKED_CAST")
        val map = mapper.readValue(json, Map::class.java) as Map<String, String>
        return Properties().apply { map.forEach { (k, v) -> this[k] = v } }
    }
}
