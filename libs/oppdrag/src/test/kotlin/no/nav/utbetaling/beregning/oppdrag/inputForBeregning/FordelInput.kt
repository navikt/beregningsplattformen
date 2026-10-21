package no.nav.utbetaling.beregning.oppdrag.inputForBeregning

import java.time.LocalDate
import java.time.LocalDateTime

enum class BeregningsStatus {
    IRRELEVANT,
}

enum class EkstraInfo {
    IRRELEVANT,
}

data class FordelingInput(
    val konto: String,
    val gjelder: AktoerId,
    val beregnet: LocalDateTime,
    val utbetalesTil: AktoerId?,
    val status: BeregningsStatus,
    val ekstraInfo: MutableList<EkstraInfo>,
    val overfoeringsdato: LocalDate?,
)
