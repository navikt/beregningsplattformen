package no.nav.utbetaling.beregning.trekk.repository

import kotlin.plus

import io.ktor.server.auth.session
import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf

import no.nav.utbetaling.beregning.trekk.domain.SatsType
import no.nav.utbetaling.beregning.trekk.domain.TrekkFagomraade
import no.nav.utbetaling.beregning.trekk.domain.TrekkFagomraadeId
import no.nav.utbetaling.beregning.trekk.domain.Trekkperiode
import no.nav.utbetaling.beregning.trekk.domain.TrekkperiodeId
import no.nav.utbetaling.beregning.trekk.domain.Trekkstatus
import no.nav.utbetaling.beregning.trekk.domain.Trekkvedtak
import no.nav.utbetaling.beregning.trekk.domain.TrekkvedtakId
import no.nav.utbetaling.beregning.trekk.utils.SqlUtils.asMap

class TrekkvedtakRepository {
    fun existsByOsId(
        eksternTrekkId: Long,
        session: TransactionalSession,
    ): Boolean =
        session.single(
            queryOf(
                """
                SELECT 1
                FROM trekkvedtak
                WHERE ekstern_trekk_id = :eksternTrekkId
                """.trimIndent(),
                mapOf("eksternTrekkId" to eksternTrekkId),
            ),
        ) { 1 } != null

    fun findByEksternTrekkId(
        eksternTrekkId: Long,
        session: TransactionalSession,
    ): Trekkvedtak? =
        session.single(
            queryOf(
                """
                SELECT *
                FROM trekkvedtak
                WHERE ekstern_trekk_id = :eksternTrekkId
                """.trimIndent(),
                mapOf("eksternTrekkId" to eksternTrekkId),
            ),
        ) { row ->
            row.tilTrekkvedtak().apply {
                val trekkvedtakId = requireNotNull(id)
                fagomraader = findFagomraadeByTrekkvedtakId(trekkvedtakId, session)
                trekkperioder = findTrekkperioderByTrekkvedtakId(trekkvedtakId, session)
            }
        }

    private fun findFagomraadeByTrekkvedtakId(
        trekkId: TrekkvedtakId,
        session: TransactionalSession,
    ): List<TrekkFagomraade> =
        session.list(
            queryOf(
                """
                SELECT *
                FROM trekk_i_fagomraade
                WHERE trekkvedtak_id = :trekkvedtakId
                """.trimIndent(),
                mapOf("trekkvedtakId" to trekkId.id),
            ),
        ) { row -> row.tilTrekkFagomraade() }

    private fun findTrekkperioderByTrekkvedtakId(
        trekkId: TrekkvedtakId,
        session: TransactionalSession,
    ): List<Trekkperiode> =
        session.list(
            queryOf(
                """
                SELECT *
                FROM trekkperiode
                WHERE trekkvedtak_id = :trekkvedtakId
                """.trimIndent(),
                mapOf("trekkvedtakId" to trekkId.id),
            ),
        ) { row -> row.tilTrekkperiode() }

    fun insertTrekkvedtak(
        trekkvedtak: Trekkvedtak,
        session: TransactionalSession,
    ): Long? {
        val params = trekkvedtak.asMap() + ("trekkstatus" to trekkvedtak.trekkstatus.name)
        val id =
            session.updateAndReturnGeneratedKey(
                queryOf(
                    """
                    INSERT INTO trekkvedtak (
                    ekstern_trekk_id,
                    trekktype,
                    kreditor_tss_id,
                    kreditor_trekk_id,
                    kreditor_ref,
                    belop_saldo,
                    kreditor_kid,
                    gyldig_tom,
                    ansv_enhet,
                    debitor_id,
                    dato_pri_fom,
                    kilde,
                    trekkstatus,
                    bruker,
                    tidspkt_reg,
                    mottatt) 
                    VALUES (:eksternTrekkId, :trekktypeKode, :kreditorIdTss, :kreditorTrekkId, :kreditorRef, :belopSaldo, :kreditorKid, :gyldigTom, :ansvarligEnhet, :debitorId, :datoPriFom, :kilde, :trekkstatus, :bruker, :tidspktReg, CURRENT_TIMESTAMP)
                    """.trimIndent(),
                    params,
                ),
            )
        return id
    }

