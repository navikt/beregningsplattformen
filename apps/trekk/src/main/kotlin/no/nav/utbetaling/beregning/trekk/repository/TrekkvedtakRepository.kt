package no.nav.utbetaling.beregning.trekk.repository

import java.sql.ResultSet
import java.time.LocalDate

import org.jdbi.v3.core.Handle

import no.nav.utbetaling.beregning.trekk.domain.SatsType
import no.nav.utbetaling.beregning.trekk.domain.TrekkFagomraade
import no.nav.utbetaling.beregning.trekk.domain.TrekkFagomraadeId
import no.nav.utbetaling.beregning.trekk.domain.Trekkperiode
import no.nav.utbetaling.beregning.trekk.domain.TrekkperiodeId
import no.nav.utbetaling.beregning.trekk.domain.Trekkstatus
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
            .map { row, _ -> row.tilTrekkvedtak() }
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
            .map { row, _ -> row.tilTrekkFagomraade() }
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
            .map { row, _ -> row.tilTrekkperiode() }
            .list()

    private fun ResultSet.tilTrekkperiode(): Trekkperiode =
        Trekkperiode(
            id = TrekkperiodeId(getLong("id")),
            trekkvedtakId = getLong("trekkvedtak_id"),
            datoFom = getObject("dato_fom", LocalDate::class.java),
            datoTom = getObject("dato_tom", LocalDate::class.java),
            sats = getBigDecimal("sats"),
            satstype = SatsType.valueOf(getString("satstype")),
            feilReg = getBoolean("feil_reg"),
            bruker = getString("bruker"),
            tidspktReg = getTimestamp("tidspkt_reg").toLocalDateTime(),
        )

    private fun ResultSet.tilTrekkFagomraade(): TrekkFagomraade =
        TrekkFagomraade(
            id = TrekkFagomraadeId(getLong("id")),
            trekkvedtakId = getLong("trekkvedtak_id"),
            fagomraadeKode = getString("fagomraade_kode"),
            aktiv = getBoolean("aktiv"),
            tidspktReg = getTimestamp("tidspkt_reg").toLocalDateTime(),
        )

    private fun ResultSet.tilTrekkvedtak(): Trekkvedtak =
        Trekkvedtak(
            id = TrekkvedtakId(getLong("id")),
            eksternTrekkId = getObject("ekstern_trekk_id", Long::class.javaObjectType),
            trekktypeKode = getString("trekktype"),
            kreditorIdTss = getString("kreditor_tss_id"),
            kreditorTrekkId = getString("kreditor_trekk_id"),
            kreditorRef = getString("kreditor_ref"),
            belopSaldo = getBigDecimal("belop_saldo"),
            kreditorKid = getString("kreditor_kid"),
            gyldigTom = getObject("gyldig_tom", LocalDate::class.java),
            ansvarligEnhet = getString("ansv_enhet"),
            debitorId = getString("debitor_id"),
            datoPriFom = getObject("dato_pri_fom", LocalDate::class.java),
            kilde = getString("kilde"),
            trekkstatus = Trekkstatus.fromString(getString("trekkstatus")),
            bruker = getString("bruker"),
            tidspktReg = getTimestamp("tidspkt_reg").toLocalDateTime(),
            mottatt = getTimestamp("mottatt").toLocalDateTime(),
        )
}
