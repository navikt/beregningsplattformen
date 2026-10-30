package no.nav.sokos.dare.poc.beregning.beregningsmotor

import java.math.BigDecimal
import java.math.BigDecimal.ZERO
import java.math.RoundingMode
import java.time.LocalDate

import no.nav.sokos.dare.poc.beregning.BeloepPerDag
import no.nav.sokos.dare.poc.beregning.BeregningsBeloep
import no.nav.sokos.dare.poc.beregning.Delberegningslinje
import no.nav.sokos.dare.poc.beregning.Enkeltbeloep
import no.nav.sokos.dare.poc.beregning.fordeler.Delperiodegrunnlag
import no.nav.sokos.dare.poc.beregning.fordeler.SatsBoetter
import no.nav.sokos.dare.poc.beregning.fordeler.TrekkvedtakForBeregning
import no.nav.sokos.dare.poc.trekk.SatsType
import no.nav.sokos.dare.poc.trekk.Trekkberegningstype

class SatsBoetteLimiter {
    fun limit(
        justertTrekk: BeregningsBeloep,
        trekkvedtak: TrekkvedtakForBeregning,
        satsBoetter: SatsBoetter,
        delperiode: Delperiodegrunnlag,
    ): BeregningsBeloep {
        val boetteIds = trekkvedtak.trekkperiode.satsBoetteIds()
        if (boetteIds.isEmpty()) return justertTrekk

        return when (trekkvedtak.trekkperiode.satstype) {
            SatsType.D ->
                when (trekkvedtak.trekktype.trekkberegningstype) {
                    Trekkberegningstype.UTBDATO -> {
                        val totalRestSats = boetteIds.sumOf { satsBoetter.restSats(it) ?: BigDecimal.valueOf(Long.MAX_VALUE) }
                        val cappedPeriode = minOf(justertTrekk.sum, totalRestSats)
                        this@SatsBoetteLimiter.calculateBeregningsBeloep(emptyMap(), cappedPeriode)
                    }

                    Trekkberegningstype.YTELPER -> {
                        val cappedDagBeloep =
                            delperiode.periode.hverdager.associateWith { dag ->
                                val boetteId = boetteIds.firstOrNull { it.boette == dag.toString() }
                                val restSats = boetteId?.let { satsBoetter.restSats(it) }
                                val beloep = justertTrekk.forDag(dag)
                                if (restSats != null) minOf(beloep, restSats) else beloep
                            }
                        this@SatsBoetteLimiter.calculateBeregningsBeloep(cappedDagBeloep, justertTrekk.periodeBeloep)
                    }
                }

            SatsType.M -> {
                val totalRestSats = boetteIds.sumOf { satsBoetter.restSats(it) ?: BigDecimal.valueOf(Long.MAX_VALUE) }
                val cappedPeriode = minOf(justertTrekk.periodeBeloep, totalRestSats)
                this@SatsBoetteLimiter.calculateBeregningsBeloep(emptyMap(), cappedPeriode)
            }

            SatsType.P -> justertTrekk
        }
    }

    fun consume(
        trekkDelberegningslinje: Delberegningslinje,
        trekkvedtak: TrekkvedtakForBeregning,
        satsBoetter: SatsBoetter,
    ) {
        val boetteIds = trekkvedtak.trekkperiode.satsBoetteIds()
        if (boetteIds.isEmpty()) return

        when (trekkvedtak.trekkperiode.satstype) {
            SatsType.D ->
                when (trekkvedtak.trekktype.trekkberegningstype) {
                    Trekkberegningstype.UTBDATO -> {
                        val totalUsed = trekkDelberegningslinje.beloep
                        val boetteId = boetteIds.first()
                        if (satsBoetter.restSats(boetteId) != null) {
                            satsBoetter.brukSats(boetteId, totalUsed)
                        }
                    }

                    Trekkberegningstype.YTELPER -> {
                        trekkDelberegningslinje.dagsbeloep.forEach { dagbeloep ->
                            val boetteId = boetteIds.firstOrNull { it.boette == dagbeloep.dato.toString() }
                            if (boetteId != null && satsBoetter.restSats(boetteId) != null) {
                                satsBoetter.brukSats(boetteId, dagbeloep.beloep)
                            }
                        }
                    }
                }

            SatsType.M -> {
                val totalUsed = trekkDelberegningslinje.beloep
                if (boetteIds.size == 1) {
                    val boetteId = boetteIds.first()
                    if (satsBoetter.restSats(boetteId) != null) {
                        satsBoetter.brukSats(boetteId, totalUsed)
                    }
                } else {
                    val totalRest = boetteIds.sumOf { satsBoetter.restSats(it) ?: ZERO }
                    if (totalRest > ZERO) {
                        boetteIds.forEach { boetteId ->
                            val rest = satsBoetter.restSats(boetteId) ?: ZERO
                            val share = totalUsed.multiply(rest).divide(totalRest, 0, RoundingMode.HALF_UP)
                            if (rest > ZERO) {
                                satsBoetter.brukSats(boetteId, minOf(share, rest))
                            }
                        }
                    }
                }
            }

            SatsType.P -> {}
        }
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
