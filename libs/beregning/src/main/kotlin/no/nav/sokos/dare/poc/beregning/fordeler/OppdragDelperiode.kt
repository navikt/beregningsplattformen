package no.nav.sokos.dare.poc.beregning.fordeler

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

import no.nav.sokos.dare.poc.aktoer.AktoerId
import no.nav.sokos.dare.poc.beregning.Beregning
import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.beregning.EkstraInfo
import no.nav.sokos.dare.poc.beregning.dataklasser.BeregningsStatus
import no.nav.sokos.dare.poc.oppdrag.Oppdrag
import no.nav.sokos.dare.poc.oppdrag.OppdragId
import no.nav.sokos.dare.poc.trekk.FagomraadeKode
import no.nav.sokos.dare.poc.trekk.TrekkvedtakId

/**
 * Holder ett og ett oppdrag, hakket opp i en og en delperiode i forhold til perioden for beregningskonteksten. Ideen er at en OppdragDelperiode kan beregnes alene, uten å forholde seg til noe annet enn scratchpad
 * og bøtter for hele beregningskonteksten.
 * `periode` er delperioden som beregnes her. Hele beregningsperioden hentes fra `BeregningsmotorKontekst.periode`.
 */
data class OppdragDelperiode(
    val oppdrag: Oppdrag,
    override val periode: DatoIntervall,
    override val konto: String,
    override val gjelder: AktoerId,
    override val beregnet: LocalDateTime,
    override val utbetalesTil: AktoerId?,
    override val status: BeregningsStatus,
    override val trekk: List<TrekkvedtakForBeregning>,
    override val ekstraInfo: MutableList<EkstraInfo>,
    override val overfoeringsdato: LocalDate?,
    override val tidligereTrukneTrekkvedtakIMaaneden: Set<TrekkvedtakId>,
    override val relaterteBeregninger: List<Beregning> = emptyList(),
) : Delperiodegrunnlag {
    override val oppdragId: OppdragId
        get() = requireNotNull(oppdrag.id)

    override val fagomraade: FagomraadeKode
        get() = oppdrag.fagomraade

    override fun forDag(dato: LocalDate): BigDecimal = oppdrag.forDag(dato)

    override fun vedtakSatsForDag(dato: LocalDate): BigDecimal = oppdrag.vedtakSatsForDag(dato)

    override fun trekkgrunnlag(periode: DatoIntervall): BigDecimal = oppdrag.trekkgrunnlag(periode)

    internal fun effektivOverfoeringsdato(): LocalDate = overfoeringsdato ?: beregnet.toLocalDate()
}
