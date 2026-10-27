package no.nav.sokos.dare.poc.beregning.fordeler

import java.math.BigDecimal

import no.nav.sokos.dare.poc.beregning.Beregning
import no.nav.sokos.dare.poc.trekk.Trekkberegningstype
import no.nav.sokos.dare.poc.trekk.Trekkvedtak
import no.nav.sokos.dare.poc.trekk.TrekkvedtakId

internal fun beregnInitielleSaldoer(
    trekk: List<Trekkvedtak>,
    relaterteBeregninger: List<Beregning>,
): Map<TrekkvedtakId, BigDecimal> =
    trekk
        .filter { it.id != null && it.erSaldoTrekk() }
        .associate { vedtak ->
            val alreadyDeducted = beregnTrukketSaldoFraRelaterteBeregninger(vedtak.id!!, relaterteBeregninger)
            vedtak.id to (vedtak.belopSaldo!! - alreadyDeducted)
        }

internal fun beregnInitielleSatser(
    segmenter: List<BeregningsSegment>,
    relaterteBeregninger: List<Beregning>,
): Map<SatsBoetteId, BigDecimal> =
    segmenter
        .flatMap { it.delperioder }
        .flatMap { it.trekk }
        .flatMap { trekkvedtak ->
            trekkvedtak.trekkperiode.satsBoetteIds().map { id ->
                val alreadyDeducted =
                    when (trekkvedtak.trekktype.trekkberegningstype) {
                        Trekkberegningstype.YTELPER -> {
                            val boettePeriode = boetteTilPeriode(id.boette, trekkvedtak.trekkperiode.periode)
                            beregnTrukketSatsFraRelaterteBeregningerYtelper(
                                trekkvedtakId = TrekkvedtakId(trekkvedtak.trekkperiode.trekkvedtakId),
                                boettePeriode = boettePeriode,
                                relaterteBeregninger = relaterteBeregninger,
                            )
                        }
                        Trekkberegningstype.UTBDATO -> {
                            beregnTrukketSatsFraRelaterteBeregningerUtbdato(
                                trekkvedtakId = TrekkvedtakId(trekkvedtak.trekkperiode.trekkvedtakId),
                                overfoeringsdato = id.boette,
                                relaterteBeregninger = relaterteBeregninger,
                            )
                        }
                    }
                id to (trekkvedtak.trekkperiode.sats - alreadyDeducted)
            }
        }.toMap()
