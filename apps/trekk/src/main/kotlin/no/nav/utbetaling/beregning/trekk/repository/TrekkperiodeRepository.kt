package no.nav.utbetaling.beregning.trekk.repository

import kotlin.plus

import kotliquery.TransactionalSession
import kotliquery.queryOf

import no.nav.utbetaling.beregning.trekk.domain.Trekkperiode
import no.nav.utbetaling.beregning.trekk.domain.TrekkvedtakId
import no.nav.utbetaling.beregning.trekk.utils.SqlUtils.asMap

class TrekkperiodeRepository {
    fun insertTrekkperiode(
        trekkperiode: Trekkperiode,
        session: TransactionalSession,
    ): Long? =
        session.updateAndReturnGeneratedKey(
            queryOf(
                """
                INSERT INTO trekkperiode (
                trekkvedtak_id,
                dato_fom,
                dato_tom,
                sats,
                satstype,
                feil_reg,
                bruker,
                tidspkt_reg)
                VALUES (:trekkvedtakId, :datoFom, :datoTom, :sats, :trekksatstype, :feilReg, :bruker, :tidspktReg)
                """.trimIndent(),
                trekkperiode.asMap() + ("trekksatstype" to trekkperiode.satstype.name),
            ),
        )

    fun updateTrekkperiode(
        trekkvedtakId: TrekkvedtakId,
        trekkperiode: Trekkperiode,
        session: TransactionalSession,
    ) {
        session.execute(
            queryOf(
                """
                UPDATE trekkperiode
                SET 
                    sats = :sats,
                    satstype = :trekksatstype,
                    feil_reg = :feilReg,
                    bruker = :bruker,
                    tidspkt_reg = :tidspktReg
                WHERE trekkvedtak_id = :trekkvedtakId
                AND dato_fom = :datoFom
                AND dato_tom IS NOT DISTINCT FROM :datoTom
                """.trimIndent(),
                trekkperiode.asMap() + ("trekkvedtakId" to trekkvedtakId.id) + ("trekksatstype" to trekkperiode.satstype.name),
            ),
        )
    }

    fun deleteByTrekkvedtakIdAndPeriode(
        trekkvedtakId: TrekkvedtakId,
        periode: Trekkperiode,
        session: TransactionalSession,
    ) {
        session.execute(
            queryOf(
                """
                DELETE
                FROM trekkperiode
                WHERE trekkvedtak_id = :trekkvedtakId
                AND dato_fom = :datoFom
                AND dato_tom IS NOT DISTINCT FROM :datoTom
                """.trimIndent(),
                mapOf(
                    "trekkvedtakId" to trekkvedtakId.id,
                    "datoFom" to periode.datoFom,
                    "datoTom" to periode.datoTom,
                ),
            ),
        )
    }
}
