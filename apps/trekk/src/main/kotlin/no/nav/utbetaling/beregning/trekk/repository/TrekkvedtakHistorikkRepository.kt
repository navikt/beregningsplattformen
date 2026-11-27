package no.nav.utbetaling.beregning.trekk.repository

import kotliquery.TransactionalSession
import kotliquery.queryOf

class TrekkvedtakHistorikkRepository {
    fun insertTrekkvedtak(
        trekkvedtak: String,
        session: TransactionalSession,
    ) = session.update(
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
}
