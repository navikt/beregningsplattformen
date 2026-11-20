package no.nav.utbetaling.beregning.trekk

import no.nav.utbetaling.beregning.trekk.domain.TrekkFagomraade
import no.nav.utbetaling.beregning.trekk.domain.Trekkperiode
import no.nav.utbetaling.beregning.trekk.domain.Trekkvedtak
import no.nav.utbetaling.beregning.trekk.dto.Fagomraade
import no.nav.utbetaling.beregning.trekk.dto.Periode
import no.nav.utbetaling.beregning.trekk.dto.Trekk

class TrekkvedtakEndringsplanlegger {
    fun planlegg(
        mottatteTrekk: Trekk,
        eksisterendeTrekk: Trekkvedtak? = null,
    ): TrekkvedtakEndringsplan {
        val eksisterendePerioder = eksisterendeTrekk?.trekkperioder.orEmpty()
        val eksisterendePeriodenoekler = eksisterendePerioder.map { it.datoFom to it.datoTom }.toSet()
        val mottattePeriodenoekler =
            mottatteTrekk.perioder.periode
                .map { it.periodeFomDato to it.periodeTomDato }
                .toSet()
        val (oppdatertePerioder, nyePerioder) =
            mottatteTrekk.perioder.periode
                .filter { it.lopenrPeriode == 9999 }
                .partition { (it.periodeFomDato to it.periodeTomDato) in eksisterendePeriodenoekler }

        val eksisterendeFagomraader = eksisterendeTrekk?.fagomraader.orEmpty()
        val eksisterendeFagomraadekoder = eksisterendeFagomraader.map { it.fagomraadeKode }.toSet()
        val mottatteFagomraadekoder =
            mottatteTrekk.fagomraader.fagomraade
                .map { it.kodeFagomraade }
                .toSet()
        val (oppdaterteFagomraader, nyeFagomraader) =
            mottatteTrekk.fagomraader.fagomraade
                .filter { it.lopenrFagomraade == 9999 }
                .partition { it.kodeFagomraade in eksisterendeFagomraadekoder }

        return TrekkvedtakEndringsplan(
            nyePerioder = nyePerioder,
            oppdatertePerioder = oppdatertePerioder,
            fjernedePerioder = eksisterendePerioder.filter { (it.datoFom to it.datoTom) !in mottattePeriodenoekler },
            nyeFagomraader = nyeFagomraader,
            oppdaterteFagomraader = oppdaterteFagomraader,
            fjernedeFagomraader = eksisterendeFagomraader.filter { it.fagomraadeKode !in mottatteFagomraadekoder },
        )
    }
}

data class TrekkvedtakEndringsplan(
    val nyePerioder: List<Periode>,
    val oppdatertePerioder: List<Periode>,
    val fjernedePerioder: List<Trekkperiode>,
    val nyeFagomraader: List<Fagomraade>,
    val oppdaterteFagomraader: List<Fagomraade>,
    val fjernedeFagomraader: List<TrekkFagomraade>,
)
