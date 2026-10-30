package no.nav.sokos.dare.poc.beregning.beregningsmotor

import java.math.BigDecimal
import java.math.BigDecimal.ZERO
import java.math.RoundingMode

import no.nav.sokos.dare.poc.beregning.EkstraInfo
import no.nav.sokos.dare.poc.beregning.EkstraInfoDsl.verdi
import no.nav.sokos.dare.poc.beregning.TekstEkstraInfo
import no.nav.sokos.dare.poc.beregning.fordeler.BeregningsScratchpad
import no.nav.sokos.dare.poc.beregning.fordeler.Delperiodegrunnlag
import no.nav.sokos.dare.poc.beregning.skatt.SkattTabellDagerPeriode
import no.nav.sokos.dare.poc.beregning.skatt.TabellkortPeriodetype
import no.nav.sokos.dare.poc.skattekort.domain.Forskuddstrekk

internal data class TabellkortPeriode(
    val delperiode: Delperiodegrunnlag,
    val scratchpad: BeregningsScratchpad,
    val skatteGrunnlag: BigDecimal,
    val forskuddstrekk: Forskuddstrekk,
) {
    fun tabellkortPeriodetype() =
        delperiode.fagomraade
            .tabellkortPeriodetype

    fun tilSkattTabellDagerPerioder(): List<SkattTabellDagerPeriode> =
        when (tabellkortPeriodetype()) {
            TabellkortPeriodetype.VERDIBEREGNET -> tilVerdiberegnetSkattTabellDagerPerioder()
            TabellkortPeriodetype.KALENDERBEREGNET -> listOf(tilSkattTabellDagerPeriode())
        }

    private fun tilVerdiberegnetSkattTabellDagerPerioder(): List<SkattTabellDagerPeriode> =
        delperiode.periode.hverdager
            .groupBy { dag -> requireNotNull(delperiode.vedtakSatsForDag(dag)) { "Oppdrag mangler vedtakssats for dag $dag" }.stripTrailingZeros() }
            .map { (vedtakssats, arbeidsdager) ->
                val bruttoYtelse = arbeidsdager.sumOf { dag -> delperiode.forDag(dag) }
                SkattTabellDagerPeriode(
                    bruttoYtelse = bruttoYtelse,
                    vedtakssats = vedtakssats,
                    antallKalenderdager = arbeidsdager.size,
                    antallArbeidsdager = arbeidsdager.size,
                )
            }

    fun tilSkattTabellDagerPeriode(): SkattTabellDagerPeriode =
        delperiode.periode.hverdager.let { arbeidsdager ->
            val sumArbeidsdagSatser = arbeidsdager.sumOf { dag -> delperiode.vedtakSatsForDag(dag) }
            SkattTabellDagerPeriode(
                bruttoYtelse = skatteGrunnlag,
                vedtakssats = if (arbeidsdager.isEmpty()) ZERO else sumArbeidsdagSatser.divide(BigDecimal(arbeidsdager.size), 10, RoundingMode.HALF_UP),
                antallKalenderdager = delperiode.periode.dager.size,
                antallArbeidsdager = arbeidsdager.size,
            )
        }
}

internal fun List<TabellkortPeriode>.beregnTabellkortDager(): Pair<Int, List<TekstEkstraInfo>> =
    groupBy { it.tabellkortPeriodetype() }
        .values
        .fold(0 to emptyList()) { (totalDager, totalEkstraInfo), perioder ->
            val periodetype = perioder.first().tabellkortPeriodetype()
            val perioderForBeregning = perioder.flatMap { it.tilSkattTabellDagerPerioder() }

            when (periodetype) {
                TabellkortPeriodetype.VERDIBEREGNET -> {
                    val partitioner = perioderForBeregning.groupBy { it.vedtakssats.stripTrailingZeros() }.values
                    val partitionEkstraInfo =
                        partitioner.map { partition ->
                            partition.tilVerdiberegnetPartiInfo()
                        }
                    val dager = partitioner.sumOf { partition -> TabellkortPeriodetype.VERDIBEREGNET.beregnAntallDager(partition) }
                    (totalDager + dager) to (totalEkstraInfo + partitionEkstraInfo)
                }

                TabellkortPeriodetype.KALENDERBEREGNET -> {
                    val dager = periodetype.beregnAntallDager(perioderForBeregning)
                    (totalDager + dager) to totalEkstraInfo
                }
            }
        }

internal fun List<SkattTabellDagerPeriode>.tilVerdiberegnetPartiInfo(): TekstEkstraInfo {
    val totalArbeidsdager = sumOf { it.antallArbeidsdager }
    require(totalArbeidsdager > 0) { "Kan ikke beregne verdiberegnet partiinformasjon uten arbeidsdager" }
    val bruttoYtelse = sumOf { it.bruttoYtelse }
    val vedtakssats =
        sumOf { it.vedtakssats * BigDecimal(it.antallArbeidsdager) }
            .divide(BigDecimal(totalArbeidsdager), 10, RoundingMode.HALF_UP)
            .stripTrailingZeros()
    val skattedager = TabellkortPeriodetype.VERDIBEREGNET.beregnAntallDager(this)

    return TekstEkstraInfo(
        grunn = EkstraInfo.Reasons.SKATT_TABELL_VERDIBEREGNET_PARTISJON,
        verdier =
            mapOf(
                "bruttoYtelse" verdi bruttoYtelse,
                "vedtakssats" verdi vedtakssats,
                "arbeidsdager" verdi totalArbeidsdager,
                "skattedager" verdi skattedager,
            ),
    )
}
