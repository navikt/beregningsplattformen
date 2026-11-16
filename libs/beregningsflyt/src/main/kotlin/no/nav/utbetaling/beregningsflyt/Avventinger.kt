package no.nav.utbetaling.beregningsflyt

import no.nav.sokos.dare.poc.beregning.EkstraInfo
import no.nav.sokos.dare.poc.beregning.EkstraInfoDsl.verdi
import no.nav.sokos.dare.poc.beregning.TekstEkstraInfo
import no.nav.sokos.dare.poc.beregning.dataklasser.BeregningsStatus
import no.nav.sokos.dare.poc.beregning.dataklasser.BeregningsStatusflagg
import no.nav.sokos.dare.poc.beregning.fordeler.BeregningsSegment
import no.nav.utbetaling.beregning.oppdrag.Avventingsårsak

internal object Avventinger {
    // Legger til avventing på et BeregningsSegment slik beregningsmotoren forventer det.
    internal fun BeregningsSegment.medAvventing(avventingsårsak: Avventingsårsak): BeregningsSegment {
        val (datoOverføres, kodeÅrsak, beregningsStatusflagg) =
            when (avventingsårsak) {
                is Avventingsårsak.Avregning -> Triple(avventingsårsak.til, "AVAV", BeregningsStatusflagg.MANUELL)
                is Avventingsårsak.Refusjonskrav -> Triple(avventingsårsak.til, "AVRK", BeregningsStatusflagg.DATOSTYRT)
            }

        val medAvventing =
            copy(
                avventStatus = BeregningsStatus.AVVENT,
                avventOverfoeres = datoOverføres,
            )
        medAvventing.beregningsStatusflagg.add(beregningsStatusflagg)
        medAvventing.ekstraInfo.add(
            TekstEkstraInfo(
                grunn = EkstraInfo.Reasons.AVVENT,
                verdier =
                    mapOf(
                        "kodeArsak" verdi kodeÅrsak,
                        "datoOverfores" verdi (datoOverføres?.toString() ?: "ikke satt — krever manuell reaktivering"),
                    ),
            ),
        )

        return medAvventing
    }
}
