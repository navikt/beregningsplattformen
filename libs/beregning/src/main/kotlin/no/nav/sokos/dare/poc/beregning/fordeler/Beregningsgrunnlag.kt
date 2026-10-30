package no.nav.sokos.dare.poc.beregning.fordeler

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

import no.nav.sokos.dare.poc.aktoer.AktoerId
import no.nav.sokos.dare.poc.beregning.Beregning
import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.beregning.EkstraInfo
import no.nav.sokos.dare.poc.beregning.dataklasser.BeregningsStatus
import no.nav.sokos.dare.poc.beregning.dataklasser.BeregningsStatusflagg
import no.nav.sokos.dare.poc.oppdrag.OppdragId
import no.nav.sokos.dare.poc.trekk.FagomraadeKode
import no.nav.sokos.dare.poc.trekk.TrekkvedtakId

interface Delperiodegrunnlag {
    val oppdragId: OppdragId
    val fagomraade: FagomraadeKode
    val periode: DatoIntervall
    val konto: String
    val gjelder: AktoerId
    val beregnet: LocalDateTime
    val utbetalesTil: AktoerId?
    val status: BeregningsStatus
    val trekk: List<TrekkvedtakForBeregning>
    val ekstraInfo: MutableList<EkstraInfo>
    val overfoeringsdato: LocalDate?
    val tidligereTrukneTrekkvedtakIMaaneden: Set<TrekkvedtakId>
    val relaterteBeregninger: List<Beregning>

    fun forDag(dato: LocalDate): BigDecimal

    fun vedtakSatsForDag(dato: LocalDate): BigDecimal

    fun trekkgrunnlag(periode: DatoIntervall): BigDecimal
}

interface Segmentgrunnlag {
    val linjeGroupKey: LinjeGroupKey?
    val delperioder: List<Delperiodegrunnlag>
    val ekstraInfo: MutableSet<EkstraInfo>
    val beregningsStatusflagg: MutableSet<BeregningsStatusflagg>
    val avventStatus: BeregningsStatus?
    val avventOverfoeres: LocalDate?
    val scratchpad: MutableMap<OppdragId, BeregningsScratchpad>
}

interface Beregningsgrunnlag {
    val periode: DatoIntervall
    val segmenter: List<Segmentgrunnlag>
    val skatteKontekst: SkatteKontekst
    val satsBoetter: SatsBoetter
    val saldoBoetter: SaldoBoetter
    var restFrikortBeloep: BigDecimal?
    val beregnet: LocalDateTime

    /** Calculation order is independent of presentation grouping; legacy keeps segment-first traversal. */
    fun beregningsrekkefolge(): List<Pair<Segmentgrunnlag, Delperiodegrunnlag>> = segmenter.flatMap { segment -> segment.delperioder.map { segment to it } }

    fun aktivDato(): LocalDate
}
