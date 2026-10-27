package no.nav.sokos.dare.poc.beregning.fordeler

import java.time.LocalDate
import java.time.LocalDateTime

import no.nav.sokos.dare.poc.aktoer.AktoerId
import no.nav.sokos.dare.poc.beregning.EkstraInfo
import no.nav.sokos.dare.poc.beregning.dataklasser.BeregningsStatus

/**
 * Samler kontekst-parametere som er felles for hele fordelingen og ikke endrer seg mellom segmenter.
 */
data class FordelingInput(
    val konto: String,
    val gjelder: AktoerId,
    val beregnet: LocalDateTime,
    val utbetalesTil: AktoerId?,
    val status: BeregningsStatus,
    val ekstraInfo: MutableList<EkstraInfo>,
    val overfoeringsdato: LocalDate?,
) {
    internal fun effektivOverfoeringsdato(): LocalDate = overfoeringsdato ?: beregnet.toLocalDate()
}
