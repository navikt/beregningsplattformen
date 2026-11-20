package no.nav.utbetaling.beregning.trekk

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi

import no.nav.utbetaling.beregning.log.Log
import no.nav.utbetaling.beregning.trekk.config.DatabaseConfig
import no.nav.utbetaling.beregning.trekk.dto.Trekk
import no.nav.utbetaling.beregning.trekk.repository.TrekkFagomraadeRepository
import no.nav.utbetaling.beregning.trekk.repository.TrekkperiodeRepository
import no.nav.utbetaling.beregning.trekk.repository.TrekkvedtakHistorikkRepository
import no.nav.utbetaling.beregning.trekk.repository.TrekkvedtakRepository

class TrekkService(
    private val jdbi: Jdbi = DatabaseConfig.jdbi,
    private val trekkvedtakHistorikkRepository: TrekkvedtakHistorikkRepository = TrekkvedtakHistorikkRepository(),
    private val trekkvedtakRepository: TrekkvedtakRepository = TrekkvedtakRepository(),
    private val trekkperiodeRepository: TrekkperiodeRepository = TrekkperiodeRepository(),
    private val trekkFagomraadeRepository: TrekkFagomraadeRepository = TrekkFagomraadeRepository(),
) {
    fun createOrUpdateTrekkvedtakFromOs(
        message: String,
        trekk: Trekk,
    ) {
        jdbi.useTransaction<Exception> { tx ->
            trekkvedtakHistorikkRepository.insertTrekkvedtak(message, tx)
            when (trekkvedtakRepository.existsByOsId(trekk.trekkvedtakId!!, tx)) {
                true -> updateTrekkvedtak(trekk, tx)
                false -> createTrekkvedtak(trekk, tx)
            }
        }
    }

    fun createTrekkvedtak(
        newTrekkvedtak: Trekk,
        tx: Handle,
    ) {
        Log.info("Oppretter nytt trekkvedtak for OS-trekkId: ${newTrekkvedtak.trekkvedtakId}")
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
        tx: Handle,
    ): Trekk? {
        Log.info("Oppdaterer trekkvedtak for OS-trekkId: ${changedTrekkvedtak.trekkvedtakId}")
        val existingTrekkvedtak =
            trekkvedtakRepository.findByEksternTrekkId(changedTrekkvedtak.trekkvedtakId!!, tx)
                ?: throw IllegalStateException("Kunne ikke finne trekkvedtak for eksternTrekkId ${changedTrekkvedtak.trekkvedtakId} og debitorId ${changedTrekkvedtak.debitorId}")
        // TODO: endre trekkvedtak basert på mottatt endringsgrunnlag fra Oppdrag Z
        // return createUpdatedTrekkvedtakFromOs(existingTrekkvedtak, changedTrekkvedtak, tx)
        return null
    }
}
