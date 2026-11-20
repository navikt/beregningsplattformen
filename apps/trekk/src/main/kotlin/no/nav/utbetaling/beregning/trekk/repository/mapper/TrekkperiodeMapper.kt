package no.nav.utbetaling.beregning.trekk.repository.mapper

import java.sql.ResultSet
import java.time.LocalDate

import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext

import no.nav.utbetaling.beregning.trekk.domain.SatsType
import no.nav.utbetaling.beregning.trekk.domain.Trekkperiode
import no.nav.utbetaling.beregning.trekk.domain.TrekkperiodeId

internal class TrekkperiodeMapper : RowMapper<Trekkperiode> {
    override fun map(
        row: ResultSet,
        ctx: StatementContext,
    ): Trekkperiode =
        Trekkperiode(
            id = TrekkperiodeId(row.getLong("id")),
            trekkvedtakId = row.getLong("trekkvedtak_id"),
            datoFom = row.getObject("dato_fom", LocalDate::class.java),
            datoTom = row.getObject("dato_tom", LocalDate::class.java),
            sats = row.getBigDecimal("sats"),
            satstype = SatsType.valueOf(row.getString("satstype")),
            feilReg = row.getBoolean("feil_reg"),
            bruker = row.getString("bruker"),
            tidspktReg = row.getTimestamp("tidspkt_reg").toLocalDateTime(),
        )
}
