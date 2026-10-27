package no.nav.sokos.dare.poc.beregning.fordeler

import java.time.LocalDate
import java.time.LocalDateTime

import no.nav.sokos.dare.poc.aktoer.AktoerId
import no.nav.sokos.dare.poc.beregning.Beregning
import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.beregning.EkstraInfo
import no.nav.sokos.dare.poc.beregning.dataklasser.BeregningsStatus
import no.nav.sokos.dare.poc.oppdrag.Oppdrag
import no.nav.sokos.dare.poc.trekk.TrekkvedtakId

/**
 * Holder ett og ett oppdrag, hakket opp i en og en delperiode i forhold til perioden for beregningskonteksten. Ideen er at en OppdragDelperiode kan beregnes alene, uten å forholde seg til noe annet enn scratchpad
 * og bøtter for hele beregningskonteksten.
 * `periode` er delperioden som beregnes her. Hele beregningsperioden hentes fra `BeregningsmotorKontekst.periode`.
 */
data class OppdragDelperiode(
    val oppdrag: Oppdrag,
    val periode: DatoIntervall,
    val konto: String,
    val gjelder: AktoerId,
    val beregnet: LocalDateTime,
    val utbetalesTil: AktoerId?,
    val status: BeregningsStatus,
    val trekk: List<TrekkvedtakForBeregning>,
    val ekstraInfo: MutableList<EkstraInfo>,
    val overfoeringsdato: LocalDate?,
    val tidligereTrukneTrekkvedtakIMaaneden: Set<TrekkvedtakId>,
    val relaterteBeregninger: List<Beregning> = emptyList(),
) {
    internal fun effektivOverfoeringsdato(): LocalDate = overfoeringsdato ?: beregnet.toLocalDate()
}
