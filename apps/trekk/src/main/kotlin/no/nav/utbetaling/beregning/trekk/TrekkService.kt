package no.nav.utbetaling.beregning.trekk

import javax.sql.DataSource

import kotliquery.TransactionalSession

import no.nav.utbetaling.beregning.log.Log
import no.nav.utbetaling.beregning.trekk.config.DatabaseConfig
import no.nav.utbetaling.beregning.trekk.dto.Trekk
import no.nav.utbetaling.beregning.trekk.repository.TrekkFagomraadeRepository
import no.nav.utbetaling.beregning.trekk.repository.TrekkperiodeRepository
import no.nav.utbetaling.beregning.trekk.repository.TrekkvedtakHistorikkRepository
import no.nav.utbetaling.beregning.trekk.repository.TrekkvedtakRepository
import no.nav.utbetaling.beregning.trekk.utils.SqlUtils.transaction

class TrekkService(
    private val dataSource: DataSource = DatabaseConfig.dataSource,
    private val trekkvedtakHistorikkRepository: TrekkvedtakHistorikkRepository = TrekkvedtakHistorikkRepository(),
    private val trekkvedtakRepository: TrekkvedtakRepository = TrekkvedtakRepository(),
    private val trekkperiodeRepository: TrekkperiodeRepository = TrekkperiodeRepository(),
    private val trekkFagomraadeRepository: TrekkFagomraadeRepository = TrekkFagomraadeRepository(),
) {
    fun createOrUpdateTrekkvedtakFromOs(
        message: String,
        trekk: Trekk,
    ) {
        dataSource.transaction { session ->
            trekkvedtakHistorikkRepository.insertTrekkvedtak(message, session)
            when (trekkvedtakRepository.existsByOsId(trekk.trekkvedtakId!!, session)) {
                true -> updateTrekkvedtak(trekk, session)
                false -> createTrekkvedtak(trekk, session)
            }
        }
    }

    fun createTrekkvedtak(
        newTrekkvedtak: Trekk,
        tx: TransactionalSession,
    ) {
        Log.debug("Oppretter nytt trekkvedtak for OS-trekkId: ${newTrekkvedtak.trekkvedtakId}")
        /*val aktoerId = aktoerService.findOrCreateByOffNr(tx, newTrekkvedtak.debitorId, "Person med trekk fra OS")
        val trekkId =
            trekkvedtakRepository.insertTrekkvedtak(newTrekkvedtak.toTrekkVedtak(), aktoerId.first.id, tx)
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
        Log.debug("Trekkvedtak opprettet med id: $trekkId")*/
    }

    fun updateTrekkvedtak(
        changedTrekkvedtak: Trekk,
        tx: TransactionalSession,
    ): Trekk? {
        val existingTrekkvedtak =
            trekkvedtakRepository.findByEksternTrekkId(changedTrekkvedtak.trekkvedtakId!!, tx)
                ?: throw IllegalStateException("Kunne ikke finne trekkvedtak for eksternTrekkId ${changedTrekkvedtak.trekkvedtakId} og debitorId ${changedTrekkvedtak.debitorId}")
        // TODO: endre trekkvedtak basert på mottatt endringsgrunnlag fra Oppdrag Z
        // return createUpdatedTrekkvedtakFromOs(existingTrekkvedtak, changedTrekkvedtak, tx)
        return null
    }
}
