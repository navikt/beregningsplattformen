package no.nav.sokos.dare.poc.beregning.beregningsmotor

import java.math.BigDecimal
import java.math.BigDecimal.ZERO

import no.nav.sokos.dare.poc.beregning.EkstraInfo.Reasons.FEIL_I_BEREGNING
import no.nav.sokos.dare.poc.beregning.TekstEkstraInfo
import no.nav.sokos.dare.poc.beregning.fordeler.BeregningsScratchpad
import no.nav.sokos.dare.poc.beregning.fordeler.BeregningsmotorKontekst
import no.nav.sokos.dare.poc.beregning.fordeler.OppdragDelperiode
import no.nav.sokos.dare.poc.beregning.fordeler.TrekkvedtakForBeregning
import no.nav.sokos.dare.poc.trekk.SatsType
import no.nav.sokos.dare.poc.trekk.Trekkberegningstype

class Trekkmotor(
    private val trekkBeloepCalculator: TrekkBeloepCalculator = TrekkBeloepCalculator(),
    private val satsBoetteLimiter: SatsBoetteLimiter = SatsBoetteLimiter(),
    private val delberegningslinjeProducer: TrekkDelberegningslinjeProducer = TrekkDelberegningslinjeProducer(),
) {
    fun calculateSkattereduserendeTrekk(calculatedYtelser: BeregningsmotorKontekst): BeregningsmotorKontekst = calculate(calculatedYtelser, TrekkKategori.SKATTEREDUSERENDE)

    fun calculateOevrigeTrekk(calculatedYtelserWithSkattetrekk: BeregningsmotorKontekst): BeregningsmotorKontekst = calculate(calculatedYtelserWithSkattetrekk, TrekkKategori.OEVRIGE)

    private fun calculate(
        kontekst: BeregningsmotorKontekst,
        kategori: TrekkKategori,
    ): BeregningsmotorKontekst {
        kontekst.segmenter.forEach { segment ->
            segment.delperioder.forEach { delperiode ->
                val trekk = delperiode.trekk.filtrerPåKategori(kategori).sortedWith(trekkvedtakSortering)
                if (trekk.isEmpty()) return@forEach

                val oppdragId = requireNotNull(delperiode.oppdrag.id) { "Oppdrag mangler id" }
                val scratchpad = segment.scratchpad.getOrPut(oppdragId) { BeregningsScratchpad() }
                val trekkgrunnlag = calculateTrekkgrunnlag(delperiode)

                processDelperiodeTrekk(
                    trekk = trekk,
                    delperiode = delperiode,
                    trekkgrunnlag = trekkgrunnlag,
                    scratchpad = scratchpad,
                    kontekst = kontekst,
                )
            }
        }
        return kontekst
    }

    private fun processDelperiodeTrekk(
        trekk: List<TrekkvedtakForBeregning>,
        delperiode: OppdragDelperiode,
        trekkgrunnlag: BigDecimal,
        scratchpad: BeregningsScratchpad,
        kontekst: BeregningsmotorKontekst,
    ) {
        trekk.forEach { trekkvedtak ->
            val halverMaanedssats = skalHalvereMaanedssats(trekkvedtak, delperiode)
            val trekkBeloep =
                trekkBeloepCalculator.calculate(
                    trekkvedtak = trekkvedtak,
                    periode = delperiode.periode,
                    trekkGrunnlag = trekkgrunnlag,
                    saldoBoetter = kontekst.saldoBoetter,
                    halverMaanedssats = halverMaanedssats,
                )
            val kapasitetsjustertTrekk =
                scratchpad.kapasitetStyring.adjustTrekk(
                    periode = delperiode.periode,
                    trekk = trekkBeloep,
                )
            val negativeSatsBoetter = negativeSatsboetterFinnes(trekkvedtak, kontekst)

            val satsBegrensetTrekk =
                satsBoetteLimiter.limit(
                    justertTrekk = kapasitetsjustertTrekk,
                    trekkvedtak = trekkvedtak,
                    satsBoetter = kontekst.satsBoetter,
                    delperiode = delperiode,
                )
            val kapasitetSnapshot = scratchpad.kapasitetStyring.snapshot(delperiode.periode)
            val trekkDelberegningslinje =
                delberegningslinjeProducer.make(
                    trekkvedtak = trekkvedtak,
                    delperiode = delperiode,
                    trekkgrunnlag = trekkgrunnlag,
                    satsBegrensetTrekk = satsBegrensetTrekk,
                    kapasitet = kapasitetSnapshot,
                    halverMaanedssats = halverMaanedssats,
                    ekstraInfo =
                        if (negativeSatsBoetter) {
                            listOf(
                                TekstEkstraInfo(
                                    grunn = FEIL_I_BEREGNING,
                                ),
                            )
                        } else {
                            emptyList()
                        },
                )

            scratchpad.beregninger
                .getOrPut(delperiode.periode) { mutableListOf() }
                .add(-trekkDelberegningslinje)
            kontekst.saldoBoetter.brukSaldo(trekkvedtak.id, trekkDelberegningslinje.sum)
            satsBoetteLimiter.consume(trekkDelberegningslinje, trekkvedtak, kontekst.satsBoetter)
            scratchpad.kapasitetStyring.consumeTrekk(delperiode.periode, trekkDelberegningslinje)
        }
    }

    private fun calculateTrekkgrunnlag(delperiode: OppdragDelperiode): BigDecimal = delperiode.oppdrag.trekkgrunnlag(delperiode.periode)

    private fun negativeSatsboetterFinnes(
        trekk: TrekkvedtakForBeregning,
        kontekst: BeregningsmotorKontekst,
    ): Boolean =
        trekk.trekkperiode.satsBoetteIds().any {
            (kontekst.satsBoetter.restSats(it) ?: ZERO) < ZERO
        }

    private val trekkvedtakSortering =
        compareBy<TrekkvedtakForBeregning>(
            { it.trekktype.prioritet },
            { it.datoPriFom },
            { it.trekkperiode.periode.fom },
        )

    companion object {
        fun skalHalvereMaanedssats(
            trekkvedtak: TrekkvedtakForBeregning,
            delperiode: OppdragDelperiode,
        ): Boolean =
            trekkvedtak.trekkperiode.satstype == SatsType.M &&
                trekkvedtak.trekktype.trekkberegningstype == Trekkberegningstype.UTBDATO &&
                delperiode.oppdrag.fagomraade.faggruppe.halverTrekkForFørsteUtbetalingIMåned &&
                trekkvedtak.id !in delperiode.tidligereTrukneTrekkvedtakIMaaneden
    }
}

fun List<TrekkvedtakForBeregning>.filtrerPåKategori(kategori: TrekkKategori) =
    this.filter {
        when (kategori) {
            TrekkKategori.SKATTEREDUSERENDE -> it.trekktype.reduserSkattegr
            TrekkKategori.OEVRIGE -> !it.trekktype.reduserSkattegr
        }
    }

enum class TrekkKategori {
    SKATTEREDUSERENDE,
    OEVRIGE,
}
