package no.nav.utbetaling.beregning.trekk.repository

import kotliquery.TransactionalSession
import kotliquery.queryOf

import no.nav.utbetaling.beregning.trekk.domain.Trekkperiode
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
}
