package no.nav.utbetaling.beregningsflyt

import no.nav.sokos.dare.poc.beregning.EkstraInfo
import no.nav.sokos.dare.poc.beregning.EkstraInfoDsl.verdi
import no.nav.sokos.dare.poc.beregning.TekstEkstraInfo
import no.nav.sokos.dare.poc.beregning.dataklasser.BeregningsStatus
import no.nav.sokos.dare.poc.beregning.dataklasser.BeregningsStatusflagg
import no.nav.sokos.dare.poc.beregning.fordeler.BeregningsSegment
import no.nav.sokos.dare.poc.beregning.fordeler.LinjeGroupKey
import no.nav.sokos.dare.poc.beregning.fordeler.OppdragDelperiode
import no.nav.utbetaling.beregning.oppdrag.Avventingsårsak
import no.nav.utbetaling.beregning.oppdrag.Opplysninger
import no.nav.utbetaling.beregningsflyt.Beregningsflyt.Segmentgruppering

internal object Avventinger {
    internal fun lagBeregningsSegment(
        delperiode: OppdragDelperiode,
        oppdragsopplysninger: Opplysninger,
    ): Pair<Segmentgruppering, BeregningsSegment> {
        val kid = oppdragsopplysninger.kidnummer?.opplysning?.nummer

        val status =
            when (oppdragsopplysninger.avventingsårsak) {
                null -> BeregningsStatus.BEREGNET
                else -> BeregningsStatus.AVVENT
            }

        val segmentGruppering =
            Segmentgruppering(
                fagområde = delperiode.oppdrag.fagomraade,
                fagsystemId = delperiode.oppdrag.fagsystemId,
                utbetalesTil = delperiode.utbetalesTil!!,
                kid = kid,
                status = status,
            )

        val beregningsSegment =
            BeregningsSegment(
                linjeGroupKey = LinjeGroupKey(utbetalesTilId = delperiode.utbetalesTil!!, kid = kid),
                delperioder = listOf(delperiode),
            )

        return segmentGruppering to
            when (val avventingårsak = oppdragsopplysninger.avventingsårsak?.opplysning) {
                null -> beregningsSegment
                else -> beregningsSegment.medAvventing(avventingårsak)
            }
    }

    // Legger til avventing på et BeregningsSegment slik beregningsmotoren forventer det.
    private fun BeregningsSegment.medAvventing(avventingsårsak: Avventingsårsak): BeregningsSegment {
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
