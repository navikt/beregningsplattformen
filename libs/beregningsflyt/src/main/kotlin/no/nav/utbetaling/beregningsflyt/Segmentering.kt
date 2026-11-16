package no.nav.utbetaling.beregningsflyt

import no.nav.utbetaling.beregning.oppdrag.Avventingsårsak
import no.nav.utbetaling.beregning.oppdrag.Fagområde
import no.nav.utbetaling.beregning.oppdrag.FagsystemId
import no.nav.utbetaling.beregning.oppdrag.Kidnummer
import no.nav.utbetaling.beregning.oppdrag.Opplysninger
import no.nav.utbetaling.beregning.oppdrag.UtbetalesTil
import no.nav.utbetaling.beregning.tid.Tidslinje
import no.nav.utbetaling.beregning.tid.Tidslinjesamling

internal data class Segmentgrunnlag(
    val utbetaltTil: UtbetalesTil?,
    val kidnummer: Kidnummer?,
    val avventingsårsak: Avventingsårsak?,
    val delperiodegrunnlag: Tidslinje<Map<Oppdragsidentifikator, List<Opplysninger>>>,
)

internal data class Oppdragsidentifikator(
    val fagområde: Fagområde,
    val fagsystemId: FagsystemId,
)

/**
 * Preprosesserer oppdragstidslinjen til et format som gjør det enklere å
 * opprette rett segment med rett delperioder slik beregningsmotoren forventer.
 *
 * Et Segmentgrunnlag -> Et BeregningsSegment
 * Segmentgrunnlagets delperiodegrunnlag er grunnlaget for delperiodene i et BeregningsSegment.
 *    Hver periode kan ha en eller fler delperioder, avehengig av antall unike fagsystemId + fagområde-kombinasjoner.
 *    Hver enkelt unike kominasjon kan igjen ha en liste med opplysninger som gjenspeiler antall ytelseslinjer i den delperioden.
 *    Det må også hensyntas oppsplitting på andre nivåer, som skatt, trekk, år etc.
 *    før delperiodegrunnlag brukes til å opprette delperioder med rett perioder.
 *
 */
internal object Segmentering {
    fun grunnlag(oppdragstidslinje: Tidslinjesamling<*, Opplysninger>): List<Segmentgrunnlag> {
        val ferdigeSegmentgrunnlag = mutableListOf<Segmentgrunnlag>()
        val aktiveSegmentgrunnlag = mutableMapOf<Segmentgruppering, Tidslinje.Builder<Map<Oppdragsidentifikator, List<Opplysninger>>>>()

        // Én iterasjon per knekkpunkt på tvers av alle kjeder
        oppdragstidslinje.perioderIterator().forEach { (periode, opplysningerIPerioden) ->
            val perSegmentgruppering = opplysningerIPerioden.groupBy { it.segmentgruppering() }

            // En aktiv gruppering lukkes når den mangler i perioden og samme utbetaltTil og kidnummer har fått en annen avventingsårsak
            aktiveSegmentgrunnlag.keys
                .filter { aktiv -> aktiv !in perSegmentgruppering && perSegmentgruppering.keys.any { it.medAnnenAvventingsårsakEnn(aktiv) } }
                .forEach { ferdigeSegmentgrunnlag += it.segmentgrunnlag(aktiveSegmentgrunnlag.remove(it)!!) }

            // Grupperinger uten linjer i perioden forblir aktive. De andre utvides, eller starter et nytt segment.
            perSegmentgruppering.forEach { (segmentgruppering, opplysninger) ->
                aktiveSegmentgrunnlag
                    .getOrPut(segmentgruppering) { Tidslinje.Builder() }
                    .periode(periode, opplysninger.groupBy { it.oppdragsidentifikator() })
            }
        }

        return ferdigeSegmentgrunnlag + aktiveSegmentgrunnlag.map { (gruppering, builder) -> gruppering.segmentgrunnlag(builder) }
    }

    private data class Segmentgruppering(
        val utbetaltTil: UtbetalesTil?,
        val kidnummer: Kidnummer?,
        val avventingsårsak: Avventingsårsak?,
    ) {
        fun medAnnenAvventingsårsakEnn(other: Segmentgruppering) = utbetaltTil == other.utbetaltTil && kidnummer == other.kidnummer && avventingsårsak != other.avventingsårsak
    }

    private fun Opplysninger.segmentgruppering() =
        Segmentgruppering(
            utbetaltTil = utbetalesTil?.opplysning,
            kidnummer = kidnummer?.opplysning,
            avventingsårsak = avventingsårsak?.opplysning,
        )

    private fun Segmentgruppering.segmentgrunnlag(builder: Tidslinje.Builder<Map<Oppdragsidentifikator, List<Opplysninger>>>) =
        Segmentgrunnlag(
            utbetaltTil = utbetaltTil,
            kidnummer = kidnummer,
            avventingsårsak = avventingsårsak,
            delperiodegrunnlag = builder.build(),
        )

    private fun Opplysninger.oppdragsidentifikator() =
        Oppdragsidentifikator(
            fagområde = fagområde,
            fagsystemId = fagsystemId,
        )
}
