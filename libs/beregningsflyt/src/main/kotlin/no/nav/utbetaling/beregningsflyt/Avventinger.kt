package no.nav.utbetaling.beregningsflyt

import no.nav.sokos.dare.poc.beregning.EkstraInfo
import no.nav.sokos.dare.poc.beregning.EkstraInfoDsl.verdi
import no.nav.sokos.dare.poc.beregning.TekstEkstraInfo
import no.nav.sokos.dare.poc.beregning.dataklasser.BeregningsStatus
import no.nav.sokos.dare.poc.beregning.dataklasser.BeregningsStatusflagg
import no.nav.sokos.dare.poc.beregning.fordeler.BeregningsSegment
import no.nav.utbetaling.beregning.oppdrag.Avventingsårsak
import no.nav.utbetaling.beregning.oppdrag.Oppdragstidslinje
import no.nav.utbetaling.beregning.tid.Periode
import no.nav.utbetaling.beregning.tid.Tidslinje

internal object Avventinger {
    // Lager en tidslinje med avventinger fra oppdragstidslinjen
    // Er det fler avventinger på samme periode, så velges den første
    internal fun Oppdragstidslinje.avventingstidslinje(): Tidslinje<Avventingsårsak> {
        val avventinger =
            perioderIterator().associate { (periode, opplysninger) ->
                periode to opplysninger.firstNotNullOfOrNull { it.avventingsårsak }?.opplysning
            }
        return avventinger.entries
            .fold(Tidslinje.Builder<Avventingsårsak>()) { builder, (periode, avventingsårsak) ->
                avventingsårsak?.let { builder.periode(periode, it) } ?: builder
            }.build()
    }

    // Legger til avventing på et BeregningsSegment,
    // hvis det finnes en avventing som overlapper med noen av delperiodene i segmentet.
    internal fun BeregningsSegment.medAvventing(avventingstidslinje: Tidslinje<Avventingsårsak>): BeregningsSegment {
        val periodene = delperioder.map { Periode.LukketPeriode(it.periode.fom, it.periode.tom!!) }

        val avventingsårsak =
            avventingstidslinje
                .firstNotNullOfOrNull { (periode, avventing) ->
                    avventing.takeIf { periodene.any { it.overlapperMed(periode) } }
                }
                ?: return this

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