    fun updateTrekkvedtak(
        trekkvedtakId: TrekkvedtakId,
        endretTrekkvedtak: Trekkvedtak,
        session: TransactionalSession,
    ) {
        val params =
            endretTrekkvedtak.asMap() + ("trekkstatus" to endretTrekkvedtak.trekkstatus.name) + ("trekkvedtakId" to trekkvedtakId.id)
        session.update(
            queryOf(
                """
                UPDATE trekkvedtak 
                SET ekstern_trekk_id = :eksternTrekkId,
                    trekktype = :trekktypeKode,
                    kreditor_tss_id = :kreditorIdTss,
                    kreditor_trekk_id = :kreditorTrekkId,
                    kreditor_ref = :kreditorRef,
                    belop_saldo = :belopSaldo,
                    kreditor_kid = :kreditorKid,
                    gyldig_tom = :gyldigTom,
                    ansv_enhet = :ansvarligEnhet,
                    debitor_id = :debitorId,
                    dato_pri_fom = :datoPriFom,
                    kilde = :kilde,
                    trekkstatus = :trekkstatus,
                    bruker = :bruker,
                    tidspkt_reg = :tidspktReg,
                    mottatt = CURRENT_TIMESTAMP
                WHERE id = :trekkvedtakId
                """.trimIndent(),
                params,
            ),
        )
    }

    fun updateTrekkvedtakReberegningstidspkt(
        trekkvedtakId: TrekkvedtakId,
        session: TransactionalSession,
    ) {
        session.update(
            queryOf(
                """
                UPDATE trekkvedtak
                SET tidspkt_reberegning = CURRENT_TIMESTAMP
                WHERE id = :trekkvedtakId
                """.trimIndent(),
                mapOf("trekkvedtakId" to trekkvedtakId.id),
            ),
        )
    }

    private fun Row.tilTrekkperiode(): Trekkperiode =
        Trekkperiode(
            id = TrekkperiodeId(long("id")),
            trekkvedtakId = long("trekkvedtak_id"),
            datoFom = localDate("dato_fom"),
            datoTom = localDateOrNull("dato_tom"),
            sats = bigDecimal("sats"),
            satstype = SatsType.valueOf(string("satstype")),
            feilReg = boolean("feil_reg"),
            bruker = string("bruker"),
            tidspktReg = localDateTime("tidspkt_reg"),
        )

    private fun Row.tilTrekkFagomraade(): TrekkFagomraade =
        TrekkFagomraade(
            id = TrekkFagomraadeId(long("id")),
            trekkvedtakId = long("trekkvedtak_id"),
            fagomraadeKode = string("fagomraade_kode"),
            aktiv = boolean("aktiv"),
            tidspktReg = localDateTime("tidspkt_reg"),
        )

    private fun Row.tilTrekkvedtak(): Trekkvedtak =
        Trekkvedtak(
            id = TrekkvedtakId(long("id")),
            eksternTrekkId = longOrNull("ekstern_trekk_id"),
            trekktypeKode = string("trekktype"),
            kreditorIdTss = stringOrNull("kreditor_tss_id"),
            kreditorTrekkId = stringOrNull("kreditor_trekk_id"),
            kreditorRef = stringOrNull("kreditor_ref"),
            belopSaldo = bigDecimalOrNull("belop_saldo"),
            kreditorKid = stringOrNull("kreditor_kid"),
            gyldigTom = localDateOrNull("gyldig_tom"),
            ansvarligEnhet = string("ansv_enhet"),
            debitorId = string("debitor_id"),
            datoPriFom = localDateOrNull("dato_pri_fom"),
            kilde = stringOrNull("kilde"),
            trekkstatus = Trekkstatus.fromString(string("trekkstatus")),
            bruker = string("bruker"),
            tidspktReg = localDateTime("tidspkt_reg"),
            mottatt = localDateTime("mottatt"),
        )
}
