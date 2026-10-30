package no.nav.utbetaling.beregningsflyt.fordeler

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

import no.nav.sokos.dare.poc.beregning.Beregning
import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.beregning.fordeler.relaterteBoundaries
import no.nav.sokos.dare.poc.oppdrag.OppdragId
import no.nav.sokos.dare.poc.trekk.SatsType
import no.nav.sokos.dare.poc.trekk.Trekkvedtak
import no.nav.utbetaling.beregning.oppdrag.Opplysninger
import no.nav.utbetaling.beregning.tid.Periode
import no.nav.utbetaling.beregning.tid.Tidslinjesamling
import no.nav.utbetaling.beregningsflyt.identitet.Beregningsidentitet

internal fun Periode.somDatoIntervall(): DatoIntervall = DatoIntervall(fom, (this as? Periode.LukketPeriode)?.tom)

internal fun delVedGrenser(
    periode: DatoIntervall,
    grenser: List<DatoIntervall>,
): List<DatoIntervall> {
    val tom = requireNotNull(periode.tom) { "Expected a finite calculation interval: $periode" }
    require(tom >= periode.fom) { "Invalid calculation interval: $periode" }
    val starter = sortedSetOf(periode.fom)
    grenser.forEach { grense ->
        if (grense.fom > periode.fom && grense.fom <= tom) starter.add(grense.fom)
        grense.tom?.let { slutt ->
            if (slutt >= periode.fom && slutt < tom) starter.add(slutt.plusDays(1))
        }
    }
    val datoer = starter.toList()
    return datoer.mapIndexed { index, fom -> DatoIntervall(fom, datoer.getOrNull(index + 1)?.minusDays(1) ?: tom) }
}

internal fun delperioder(
    oppdrag: Tidslinjesamling<UUID, Opplysninger>,
    periode: DatoIntervall,
    identitet: Beregningsidentitet,
    oppdragId: OppdragId,
    trekk: List<Trekkvedtak>,
    beregnet: LocalDateTime,
    relaterteBeregninger: List<Beregning>,
): List<DatoIntervall> {
    val kildegrenser = oppdrag.tidslinjer.values.flatMap { tidslinje -> tidslinje.map { it.first.somDatoIntervall() } }
    val relevanteKildeperioder = mutableListOf<DatoIntervall>()
    var forrigeSignatur: Map<UUID, List<Any?>>? = null
    delVedGrenser(periode, kildegrenser).forEach { kandidat ->
        val signatur = oppdrag[kandidat.fom].mapValues { it.value.beregningssignatur(identitet) }
        if (signatur == forrigeSignatur) {
            val forrige = relevanteKildeperioder.removeAt(relevanteKildeperioder.lastIndex)
            relevanteKildeperioder.add(DatoIntervall(forrige.fom, kandidat.tom))
        } else {
            relevanteKildeperioder.add(kandidat)
        }
        forrigeSignatur = signatur
    }
    val trekkgrenser =
        trekk.flatMap { it.trekkperioder }.flatMap { tp ->
            val overlapp = tp.periode.intersection(periode)
            when {
                overlapp == null -> emptyList()
                tp.satstype == SatsType.M -> delVedGrenser(overlapp, overlapp.months())
                else -> listOf(overlapp)
            }
        }
    val desemberIFjor = LocalDate.of(beregnet.year - 1, 12, 1)
    val skatteGrenser =
        buildList {
            if (periode.contains(desemberIFjor)) add(DatoIntervall(periode.fom, desemberIFjor))
            add(DatoIntervall(LocalDate.of(beregnet.year, 1, 1), LocalDate.of(beregnet.year, 1, 31)))
            addAll(periode.years())
        }
    return delVedGrenser(
        periode,
        relevanteKildeperioder + trekkgrenser + skatteGrenser + relaterteBoundaries(relaterteBeregninger, setOf(oppdragId)),
    ).filter { oppdrag[it.fom].isNotEmpty() }
}
