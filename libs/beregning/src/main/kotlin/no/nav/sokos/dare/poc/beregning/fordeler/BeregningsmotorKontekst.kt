package no.nav.sokos.dare.poc.beregning.fordeler

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

import no.nav.sokos.dare.poc.beregning.Beregning
import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.beregning.Delberegning

data class BeregningsmotorKontekst(
    override val periode: DatoIntervall,
    override val segmenter: List<BeregningsSegment>,
    override val skatteKontekst: SkatteKontekst,
    override val satsBoetter: SatsBoetter,
    override val saldoBoetter: SaldoBoetter,
    override var restFrikortBeloep: BigDecimal? = null,
    val overføringsdato: LocalDate?,
    override val beregnet: LocalDateTime,
) : Beregningsgrunnlag {
    fun beregninger(): List<Beregning> =
        segmenter.filter { it.delperioder.isNotEmpty() }.map { segment: BeregningsSegment ->
            require(
                segment.delperioder
                    .map { it.oppdrag.id }
                    .distinct()
                    .size == 1,
            ) { "Vi har ikke implementert støtte for beregning av multiple oppdrag enda" }
            val grunnlag = segment.delperioder.first()
            val segmentPeriode =
                DatoIntervall(
                    fom = segment.delperioder.minOf { it.periode.fom },
                    tom =
                        if (segment.delperioder.any { it.periode.tom == null }) {
                            null
                        } else {
                            segment.delperioder.maxOf { it.periode.tom!! }
                        },
                )
            Beregning(
                periode = segmentPeriode,
                konto = grunnlag.konto,
                overfoeres = if (segment.avventStatus != null) segment.avventOverfoeres else grunnlag.overfoeringsdato,
                fagomraade =
                    segment.delperioder
                        .first()
                        .oppdrag.fagomraade,
                gjelder = grunnlag.gjelder,
                beregnet = grunnlag.beregnet,
                utbetalesTil = grunnlag.utbetalesTil,
                status = segment.avventStatus ?: grunnlag.status,
                statusflagg = segment.beregningsStatusflagg.toList(),
                delberegning =
                    segment.scratchpad.flatMap { (oppdragId, scratchpad) ->
                        scratchpad.beregninger.map { (delperiode, linjer) ->
                            Delberegning(
                                oppdragId = oppdragId,
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
