package no.nav.utbetaling.beregning.trekk.repository

import kotliquery.TransactionalSession
import kotliquery.queryOf

import no.nav.utbetaling.beregning.trekk.domain.TrekkFagomraade
import no.nav.utbetaling.beregning.trekk.domain.TrekkvedtakId

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

    fun updateTrekkFagomraade(
        trekkvedtakId: TrekkvedtakId,
        trekkFagomraade: TrekkFagomraade,
        session: TransactionalSession,
    ) {
        session.execute(
            queryOf(
                """
                UPDATE trekk_i_fagomraade
                SET 
                    aktiv = :aktiv,
                    tidspkt_reg = :tidspktReg
                WHERE trekkvedtak_id = :trekkvedtakId
                AND fagomraade_kode = :fagomraadeKode
                """.trimIndent(),
                mapOf(
                    "trekkvedtakId" to trekkvedtakId.id,
                    "aktiv" to trekkFagomraade.aktiv,
                    "tidspktReg" to trekkFagomraade.tidspktReg,
                ),
            ),
        )
    }

    fun deleteByTrekkvedtakIdAndFagomraadeKode(
        trekkvedtakId: TrekkvedtakId,
        fagomraadekode: String,
        session: TransactionalSession,
    ) {
        session.execute(
            queryOf(
                """
                DELETE
                FROM trekk_i_fagomraade
                WHERE trekkvedtak_id = :trekkvedtakId
                AND fagomraade_kode = :fagomraadeKode
                """.trimIndent(),
                mapOf(
                    "trekkvedtakId" to trekkvedtakId.id,
                    "fagomraadeKode" to fagomraadekode,
                ),
            ),
        )
    }
}
