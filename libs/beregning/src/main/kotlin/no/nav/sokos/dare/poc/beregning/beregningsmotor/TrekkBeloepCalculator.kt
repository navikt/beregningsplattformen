package no.nav.sokos.dare.poc.beregning.beregningsmotor

import java.math.BigDecimal
import java.math.BigDecimal.ZERO
import java.math.RoundingMode
import java.time.LocalDate

import no.nav.sokos.dare.poc.beregning.BeloepPerDag
import no.nav.sokos.dare.poc.beregning.BeregningsBeloep
import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.beregning.Enkeltbeloep
import no.nav.sokos.dare.poc.beregning.fordeler.SaldoBoetter
import no.nav.sokos.dare.poc.beregning.fordeler.TrekkvedtakForBeregning
import no.nav.sokos.dare.poc.trekk.SatsType

private val HUNDRE: BigDecimal = BigDecimal(100L)

class TrekkBeloepCalculator {
    fun calculate(
        trekkvedtak: TrekkvedtakForBeregning,
        periode: DatoIntervall,
        trekkGrunnlag: BigDecimal,
        saldoBoetter: SaldoBoetter,
        halverMaanedssats: Boolean,
    ): BeregningsBeloep {
        val trekkperiode = trekkvedtak.trekkperiode
        val satstype = trekkperiode.satstype
        val sats = trekkperiode.sats
        val gjenstaaendeSaldo = saldoBoetter.finnRestSaldo(trekkvedtak.id)

        val dagBeloep: Map<LocalDate, BigDecimal> =
            when (satstype) {
                SatsType.D ->
                    periode.hverdager
                        .filter { trekkperiode.periode.contains(it) }
                        .associateWith { sats }

                SatsType.M, SatsType.P -> emptyMap()
            }

        val periodeBeloep: BigDecimal =
            when (satstype) {
                SatsType.P ->
                    trekkGrunnlag
                        .multiply(sats)
                        .divide(HUNDRE, 2, RoundingMode.HALF_UP)
                        .setScale(0, RoundingMode.HALF_UP)

                SatsType.M ->
                    if (halverMaanedssats) {
                        sats.divide(BigDecimal(2), 0, RoundingMode.HALF_UP)
                    } else {
                        sats
                    }

                SatsType.D -> ZERO
            }

        val totalBeloep = dagBeloep.values.sumOf { it } + periodeBeloep
        val begrensetTotal =
            if (trekkvedtak.erSaldoTrekk() && gjenstaaendeSaldo != null && totalBeloep > gjenstaaendeSaldo) {
                gjenstaaendeSaldo
            } else {
                totalBeloep
            }

        if (begrensetTotal < totalBeloep && totalBeloep > ZERO) {
            val faktor = begrensetTotal.divide(totalBeloep, 10, RoundingMode.HALF_UP)
            val justertDagBeloep = dagBeloep.mapValues { (_, v) -> v.multiply(faktor).setScale(0, RoundingMode.HALF_UP) }
            val justertDagSum = justertDagBeloep.values.sumOf { it }
            val justertPeriode = begrensetTotal - justertDagSum
            return calculateBeregningsBeloep(justertDagBeloep, justertPeriode)
        }

        return calculateBeregningsBeloep(dagBeloep, periodeBeloep)
    }

    private fun calculateBeregningsBeloep(
        dagBeloep: Map<LocalDate, BigDecimal>,
        periodeBeloep: BigDecimal,
    ): BeregningsBeloep =
        if (dagBeloep.isEmpty() || dagBeloep.values.all { it.compareTo(BigDecimal.ZERO) == 0 }) {
            Enkeltbeloep(periodeBeloep)
        } else {
            BeloepPerDag(dagBeloep)
        }
}
