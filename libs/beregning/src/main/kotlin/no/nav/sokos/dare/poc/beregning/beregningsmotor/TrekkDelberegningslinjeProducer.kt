package no.nav.sokos.dare.poc.beregning.beregningsmotor

import java.math.BigDecimal
import java.math.RoundingMode

import no.nav.sokos.dare.poc.beregning.BeregningsBeloep
import no.nav.sokos.dare.poc.beregning.DelberegningType
import no.nav.sokos.dare.poc.beregning.Delberegningslinje
import no.nav.sokos.dare.poc.beregning.EkstraInfo
import no.nav.sokos.dare.poc.beregning.EkstraInfo.Reasons.TREKK
import no.nav.sokos.dare.poc.beregning.EkstraInfo.Reasons.TREKK_MAANED_HALVERING
import no.nav.sokos.dare.poc.beregning.EkstraInfoDsl.verdi
import no.nav.sokos.dare.poc.beregning.Kapasitet
import no.nav.sokos.dare.poc.beregning.TekstEkstraInfo
import no.nav.sokos.dare.poc.beregning.dataklasser.Satstype
import no.nav.sokos.dare.poc.beregning.fordeler.OppdragDelperiode
import no.nav.sokos.dare.poc.beregning.fordeler.TrekkvedtakForBeregning
import no.nav.sokos.dare.poc.trekk.TrekkAlt

class TrekkDelberegningslinjeProducer {
    fun make(
        trekkvedtak: TrekkvedtakForBeregning,
        delperiode: OppdragDelperiode,
        trekkgrunnlag: BigDecimal,
        satsBegrensetTrekk: BeregningsBeloep,
        kapasitet: Kapasitet,
        ekstraInfo: List<EkstraInfo> = emptyList(),
        halverMaanedssats: Boolean,
    ): Delberegningslinje {
        val trekkalternativ = if (trekkvedtak.erSaldoTrekk()) TrekkAlt.SAL else TrekkAlt.LOP
        val nyEkstraInfo =
            buildSet<EkstraInfo> {
                add(
                    TekstEkstraInfo(
                        TREKK,
                        mapOf(
                            "kode" verdi trekkvedtak.trekktypeKode,
                            "sats" verdi trekkvedtak.trekkperiode.sats,
                            "saldo" verdi trekkvedtak.belopSaldo,
                            "redusererSkattegrunnlag" verdi trekkvedtak.trekktype.reduserSkattegr,
                            "alternativ" verdi trekkalternativ,
                            "datoFom" verdi trekkvedtak.trekkperiode.periode.fom,
                            "datoTom" verdi trekkvedtak.trekkperiode.periode.tom,
                            "trekkgrunnlag" verdi trekkgrunnlag,
                            "kapasitet" verdi kapasitet.sum,
                        ),
                    ),
                )
                if (halverMaanedssats) {
                    add(
                        TekstEkstraInfo(
                            grunn = TREKK_MAANED_HALVERING,
                            verdier =
                                mapOf(
                                    "opprinneligSats" verdi trekkvedtak.trekkperiode.sats,
                                    "halvertSats" verdi trekkvedtak.trekkperiode.sats.divide(BigDecimal(2), 0, RoundingMode.HALF_UP),
                                ),
                        ),
                    )
                }
                addAll(ekstraInfo)
            }

        return Delberegningslinje.create(
            periode = delperiode.periode,
            type = DelberegningType.TREKK,
            kilde = satsBegrensetTrekk,
            ekstraInfo = nyEkstraInfo,
            kapasitet = kapasitet,
            satstype = if (trekkvedtak.erSaldoTrekk()) Satstype.SAL else Satstype.LOP,
            sats = trekkvedtak.trekkperiode.sats,
            trekkvedtakId = trekkvedtak.id,
        )
    }
}
