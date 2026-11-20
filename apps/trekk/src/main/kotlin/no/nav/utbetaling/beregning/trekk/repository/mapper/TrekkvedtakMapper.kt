package no.nav.utbetaling.beregning.trekk.repository.mapper

import java.sql.ResultSet
import java.time.LocalDate

import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext

import no.nav.utbetaling.beregning.trekk.domain.Trekkstatus
import no.nav.utbetaling.beregning.trekk.domain.Trekkvedtak
import no.nav.utbetaling.beregning.trekk.domain.TrekkvedtakId

internal class TrekkvedtakMapper : RowMapper<Trekkvedtak> {
    override fun map(
        row: ResultSet,
        ctx: StatementContext,
    ): Trekkvedtak =
        Trekkvedtak(
            id = TrekkvedtakId(row.getLong("id")),
            eksternTrekkId = row.getObject("ekstern_trekk_id", Long::class.javaObjectType),
            trekktypeKode = row.getString("trekktype"),
            kreditorIdTss = row.getString("kreditor_tss_id"),
            kreditorTrekkId = row.getString("kreditor_trekk_id"),
            kreditorRef = row.getString("kreditor_ref"),
            belopSaldo = row.getBigDecimal("belop_saldo"),
            kreditorKid = row.getString("kreditor_kid"),
            gyldigTom = row.getObject("gyldig_tom", LocalDate::class.java),
            ansvarligEnhet = row.getString("ansv_enhet"),
            debitorId = row.getString("debitor_id"),
            datoPriFom = row.getObject("dato_pri_fom", LocalDate::class.java),
            kilde = row.getString("kilde"),
            trekkstatus = Trekkstatus.fromString(row.getString("trekkstatus")),
            bruker = row.getString("bruker"),
            tidspktReg = row.getTimestamp("tidspkt_reg").toLocalDateTime(),
            mottatt = row.getTimestamp("mottatt").toLocalDateTime(),
        )
}
