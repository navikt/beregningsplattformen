package no.nav.utbetaling.beregning.trekk

import javax.sql.DataSource

import kotliquery.TransactionalSession

import no.nav.utbetaling.beregning.log.Log
import no.nav.utbetaling.beregning.trekk.domain.SatsType
import no.nav.utbetaling.beregning.trekk.domain.TrekkvedtakId
import no.nav.utbetaling.beregning.trekk.dto.Trekk
import no.nav.utbetaling.beregning.trekk.repository.TrekkFagomraadeRepository
import no.nav.utbetaling.beregning.trekk.repository.TrekkperiodeRepository
import no.nav.utbetaling.beregning.trekk.repository.TrekkvedtakHistorikkRepository
import no.nav.utbetaling.beregning.trekk.repository.TrekkvedtakRepository
import no.nav.utbetaling.beregning.trekk.utils.SqlUtils.transaction
import no.nav.utbetaling.beregning.trekk.utils.TrekkConverter.toTrekkFagomraade
import no.nav.utbetaling.beregning.trekk.utils.TrekkConverter.toTrekkVedtak
import no.nav.utbetaling.beregning.trekk.utils.TrekkConverter.toTrekkperiode

class TrekkService(
    private val dataSource: DataSource,
    private val trekkvedtakHistorikkRepository: TrekkvedtakHistorikkRepository = TrekkvedtakHistorikkRepository(),
    private val trekkvedtakRepository: TrekkvedtakRepository = TrekkvedtakRepository(),
    private val trekkperiodeRepository: TrekkperiodeRepository = TrekkperiodeRepository(),
    private val trekkFagomraadeRepository: TrekkFagomraadeRepository = TrekkFagomraadeRepository(),
) {
    private val endringsplanlegger = TrekkvedtakEndringsplanlegger()

    fun lagreTrekkvedtakBevis(trekkJson: String) =
        dataSource.transaction { session ->
            trekkvedtakHistorikkRepository.insertTrekkvedtak(trekkJson, session)
        }

    fun lagEllerOppdaterTrekkvedtak(trekk: Trekk) {
        dataSource.transaction { session ->
            when (trekkvedtakRepository.existsByOsId(trekk.trekkvedtakId!!, session)) {
                true -> oppdaterTrekkvedtak(trekk, session)
                false -> lagTrekkvedtak(trekk, session)
            }
        }
    }

    fun lagTrekkvedtak(
        newTrekkvedtak: Trekk,
        tx: TransactionalSession,
    ) {
        Log.info("Oppretter nytt trekkvedtak for OS-trekkId: ${newTrekkvedtak.trekkvedtakId}")
        val trekkId =
            trekkvedtakRepository.insertTrekkvedtak(newTrekkvedtak.toTrekkVedtak(), tx)
                ?: throw IllegalStateException("Kunne ikke opprette trekkvedtak for debitorId: ${newTrekkvedtak.debitorId}")

        val endringsplan = endringsplanlegger.planlegg(newTrekkvedtak)
        endringsplan.nyePerioder.forEach { periode ->
            trekkperiodeRepository.insertTrekkperiode(
                trekkperiode = periode.toTrekkperiode(trekkId, SatsType.valueOf(newTrekkvedtak.kodeTrekkAlternativ.substring(3, 4))),
                session = tx,
            )
        }
        endringsplan.nyeFagomraader.forEach { fagomraade ->
            trekkFagomraadeRepository.insertTrekkFagomraade(
                trekkFagomraade = fagomraade.toTrekkFagomraade(trekkId),
                session = tx,
            )
        }
        Log.info("Trekkvedtak opprettet med id: $trekkId")
    }

    fun oppdaterTrekkvedtak(
        changedTrekkvedtak: Trekk,
        tx: TransactionalSession,
    ): Trekk {
        Log.info("Oppdaterer trekkvedtak for OS-trekkId: ${changedTrekkvedtak.trekkvedtakId}")
        val existingTrekkvedtak =
            trekkvedtakRepository.findByEksternTrekkId(changedTrekkvedtak.trekkvedtakId!!, tx)
                ?: throw IllegalStateException("Kunne ikke finne trekkvedtak for eksternTrekkId ${changedTrekkvedtak.trekkvedtakId} og debitorId ${changedTrekkvedtak.debitorId}")
        val trekkvedtakId = existingTrekkvedtak.id!!
        val endringsplan = endringsplanlegger.planlegg(changedTrekkvedtak, existingTrekkvedtak)

        trekkvedtakRepository.updateTrekkvedtak(trekkvedtakId, changedTrekkvedtak.toTrekkVedtak(), tx)
        lagreTrekkperioder(endringsplan, changedTrekkvedtak, trekkvedtakId, tx)
        lagreFagomraader(endringsplan, trekkvedtakId, tx)

        val recalculateNecessary = ReberegnEvaluator.erReberegningPaakrevd(existingTrekkvedtak, changedTrekkvedtak)
        if (recalculateNecessary) {
            Log.info("Reberegning av trekkvedtak med OS-trekkId ${changedTrekkvedtak.trekkvedtakId} er nødvendig.")
        }
        trekkvedtakRepository.updateTrekkvedtakReberegningstidspkt(trekkvedtakId, tx)
        Log.info("Trekkvedtak med OS-trekkId ${changedTrekkvedtak.trekkvedtakId} endret")
        return changedTrekkvedtak.apply { id = trekkvedtakId.id }
    }

    private fun lagreTrekkperioder(
        endringsplan: TrekkvedtakEndringsplan,
        changedTrekkvedtak: Trekk,
        trekkvedtakId: TrekkvedtakId,
        session: TransactionalSession,
    ) {
        endringsplan.nyePerioder.forEach { periode ->
            trekkperiodeRepository.insertTrekkperiode(
                trekkperiode = periode.toTrekkperiode(trekkvedtakId.id, SatsType.valueOf(changedTrekkvedtak.kodeTrekkAlternativ.substring(3, 4))),
                session = session,
            )
        }
        endringsplan.oppdatertePerioder.forEach { periode ->
            trekkperiodeRepository.updateTrekkperiode(
                trekkvedtakId = trekkvedtakId,
                trekkperiode = periode.toTrekkperiode(trekkvedtakId.id, SatsType.valueOf(changedTrekkvedtak.kodeTrekkAlternativ.substring(3, 4))),
                session = session,
            )
        }
        endringsplan.fjernedePerioder.forEach { periode ->
            trekkperiodeRepository.deleteByTrekkvedtakIdAndPeriode(
                trekkvedtakId = trekkvedtakId,
                periode = periode,
                session = session,
            )
        }
    }

    private fun lagreFagomraader(
        endringsplan: TrekkvedtakEndringsplan,
        trekkvedtakId: TrekkvedtakId,
        session: TransactionalSession,
    ) {
        endringsplan.nyeFagomraader.forEach { fagomraade ->
            trekkFagomraadeRepository.insertTrekkFagomraade(
                trekkFagomraade = fagomraade.toTrekkFagomraade(trekkvedtakId.id),
                session = session,
            )
        }
        endringsplan.oppdaterteFagomraader.forEach { fagomraade ->
            trekkFagomraadeRepository.updateTrekkFagomraade(
                trekkvedtakId = trekkvedtakId,
                trekkFagomraade = fagomraade.toTrekkFagomraade(trekkvedtakId.id),
                session = session,
            )
        }
        endringsplan.fjernedeFagomraader.forEach { fagomraade ->
            trekkFagomraadeRepository.deleteByTrekkvedtakIdAndFagomraadeKode(
                trekkvedtakId = trekkvedtakId,
                fagomraadekode = fagomraade.fagomraadeKode,
                session = session,
            )
        }
    }
}
