package no.nav.utbetaling.beregning.trekk.repository.mapper

import java.sql.ResultSet

import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext

import no.nav.utbetaling.beregning.trekk.domain.TrekkFagomraade
import no.nav.utbetaling.beregning.trekk.domain.TrekkFagomraadeId

internal class TrekkFagomraadeMapper : RowMapper<TrekkFagomraade> {
    override fun map(
        row: ResultSet,
        ctx: StatementContext,
    ): TrekkFagomraade =
        TrekkFagomraade(
            id = TrekkFagomraadeId(row.getLong("id")),
            trekkvedtakId = row.getLong("trekkvedtak_id"),
            fagomraadeKode = row.getString("fagomraade_kode"),
            aktiv = row.getBoolean("aktiv"),
            tidspktReg = row.getTimestamp("tidspkt_reg").toLocalDateTime(),
        )
}
