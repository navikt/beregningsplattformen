package no.nav.sokos.dare.poc.beregning.fordeler

import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

import no.nav.sokos.dare.poc.beregning.Beregning
import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.beregning.DelberegningType
import no.nav.sokos.dare.poc.oppdrag.Oppdrag
import no.nav.sokos.dare.poc.oppdrag.OppdragId
import no.nav.sokos.dare.poc.trekk.TrekkvedtakId
import no.nav.sokos.dare.poc.util.BigDecimalUtils

internal fun relaterteBoundaries(
    relaterteBeregninger: List<Beregning>,
    oppdrag: List<Oppdrag>,
): List<DatoIntervall> = relaterteBoundaries(relaterteBeregninger, oppdrag.mapNotNull { it.id }.toSet())

fun relaterteBoundaries(
    relaterteBeregninger: List<Beregning>,
    oppdragIds: Set<OppdragId>,
): List<DatoIntervall> =
    relaterteBeregninger
        .flatMap { it.delberegning }
        .filter { it.oppdragId in oppdragIds }
        .filter { delberegning -> delberegning.linjer.any { it.type == DelberegningType.YTELSE } }
        .map { it.datoIntervall }

internal fun filterRelaterteTilOppdrag(
    relaterteBeregninger: List<Beregning>,
    oppdrag: Oppdrag,
    periode: DatoIntervall,
): List<Beregning> {
    val oppdragId = oppdrag.id ?: return emptyList()
    return filterRelaterteTilOppdrag(relaterteBeregninger, oppdragId, periode)
}

fun filterRelaterteTilOppdrag(
    relaterteBeregninger: List<Beregning>,
    oppdragId: OppdragId,
    periode: DatoIntervall,
): List<Beregning> =
    relaterteBeregninger.mapNotNull { beregning ->
        val relevantDelberegninger =
            beregning.delberegning.filter { delberegning ->
                delberegning.oppdragId == oppdragId &&
                    delberegning.datoIntervall.overlaps(periode) &&
                    delberegning.linjer.any { it.type == DelberegningType.YTELSE }
            }
        if (relevantDelberegninger.isEmpty()) {
            null
        } else {
            beregning.copy(delberegning = relevantDelberegninger)
        }
    }

internal fun beregnTrukketSaldoFraRelaterteBeregninger(
    trekkvedtakId: TrekkvedtakId,
    relaterteBeregninger: List<Beregning>,
): BigDecimal =
    relaterteBeregninger
        .flatMap { it.delberegning }
        .flatMap { it.linjer }
        .filter { it.type == DelberegningType.TREKK && it.trekkvedtakId == trekkvedtakId }
        .sumOf { it.beloep.negate() }

internal fun beregnTrukketSatsFraRelaterteBeregningerYtelper(
    trekkvedtakId: TrekkvedtakId,
    boettePeriode: DatoIntervall,
    relaterteBeregninger: List<Beregning>,
): BigDecimal =
    relaterteBeregninger
        .flatMap { it.delberegning }
        .filter { it.datoIntervall.overlaps(boettePeriode) }
        .flatMap { delberegning ->
            delberegning.linjer
                .filter { it.type == DelberegningType.TREKK && it.trekkvedtakId == trekkvedtakId }
                .map { linje -> delberegning to linje }
        }.sumOf { (delberegning, linje) ->
            if (linje.dagsbeloep.isNotEmpty()) {
                linje.dagsbeloep
                    .filter { boettePeriode.contains(it.dato) }
                    .sumOf { it.beloep }
                    .negate()
            } else {
                val sourceHverdager = delberegning.datoIntervall.hverdager
                val overlapIndices = sourceHverdager.indices.filter { boettePeriode.contains(sourceHverdager[it]) }
                BigDecimalUtils.prorater(linje.beloep.negate(), sourceHverdager.size, overlapIndices)
            }
        }

/**
 * For UTBDATO-trekk: finner allerede trukket beløp fra relaterte beregninger
 * ved å sjekke overfoeres-datoen (utbetalingsdato) i stedet for ytelsesperioden.
 */
internal fun beregnTrukketSatsFraRelaterteBeregningerUtbdato(
    trekkvedtakId: TrekkvedtakId,
    overfoeringsdato: String,
    relaterteBeregninger: List<Beregning>,
): BigDecimal =
    relaterteBeregninger
        .filter {
            it.overfoeres?.toString() == overfoeringsdato ||
                (
                    erMaanedBoette(overfoeringsdato) &&
                        (it.overfoeres?.let { dato -> YearMonth.from(dato).toString() == overfoeringsdato } == true)
                )
        }.flatMap { it.delberegning }
        .flatMap { delberegning ->
            delberegning.linjer
                .filter { it.type == DelberegningType.TREKK && it.trekkvedtakId == trekkvedtakId }
        }.sumOf { it.beloep.negate() }

internal fun boetteTilPeriode(
    boette: String,
    trekkperiode: DatoIntervall,
): DatoIntervall =
    when {
        boette.length == 10 -> {
            DatoIntervall(LocalDate.parse(boette), LocalDate.parse(boette))
        }

        else -> {
            val yearMonth = YearMonth.parse(boette)
            val fom = maxOf(yearMonth.atDay(1), trekkperiode.fom)
            val tom = minOf(yearMonth.atEndOfMonth(), trekkperiode.tom ?: yearMonth.atEndOfMonth())
            DatoIntervall(fom, tom)
        }
    }

/** Sjekker om en bøtte-nøkkel representerer en måned (format "YYYY-MM") */
internal fun erMaanedBoette(boette: String): Boolean = boette.length == 7
