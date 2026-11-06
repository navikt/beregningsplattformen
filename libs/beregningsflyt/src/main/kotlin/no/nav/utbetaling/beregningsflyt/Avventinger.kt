package no.nav.utbetaling.beregningsflyt

import no.nav.sokos.dare.poc.aktoer.AktoerId
import no.nav.sokos.dare.poc.beregning.EkstraInfo
import no.nav.sokos.dare.poc.beregning.EkstraInfoDsl.verdi
import no.nav.sokos.dare.poc.beregning.TekstEkstraInfo
import no.nav.sokos.dare.poc.beregning.dataklasser.BeregningsStatus
import no.nav.sokos.dare.poc.beregning.dataklasser.BeregningsStatusflagg
import no.nav.sokos.dare.poc.beregning.fordeler.BeregningsSegment
import no.nav.sokos.dare.poc.beregning.fordeler.LinjeGroupKey
import no.nav.sokos.dare.poc.beregning.fordeler.OppdragDelperiode
import no.nav.sokos.dare.poc.trekk.FagomraadeKode
import no.nav.utbetaling.beregning.oppdrag.Avventingsårsak
import no.nav.utbetaling.beregning.oppdrag.Fagområde
import no.nav.utbetaling.beregning.oppdrag.Kidnummer
import no.nav.utbetaling.beregning.oppdrag.Oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.UtbetalesTil
import no.nav.utbetaling.beregning.tid.Periode
import no.nav.utbetaling.beregning.tid.Tidslinje
import no.nav.utbetaling.beregningsflyt.Beregningsflyt.*

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

    internal fun lagBeregningsSegment(
        delperiode: OppdragDelperiode,
        avventingstidslinje: Tidslinje<Avventingsårsak>
    ): Pair<Segmentgruppering, BeregningsSegment> {
        val kid = delperiode.oppdrag.oppdragslinjekjeder
            .single()
            .linjer
            .single()
            .kid
        val beregningsSegment = BeregningsSegment(
            linjeGroupKey = LinjeGroupKey(utbetalesTilId = delperiode.utbetalesTil!!, kid = kid),
            delperioder = listOf(delperiode),
        ).medAvventing(avventingstidslinje)

        val segmentGruppering = Segmentgruppering(
            fagområde = delperiode.oppdrag.fagomraade,
            fagsystemId = delperiode.oppdrag.fagsystemId,
            utbetalesTil = delperiode.utbetalesTil!!,
            kid = kid,
            status = beregningsSegment.avventStatus ?: BeregningsStatus.BEREGNET
        )

        return segmentGruppering to beregningsSegment
    }

    // Legger til avventing på et BeregningsSegment,
    // hvis det finnes en avventing som overlapper med noen av delperiodene i segmentet.
    private fun BeregningsSegment.medAvventing(avventingstidslinje: Tidslinje<Avventingsårsak>): BeregningsSegment {
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
