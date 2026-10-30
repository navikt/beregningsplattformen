package no.nav.utbetaling.beregningsflyt.kontekst

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

import no.nav.sokos.dare.poc.beregning.Beregning
import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.beregning.Delberegning
import no.nav.sokos.dare.poc.beregning.fordeler.Beregningsgrunnlag
import no.nav.sokos.dare.poc.beregning.fordeler.SaldoBoetter
import no.nav.sokos.dare.poc.beregning.fordeler.SatsBoetter
import no.nav.sokos.dare.poc.beregning.fordeler.SkatteKontekst

class BeregningsmotorKontekst(
    override val periode: DatoIntervall,
    override val segmenter: List<BeregningsSegment>,
    override val skatteKontekst: SkatteKontekst,
    satsBoetter: SatsBoetter,
    saldoBoetter: SaldoBoetter,
    override var restFrikortBeloep: BigDecimal? = null,
    val overføringsdato: LocalDate?,
    override val beregnet: LocalDateTime,
) : Beregningsgrunnlag {
    override val satsBoetter: SatsBoetter = SatsBoetter(satsBoetter.satser.toMap())
    override val saldoBoetter: SaldoBoetter = SaldoBoetter(saldoBoetter.saldoer.toMap())

    init {
        require(periode.tom?.let { !it.isBefore(periode.fom) } == true) { "Expected a finite calculation interval: $periode" }
        segmenter.flatMap { it.delperioder }.forEach {
            require(periode.contains(it.periode)) { "${it.oppdragsnokkel} ${it.periode}: Outside calculation interval $periode" }
        }
    }

    override fun beregningsrekkefolge(): List<Pair<BeregningsSegment, OppdragDelperiode>> =
        segmenter
            .flatMap { segment -> segment.delperioder.map { segment to it } }
            .groupBy { (_, delperiode) -> delperiode.oppdragsnokkel }
            .values
            .flatMap { perOppdrag -> perOppdrag.sortedBy { (_, delperiode) -> delperiode.periode.fom } }

    fun beregninger(): List<Beregning> =
        segmenter.filter { it.delperioder.isNotEmpty() }.map { segment ->
            val grunnlag = segment.delperioder.first()
            require(segment.scratchpad.keys.all { it == grunnlag.oppdragId }) {
                "${grunnlag.oppdragsnokkel} ${grunnlag.periode}: Conflicting scratchpad assignment identity"
            }
            val segmentPeriode =
                DatoIntervall(
                    fom = segment.delperioder.minOf { it.periode.fom },
                    tom = segment.delperioder.maxOf { requireNotNull(it.periode.tom) },
                )
            Beregning(
                periode = segmentPeriode,
                konto = grunnlag.konto,
                overfoeres = if (segment.avventStatus != null) segment.avventOverfoeres else grunnlag.overfoeringsdato,
                fagomraade = grunnlag.fagomraade,
                gjelder = grunnlag.gjelder,
                beregnet = grunnlag.beregnet,
                utbetalesTil = grunnlag.utbetalesTil,
                status = segment.avventStatus ?: grunnlag.status,
                statusflagg = segment.beregningsStatusflagg.toList(),
                delberegning =
                    segment.scratchpad.values.flatMap { scratchpad ->
                        scratchpad.beregninger.map { (delperiode, linjer) ->
                            Delberegning(
                                oppdragId = grunnlag.oppdragId,
                                linjer = linjer,
                                datoIntervall = delperiode,
                                ekstraInfo = scratchpad.ekstraInfo,
                            )
                        }
                    },
                ekstraInfo = segment.ekstraInfo,
            )
        }

    override fun aktivDato(): LocalDate = overføringsdato ?: beregnet.toLocalDate()
}
