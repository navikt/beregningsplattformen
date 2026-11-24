package no.nav.utbetaling.beregning.trekk.repository

import kotliquery.TransactionalSession
import kotliquery.queryOf

import no.nav.utbetaling.beregning.trekk.domain.TrekkFagomraade

class TrekkFagomraadeRepository {
    fun insertTrekkFagomraade(
        trekkFagomraade: TrekkFagomraade,
        session: TransactionalSession,
    ): Long? =
        session.updateAndReturnGeneratedKey(
            queryOf(
                """
                INSERT INTO trekk_i_fagomraade (
                trekkvedtak_id,
                fagomraade_kode,
                aktiv,
                tidspkt_reg)
                VALUES (:trekkvedtakId, :fagomraadeKode, :aktiv, :tidspktReg)
                """.trimIndent(),
                mapOf(
                    "trekkvedtakId" to trekkFagomraade.trekkvedtakId,
                    "fagomraadeKode" to trekkFagomraade.fagomraadeKode,
                    "aktiv" to trekkFagomraade.aktiv,
                    "tidspktReg" to trekkFagomraade.tidspktReg,
                ),
            ),
        )
}
