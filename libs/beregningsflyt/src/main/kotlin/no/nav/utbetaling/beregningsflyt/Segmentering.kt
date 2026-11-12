package no.nav.utbetaling.beregningsflyt

import no.nav.utbetaling.beregning.oppdrag.Avventingsårsak
import no.nav.utbetaling.beregning.oppdrag.Kidnummer
import no.nav.utbetaling.beregning.oppdrag.Oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.Opplysninger
import no.nav.utbetaling.beregning.oppdrag.UtbetalesTil
import no.nav.utbetaling.beregning.tid.Periode
import no.nav.utbetaling.beregning.tid.Tidslinje

internal data class Segmentbygger(
    val utbetaltTil: UtbetalesTil?,
    val kidnummer: Kidnummer?,
    val avventingsårsak: Avventingsårsak?,
    val tidslinje: Tidslinje<List<Opplysninger>>,
)

/**
 * Masserer oppdragstidslinjen til et format som gjør det enklere å
 * opprette rett segment med rett delperioder slik beregningsmotoren forventer.
 *
 * En Segmentbygger -> Et BeregningsSegment
 * Segmentbyggerens tidslinje er grunnlaget for delperiodene i et BeregningsSegment.
 *    Men det må også hensyntas oppsplitting på andre nivåer, som skatt, trekk, år etc.
 *    før delperiodene kan opprettes.
 *
 * Antall opplysninger for en periode i tidslinjen gjenspeiler hvor mange ytelseslinjer
 * som skal være i delperioden.
 */
internal object Segmentering {
    fun segmentbyggere(
        oppdragstidslinje: Oppdragstidslinje,
        beregningsperiode: Periode.LukketPeriode,
    ): List<Segmentbygger> {
        val ferdigeSegmentbyggere = mutableListOf<Segmentbygger>()
        var aktiveSegmentbyggere = emptyMap<Segmentgruppering, Segmentbygger>()

        // Bygger segmenter basert på endringer i segmentgrupperingene
        oppdragstidslinje.perioderIterator().forEach { (periode, opplysningerIPerioden) ->
            val begrensetPeriode = periode.overlappendePeriode(beregningsperiode) ?: return@forEach
            val perGruppering = opplysningerIPerioden.groupBy { it.segmentgruppering() }

            // Grupperinger som ikke finnes i denne perioden er ferdige
            ferdigeSegmentbyggere += aktiveSegmentbyggere.filterKeys { it !in perGruppering }.values

            // Resten fortsetter, eller starter et nytt segment
            aktiveSegmentbyggere =
                perGruppering.mapValues { (gruppering, opplysninger) ->
                    val tidslinje = nyTidslinje(begrensetPeriode, opplysninger)
                    aktiveSegmentbyggere[gruppering]?.let { it.copy(tidslinje = it.tidslinje + tidslinje) }
                        ?: Segmentbygger(gruppering.utbetaltTil, gruppering.kidnummer, gruppering.avventingsårsak, tidslinje)
                }
        }

        return ferdigeSegmentbyggere + aktiveSegmentbyggere.values
    }

    private data class Segmentgruppering(
        val utbetaltTil: UtbetalesTil?,
        val kidnummer: Kidnummer?,
        val avventingsårsak: Avventingsårsak?,
    )

    private fun Opplysninger.segmentgruppering() =
        Segmentgruppering(
            utbetaltTil = utbetalesTil?.opplysning,
            kidnummer = kidnummer?.opplysning,
            avventingsårsak = avventingsårsak?.opplysning,
        )

    private fun nyTidslinje(
        periode: Periode,
        opplysninger: List<Opplysninger>,
    ) = Tidslinje.Builder<List<Opplysninger>>().periode(periode, opplysninger).build()

    private operator fun Tidslinje<List<Opplysninger>>.plus(other: Tidslinje<List<Opplysninger>>) =
        fullJoin(other) { periode, eksisterende, ny ->
            check(eksisterende == null || ny == null) { "Overlappende periode $periode" }
            ny ?: eksisterende
        }
}
