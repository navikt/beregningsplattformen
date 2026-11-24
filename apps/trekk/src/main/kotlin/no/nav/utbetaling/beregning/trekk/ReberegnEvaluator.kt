package no.nav.utbetaling.beregning.trekk

import kotlin.collections.intersect

import no.nav.utbetaling.beregning.trekk.domain.TrekkFagomraade
import no.nav.utbetaling.beregning.trekk.domain.Trekkvedtak
import no.nav.utbetaling.beregning.trekk.dto.Fagomraade
import no.nav.utbetaling.beregning.trekk.dto.Trekk

object ReberegnEvaluator {
    // ersom det er endring på saldobeløp, periodedatoer, periodestatus, trekkvedtakstatus eller fagområder vil det påvirke trekkberegningen.
    // De trekkvedtakene som OS sender vil inneholde kun de fagområder og trekkperioder som har løpenr = 9999. Normalt vil de også ha en feilregertreringskode = N (dvs ikke feilregistrert).
    // Ved patch-jobber på OS kan det forekomme at fagområder eller trekkperioder med løpenr = 9999 vil få feilregistreringskode = J (dvs feilregistrert). Dette må følgelig også håndteres på mottakssiden.
    fun erReberegningPaakrevd(
        eksisterendeTrekkVedtak: Trekkvedtak,
        endredeTrekkvedtak: Trekk,
    ): Boolean =
        erTrekkvedtakSaldoEllerGyldigTomEndret(eksisterendeTrekkVedtak, endredeTrekkvedtak) ||
            erTrekkperiodeendret(eksisterendeTrekkVedtak, endredeTrekkvedtak) ||
            erTrekkStatusEndret(eksisterendeTrekkVedtak, endredeTrekkvedtak)

    private fun erTrekkStatusEndret(
        eksisterendeTrekkVedtak: Trekkvedtak,
        endredeTrekkvedtak: Trekk,
    ): Boolean {
        val eksisterendeTrekkFagomraader: Map<String, TrekkFagomraade> = eksisterendeTrekkVedtak.fagomraader.associateBy { it.fagomraadeKode }
        val endredeTrekkFagomraader: Map<String, Fagomraade> =
            endredeTrekkvedtak.fagomraader.fagomraade
                .filter { it.lopenrFagomraade == 9999 }
                .associateBy { it.kodeFagomraade }

        val endretFagomraadeStatus =
            eksisterendeTrekkFagomraader.keys.intersect(endredeTrekkFagomraader.keys).any { fagomraadeKode ->
                val eksisterendeFagomraade = eksisterendeTrekkFagomraader.getValue(fagomraadeKode)
                val endretFagomraade = endredeTrekkFagomraader.getValue(fagomraadeKode)
                val endretFagomraadeErAktiv = endretFagomraade.feilRegFagomraade == "N"
                eksisterendeFagomraade.aktiv != endretFagomraadeErAktiv
            }

        val nyeFagomraader = endredeTrekkFagomraader.keys.any { it !in eksisterendeTrekkFagomraader }
        val fjernedeFagomraader = eksisterendeTrekkFagomraader.keys.any { it !in endredeTrekkFagomraader }
        val endretTrekkvedtakStatus =
            eksisterendeTrekkVedtak.trekkstatus.name
                .equals(endredeTrekkvedtak.trekkstatus, ignoreCase = true)
                .not()

        return nyeFagomraader || endretFagomraadeStatus || endretTrekkvedtakStatus || fjernedeFagomraader
    }

    private fun erTrekkperiodeendret(
        eksisterendeTrekkvedtak: Trekkvedtak,
        endretTrekkvedtak: Trekk,
    ): Boolean {
        val nyTrekkperiode =
            endretTrekkvedtak.perioder.periode.any { endretPeriod ->
                eksisterendeTrekkvedtak.trekkperioder.none { it.datoFom == endretPeriod.periodeFomDato && it.datoTom == endretPeriod.periodeTomDato }
            }
        val trekkperiodeEndret =
            eksisterendeTrekkvedtak.trekkperioder.any { eksisterendePeriod ->
                val endretPeriod =
                    endretTrekkvedtak.perioder.periode
                        .filter { it.lopenrPeriode == 9999 }
                        .find { it.periodeFomDato == eksisterendePeriod.datoFom && it.periodeTomDato == eksisterendePeriod.datoTom }
                        ?: return true

                val endretSats = eksisterendePeriod.sats != endretPeriod.sats

                val endretPeriodIsInactive = endretPeriod.feilRegPeriode.equals("J", true)

                val endretStatus = eksisterendePeriod.feilReg != endretPeriodIsInactive
                endretSats || endretStatus || nyTrekkperiode
            }

        return trekkperiodeEndret
    }

    private fun erTrekkvedtakSaldoEllerGyldigTomEndret(
        eksisterendeTrekkVedtak: Trekkvedtak,
        changedTrekkvedtak: Trekk,
    ) = eksisterendeTrekkVedtak.belopSaldo != changedTrekkvedtak.belopSaldo ||
        eksisterendeTrekkVedtak.gyldigTom != changedTrekkvedtak.datoGyldigTom
}
