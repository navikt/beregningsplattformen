package no.nav.beregning.task.arkiv

import mu.KotlinLogging
import org.jdbi.v3.core.Jdbi
import java.time.Duration

private val log = KotlinLogging.logger {}

/**
 * Flytter FERDIG-tasks (og historikken deres) eldre enn [eldreEnn] til arkivtabellene, så JOBB
 * og plukkeindeksen holdes små. Kjøres på intervall av en bakgrunnstråd eller en cron-task.
 *
 * Flyttingen skjer i én transaksjon per batch: kopier til arkiv, så slett fra kilde. Rekkefølgen
 * på slettingene respekterer fremmednøkkelen (historikk før jobb).
 */
class ArkiverFerdige(
    private val jdbi: Jdbi,
    private val eldreEnn: Duration = Duration.ofDays(7),
    private val batchStorrelse: Int = 500,
) {
    fun arkiver(): Int =
        jdbi.inTransaction<Int, Exception> { handle ->
            val grense = java.time.LocalDateTime.now().minus(eldreEnn)
            val ids =
                handle.createQuery(
                    """
                    SELECT id FROM JOBB
                    WHERE status = 'FERDIG' AND sist_endret < :grense
                    ORDER BY id
                    LIMIT :batch
                    FOR UPDATE SKIP LOCKED
                    """.trimIndent(),
                )
                    .bind("grense", grense)
                    .bind("batch", batchStorrelse)
                    .mapTo(Long::class.java)
                    .list()

            if (ids.isEmpty()) return@inTransaction 0

            handle.createUpdate(
                """
                INSERT INTO JOBB_HISTORIKK_ARKIV (id, jobb_id, status, feilmelding, opprettet_tid)
                SELECT id, jobb_id, status, feilmelding, opprettet_tid FROM JOBB_HISTORIKK WHERE jobb_id IN (<ids>)
                """.trimIndent(),
            ).bindList("ids", ids).execute()

            handle.createUpdate(
                """
                INSERT INTO JOBB_ARKIV (id, type, status, neste_kjoring, parameters, payload, prioritet, antall_forsok, opprettet_tid, sist_endret)
                SELECT id, type, status, neste_kjoring, parameters, payload, prioritet, antall_forsok, opprettet_tid, sist_endret FROM JOBB WHERE id IN (<ids>)
                """.trimIndent(),
            ).bindList("ids", ids).execute()

            handle.createUpdate("DELETE FROM JOBB_HISTORIKK WHERE jobb_id IN (<ids>)").bindList("ids", ids).execute()
            handle.createUpdate("DELETE FROM JOBB WHERE id IN (<ids>)").bindList("ids", ids).execute()

            log.info { "Arkiverte ${ids.size} ferdige tasks" }
            ids.size
        }
}
