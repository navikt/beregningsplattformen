package no.nav.utbetaling.beregning.trekk.repository

import org.jdbi.v3.core.Handle

import no.nav.utbetaling.beregning.trekk.domain.TrekkFagomraade
import no.nav.utbetaling.beregning.trekk.domain.Trekkperiode
import no.nav.utbetaling.beregning.trekk.domain.Trekkvedtak
import no.nav.utbetaling.beregning.trekk.domain.TrekkvedtakId

class TrekkvedtakRepository {
    fun existsByOsId(
        eksternTrekkId: Long,
        tx: Handle,
    ): Boolean =
        tx
            .createQuery(
                """
                SELECT 1
                FROM trekkvedtak
                WHERE ekstern_trekk_id = :eksternTrekkId
                """.trimIndent(),
            )
            .bind("eksternTrekkId", eksternTrekkId)
            .mapTo(Int::class.javaObjectType)
            .findOne()
            .isPresent

    fun findByEksternTrekkId(
        eksternTrekkId: Long,
        tx: Handle,
    ): Trekkvedtak? =
        tx
            .createQuery(
                """
                SELECT *
                FROM trekkvedtak
                WHERE ekstern_trekk_id = :eksternTrekkId
                """.trimIndent(),
            )
            .bind("eksternTrekkId", eksternTrekkId)
            .mapTo(Trekkvedtak::class.java)
            .findOne()
            .orElse(null)
            ?.apply {
                val trekkvedtakId = requireNotNull(id)
                fagomraader = findFagomraadeByTrekkvedtakId(trekkvedtakId, tx)
                trekkperioder = findTrekkperioderByTrekkvedtakId(trekkvedtakId, tx)
            }

    private fun findFagomraadeByTrekkvedtakId(
        trekkId: TrekkvedtakId,
        tx: Handle,
    ): List<TrekkFagomraade> =
        tx
            .createQuery(
                """
                SELECT *
                FROM trekk_i_fagomraade
                WHERE trekkvedtak_id = :trekkvedtakId
                """.trimIndent(),
            )
            .bind("trekkvedtakId", trekkId.id)
            .mapTo(TrekkFagomraade::class.java)
            .list()

    private fun findTrekkperioderByTrekkvedtakId(
        trekkId: TrekkvedtakId,
        tx: Handle,
    ): List<Trekkperiode> =
        tx
            .createQuery(
                """
                SELECT *
                FROM trekkperiode
                WHERE trekkvedtak_id = :trekkvedtakId
                """.trimIndent(),
            )
            .bind("trekkvedtakId", trekkId.id)
            .mapTo(Trekkperiode::class.java)
            .list()
}
