package no.nav.sokos.dare.poc.beregning.beregningsmotor

import java.math.BigDecimal
import java.math.BigDecimal.ZERO
import java.time.LocalDate
import java.util.TreeMap

import no.nav.sokos.dare.poc.beregning.BeregningsBeloep
import no.nav.sokos.dare.poc.beregning.Dagbeloep
import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.beregning.Delberegningslinje
import no.nav.sokos.dare.poc.beregning.JustertTrekk
import no.nav.sokos.dare.poc.beregning.Kapasitet
import no.nav.sokos.dare.poc.beregning.toDagBeloepMap
import no.nav.sokos.dare.poc.util.BigDecimalUtils
import no.nav.sokos.dare.poc.util.DagBeloepMap
import no.nav.sokos.dare.poc.util.MutableDagBeloepMap

class KapasitetStyring(
    private val restKapasitet: MutableDagBeloepMap = MutableDagBeloepMap(),
) {
    fun addYtelse(dagsbeloep: List<Dagbeloep>) {
        dagsbeloep.forEach { dagbeloep ->
            if (dagbeloep.beloep.compareTo(ZERO) == 0) return@forEach
            restKapasitet[dagbeloep.dato] = restKapasitet[dagbeloep.dato] + dagbeloep.beloep
        }
    }

    fun applyTidligereUtbetalt(
        periode: DatoIntervall,
        beloep: BigDecimal,
    ) {
        reduserKapasitet(periode.hverdager, beloep)
    }

    fun adjustTrekk(
        periode: DatoIntervall,
        trekk: BeregningsBeloep,
    ): BeregningsBeloep {
        val current = current()
        val kapasitet =
            MutableDagBeloepMap(
                periode.dager.associateWith { dag -> current[dag] ?: ZERO }.toMutableMap(),
            )
        val (periodeBeloep, trekkPerDag) = trekk.toDagBeloepMap(periode)
        val restEtterDagligTrekk = dagbeloepstrekk(kapasitet, periode.dager, trekkPerDag)
        // Rest sum er delen av trekket som ikke ble dekket.
        val restSum = restEtterDagligTrekk.sum()
        val justertPeriodebeloep = periodeBeloep + restSum
        val restEtterPeriodeTrekk = enkeltBeloepstrekk(kapasitet, periode.dager, justertPeriodebeloep)

        if (restSum <= ZERO && restEtterPeriodeTrekk <= ZERO) {
            return trekk
        }

        return JustertTrekk(
            trekk = trekk,
            periodeBeloep = justertPeriodebeloep - restEtterPeriodeTrekk,
            justerteDagBeloep = trekkPerDag - restEtterDagligTrekk,
        )
    }

    fun consumeTrekk(
        periode: DatoIntervall,
        linje: Delberegningslinje,
    ) {
        if (linje.dagsbeloep.isNotEmpty()) {
            linje.dagsbeloep.forEach { dagbeloep ->
                val currentKapasitet = restKapasitet[dagbeloep.dato] ?: ZERO
                restKapasitet[dagbeloep.dato] = (currentKapasitet - dagbeloep.beloep.abs()).coerceAtLeast(ZERO)
            }
            return
        }

        if (linje.beloep.compareTo(ZERO) != 0) {
            reduserKapasitet(periode.hverdager, linje.beloep.abs())
        }
    }

    fun consumeSkatt(
        periode: DatoIntervall,
        beloep: BigDecimal,
    ) {
        reduserKapasitet(periode.hverdager, beloep)
    }

    fun snapshot(periode: DatoIntervall): Kapasitet {
        val current = current()
        return Kapasitet(
            dagbeloep =
                TreeMap<LocalDate, BigDecimal>().apply {
                    periode.dager.forEach { dag ->
                        put(dag, current[dag] ?: ZERO)
                    }
                },
            periodeBeloep = ZERO,
        )
    }

    fun current(): Map<LocalDate, BigDecimal> = restKapasitet.toMap()

    private fun reduserKapasitet(
        dager: List<LocalDate>,
        beloep: BigDecimal,
    ) {
        if (beloep <= ZERO) return

        val currentKapasitet = dager.map { restKapasitet[it] ?: ZERO }
        val kapasitetSum = currentKapasitet.sumOf { it }
        val trekkBeloep = minOf(beloep, kapasitetSum)
        if (trekkBeloep <= ZERO) return

        if (trekkBeloep >= kapasitetSum) {
            dager.forEach { dag ->
                restKapasitet[dag] = ZERO
            }
            return
        }

        val redusert = BigDecimalUtils.reverseWaterFill(currentKapasitet, trekkBeloep)
        dager.forEachIndexed { idx, dag ->
            restKapasitet[dag] = redusert[idx]
        }
    }

    private fun dagbeloepstrekk(
        kapasitet: MutableDagBeloepMap,
        dager: List<LocalDate>,
        trekkbeloepPerDag: DagBeloepMap,
    ): DagBeloepMap {
        val restbeloepPerDag = MutableDagBeloepMap(trekkbeloepPerDag)

        dager.forEach { dag ->
            val dagTrekkBeloep = trekkbeloepPerDag[dag]
            if (dagTrekkBeloep <= ZERO) return@forEach
            val dagKapasitet = kapasitet[dag]

            if (dagKapasitet <= ZERO) {
                restbeloepPerDag[dag] = dagTrekkBeloep
                return@forEach
            }

            if (dagKapasitet >= dagTrekkBeloep) {
                kapasitet[dag] = dagKapasitet - dagTrekkBeloep
                restbeloepPerDag[dag] = ZERO
            } else {
                kapasitet[dag] = ZERO
                restbeloepPerDag[dag] = dagTrekkBeloep - dagKapasitet
            }
        }

        return DagBeloepMap(restbeloepPerDag)
    }

    private fun enkeltBeloepstrekk(
        kapasitet: MutableDagBeloepMap,
        dager: List<LocalDate>,
        trekkBeloep: BigDecimal,
    ): BigDecimal {
        if (trekkBeloep <= ZERO) return ZERO

        val dagbeloepSum = kapasitet.values.sumOf { it }
        if (dagbeloepSum <= trekkBeloep) {
            val rest = trekkBeloep - dagbeloepSum
            dager.forEach { dag ->
                kapasitet[dag] = ZERO
            }
            return rest
        }

        val beloepPerDag = dager.map { dag -> kapasitet.getOrDefault(dag, ZERO) }
        val perDagEtterTrekk = BigDecimalUtils.reverseWaterFill(beloepPerDag, trekkBeloep)
        dager.forEachIndexed { index, dag ->
            kapasitet[dag] = perDagEtterTrekk[index]
        }
        return ZERO
    }
}
