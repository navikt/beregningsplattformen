package no.nav.utbetaling.beregning.trekk.persistence

import kotliquery.TransactionalSession
import kotliquery.queryOf

class TrekkvedtakHistorikkRepository {
    fun insertTrekkvedtak(
        trekkvedtak: String,
        session: TransactionalSession,
    ): Long? {
        val id =
            session.updateAndReturnGeneratedKey(
                queryOf(
                    """
                    INSERT INTO trekkvedtak_historikk (
                        trekkvedtak,
                        tidspkt_reg
                    ) 
                    VALUES (:trekkvedtak, CURRENT_TIMESTAMP)
                    """.trimIndent(),
                    mapOf("trekkvedtak" to trekkvedtak),
                ),
            )
        return id
    }
}
