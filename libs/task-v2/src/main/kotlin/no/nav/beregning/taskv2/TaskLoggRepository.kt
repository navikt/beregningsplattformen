package no.nav.beregning.taskv2

import no.nav.beregning.taskv2.domene.Loggtype
import no.nav.beregning.taskv2.domene.TaskLogg
import org.jdbi.v3.core.Handle

/** JDBI-repository for task_logg (append-only). */
class TaskLoggRepository(private val handle: Handle) {

    fun lagre(logg: TaskLogg): Long =
        handle.createUpdate(
            """
            INSERT INTO task_logg (task_id, type, node, endret_av, melding)
            VALUES (:taskId, :type, :node, :endretAv, :melding)
            """.trimIndent(),
        )
            .bind("taskId", logg.taskId)
            .bind("type", logg.type.name)
            .bind("node", logg.node)
            .bind("endretAv", logg.endretAv)
            .bind("melding", logg.melding)
            .executeAndReturnGeneratedKeys("id")
            .mapTo(Long::class.java)
            .one()

    fun antallAvType(taskId: Long, type: Loggtype): Int =
        handle.createQuery(
            "SELECT count(*) FROM task_logg WHERE task_id = :taskId AND type = :type",
        )
            .bind("taskId", taskId)
            .bind("type", type.name)
            .mapTo(Int::class.java)
            .one()

    fun slettForTasks(taskIds: List<Long>) {
        if (taskIds.isEmpty()) return
        handle.createUpdate("DELETE FROM task_logg WHERE task_id IN (<ids>)")
            .bindList("ids", taskIds)
            .execute()
    }
}
