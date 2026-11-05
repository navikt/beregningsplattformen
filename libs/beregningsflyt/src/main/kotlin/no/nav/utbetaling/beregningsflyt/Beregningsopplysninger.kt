package no.nav.utbetaling.beregningsflyt

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month.DECEMBER
import java.time.Month.JANUARY

import no.nav.utbetaling.beregning.oppdrag.Opplysninger
import no.nav.utbetaling.beregning.tid.Periode
import no.nav.utbetaling.beregning.tid.Tidslinje

data class Beregningsopplysninger(
    val oppdragopplysninger: Opplysninger,
    val tøyseteller: Int,
)

fun beregningstidslinje(
    beregningsperiode: Periode.LukketPeriode,
    tidslinje: Tidslinje<Opplysninger>,
    beregningsDato: LocalDateTime,
): Tidslinje<Beregningsopplysninger> {
    val perioder = skattekortBoundaries(beregningsperiode, beregningsDato)

    val skattetidslinje =
        perioder
            .foldIndexed(Tidslinje.Builder<Int>()) { index, builder, periode ->
                builder.periode(periode, index)
            }.build()

    val begrensettidslinje = tidslinje.begrensetTil(beregningsperiode.tom) // todo lag en begrensTilPeriode i tidslinje som tar en periode og begrenser tidslinjen til den perioden.

    return begrensettidslinje.kombinerMed(skattetidslinje) { periode, oppdragsopplysninger, skatteOpplysninger ->
        if (oppdragsopplysninger == null || skatteOpplysninger == null) {
            null
        } else {
            Beregningsopplysninger(oppdragsopplysninger, skatteOpplysninger)
        }
    }
}

private fun skattekortBoundaries(
    periode: Periode.LukketPeriode,
    beregningsDato: LocalDateTime,
): List<Periode.LukketPeriode> {
    val desemberIFjor = LocalDate.of(beregningsDato.year - 1, DECEMBER, 1)
    val needsDecemberSplit = periode.contains(desemberIFjor)
    val janFeb = Periode.LukketPeriode(LocalDate.of(beregningsDato.year, JANUARY, 1), LocalDate.of(beregningsDato.year, JANUARY, 31))
    val needsJanFebSplit = periode.overlapperMed(janFeb)

    return if (needsDecemberSplit || needsJanFebSplit) {
        buildList {
            if (needsDecemberSplit) add(Periode.LukketPeriode(periode.fom, desemberIFjor.minusDays(1))) // skal lage splitt siste dag i november
            if (needsJanFebSplit) add(janFeb)
        }.toList()
    } else {
        listOf(periode)
    }
}
