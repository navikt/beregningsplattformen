package no.nav.utbetaling.beregning.trekk.persistence

import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf

import no.nav.utbetaling.beregning.trekk.domain.SatsType
import no.nav.utbetaling.beregning.trekk.domain.TrekkFagomraade
import no.nav.utbetaling.beregning.trekk.domain.TrekkFagomraadeId
import no.nav.utbetaling.beregning.trekk.domain.Trekkberegningstype
import no.nav.utbetaling.beregning.trekk.domain.Trekkperiode
import no.nav.utbetaling.beregning.trekk.domain.TrekkperiodeId
import no.nav.utbetaling.beregning.trekk.domain.Trekkstatus
import no.nav.utbetaling.beregning.trekk.domain.Trekktype
import no.nav.utbetaling.beregning.trekk.domain.Trekkvedtak
import no.nav.utbetaling.beregning.trekk.domain.TrekkvedtakId

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
                trekktype = findTrekktypeByKode(row.string("trekktype"), session)
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

    fun findTrekktypeByKode(
        trekktypeKode: String,
        session: TransactionalSession,
    ): Trekktype? =
        session.single(
            queryOf(
                """
                SELECT *
                FROM trekktype
                WHERE kode_trekktype = :trekktypeKode
                """.trimIndent(),
                mapOf("trekktypeKode" to trekktypeKode),
            ),
        ) { row -> row.tilTrekktype() }

    private fun Row.tilTrekktype(): Trekktype =
        Trekktype(
            kodeTrekktype = string("kode_trekktype"),
            beskrivelse = string("beskrivelse"),
            prioritet = string("prioritet"), // TODO: integer
            kodeKlasseTrekk = string("kode_klasse_trekk"),
            kodeTrekkategori = string("kode_trekkategori"),
            reduserSkattegr = boolean("reduser_skattegr"),
            kodeOppgjoerstype = string("kode_oppgjorstype"),
            kodeKlasseOppgj = string("kode_klasse_oppgj"),
            belopsgrense = long("belopsgrense"),
            bruker = string("brukerid"),
            tidspktEndret = localDateTime("tidspkt_endret"),
            trekkberegningstype = Trekkberegningstype.valueOf(string("trekkberegningstype")),
        )

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
            debitorId = long("debitor_id"),
            datoPriFom = localDateOrNull("dato_pri_fom"),
            kilde = stringOrNull("kilde"),
            trekkstatus = Trekkstatus.fromString(string("trekkstatus")),
            bruker = string("bruker"),
            tidspktReg = localDateTime("tidspkt_reg"),
            mottatt = localDateTimeOrNull("mottatt"),
        )
}
