package no.nav.utbetaling.beregningsflyt.fordeler

import java.util.UUID

import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.beregning.fordeler.LinjeGroupKey
import no.nav.utbetaling.beregning.oppdrag.Opplysninger
import no.nav.utbetaling.beregning.tid.DefaultTidslinjesamling
import no.nav.utbetaling.beregning.tid.Tidslinje
import no.nav.utbetaling.beregning.tid.Tidslinjesamling
import no.nav.utbetaling.beregningsflyt.identitet.Beregningsidentitet
import no.nav.utbetaling.beregningsflyt.identitet.Oppdragsnokkel
import no.nav.utbetaling.beregningsflyt.kontekst.avgrensTil

internal data class Tidslinjegruppe(
    val oppdragsnokkel: Oppdragsnokkel,
    val linjeGroupKey: LinjeGroupKey,
    val oppdrag: Tidslinjesamling<UUID, Opplysninger>,
)

internal fun grupperTidslinjer(
    oppdrag: Tidslinjesamling<UUID, Opplysninger>,
    periode: DatoIntervall,
    identitet: Beregningsidentitet,
): List<Tidslinjegruppe> {
    val grupper = linkedMapOf<Pair<Oppdragsnokkel, LinjeGroupKey>, MutableMap<UUID, Tidslinje.Builder<Opplysninger>>>()
    oppdrag.avgrensTil(periode).tidslinjer.forEach { (kjedeId, tidslinje) ->
        tidslinje.forEach { (kildeperiode, verdi) ->
            val nokkel = Oppdragsnokkel(verdi.fagområde, verdi.fagsystemId)
            val gjelder = identitet.gjelderId(requireNotNull(verdi.gjelderId) { "$nokkel $kildeperiode: Missing source actor" }.opplysning)
            val key = LinjeGroupKey(verdi.utbetalesTil?.let { identitet.mottakerId(it.opplysning) } ?: gjelder, verdi.kidnummer?.opplysning?.nummer)
            grupper
                .getOrPut(nokkel to key) { linkedMapOf() }
                .getOrPut(kjedeId) { Tidslinje.Builder() }
                .periode(kildeperiode, verdi)
        }
    }
    return grupper
        .map { (key, kjeder) ->
            Tidslinjegruppe(key.first, key.second, DefaultTidslinjesamling(kjeder.mapValues { it.value.build() }))
        }
}

internal fun Opplysninger.beregningssignatur(identitet: Beregningsidentitet): List<Any?> =
    listOf(
        delytelseId,
        refDelytelseId,
        vedtakId?.opplysning,
        sats.opplysning.verdi,
        sats.opplysning.satsType,
        vedtaksSats?.opplysning?.verdi,
        ytelseKlassekode.opplysning,
        gjelderId?.let { identitet.gjelderId(it.opplysning) },
        utbetalesTil?.let { identitet.mottakerId(it.opplysning) } ?: gjelderId?.let { identitet.gjelderId(it.opplysning) },
        kidnummer?.opplysning,
        status.opplysning,
        avventingsårsak?.opplysning,
    )
