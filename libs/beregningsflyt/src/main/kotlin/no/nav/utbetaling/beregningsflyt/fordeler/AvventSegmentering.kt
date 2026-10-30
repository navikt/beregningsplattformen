package no.nav.utbetaling.beregningsflyt.fordeler

import java.util.UUID

import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.utbetaling.beregning.oppdrag.Avventingsårsak
import no.nav.utbetaling.beregning.oppdrag.Opplysninger
import no.nav.utbetaling.beregning.tid.Tidslinjesamling
import no.nav.utbetaling.beregningsflyt.identitet.Oppdragsnokkel
import no.nav.utbetaling.beregningsflyt.kontekst.avgrensTil

internal data class AvventSegmentering(
    val periode: DatoIntervall,
    val arsak: Avventingsårsak?,
)

internal fun avventSegmentering(
    oppdrag: Tidslinjesamling<UUID, Opplysninger>,
    periode: DatoIntervall,
): Map<Oppdragsnokkel, List<AvventSegmentering>> {
    val kandidater =
        oppdrag
            .avgrensTil(periode)
            .tidslinjer.values
            .flatMap { tidslinje ->
                tidslinje.map { (kildeperiode, verdi) ->
                    val nokkel = Oppdragsnokkel(verdi.fagområde, verdi.fagsystemId)
                    nokkel to AvventSegmentering(kildeperiode.somDatoIntervall(), verdi.avventingsårsak?.opplysning)
                }
            }.groupBy({ it.first }, { it.second })
    return kandidater.mapValues { (nokkel, verdier) ->
        val avvent = verdier.filter { it.arsak != null }
        val resultat = mutableListOf<AvventSegmentering>()
        delVedGrenser(periode, avvent.map { it.periode }).forEach { delperiode ->
            val arsaker = avvent.filter { it.periode.contains(delperiode.fom) }.map { it.arsak }.distinct()
            require(arsaker.size <= 1) { "$nokkel $delperiode: Conflicting simultaneous avvent information: $arsaker" }
            val arsak = arsaker.singleOrNull()
            val forrige = resultat.lastOrNull()
            if (forrige != null && forrige.arsak == arsak) {
                resultat[resultat.lastIndex] = forrige.copy(periode = DatoIntervall(forrige.periode.fom, delperiode.tom))
            } else {
                resultat.add(AvventSegmentering(delperiode, arsak))
            }
        }
        resultat
    }
}
