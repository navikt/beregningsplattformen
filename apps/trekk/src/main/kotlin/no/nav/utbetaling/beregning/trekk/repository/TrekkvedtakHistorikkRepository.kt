package no.nav.utbetaling.beregning.trekk.repository

import org.jdbi.v3.core.Handle

class TrekkvedtakHistorikkRepository {
    fun insertTrekkvedtak(
        trekkvedtak: String,
        tx: Handle,
    ): Long? =
        tx
            .createUpdate(
                """
                INSERT INTO trekkvedtak_historikk (
                    trekkvedtak,
                    tidspkt_reg
                )
                VALUES (:trekkvedtak, CURRENT_TIMESTAMP)
                """.trimIndent(),
            )
            .bind("trekkvedtak", trekkvedtak)
            .executeAndReturnGeneratedKeys("id")
            .mapTo(Long::class.javaObjectType)
            .findOne()
            .orElse(null)
}
