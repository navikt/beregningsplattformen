package no.nav.sokos.dare.poc.beregning.dto

import java.math.BigDecimal

import kotlinx.serialization.Serializable

import no.nav.sokos.dare.poc.BigDecimalJson
import no.nav.sokos.dare.poc.LocalDateJson
import no.nav.sokos.dare.poc.LocalDateTimeJson
import no.nav.sokos.dare.poc.aktoer.AktoerId
import no.nav.sokos.dare.poc.beregning.Beregning
import no.nav.sokos.dare.poc.beregning.BeregningId
import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.beregning.EkstraInfo
import no.nav.sokos.dare.poc.beregning.dataklasser.BeregningsStatus
import no.nav.sokos.dare.poc.beregning.dataklasser.BeregningsStatusflagg
import no.nav.sokos.dare.poc.trekk.FagomraadeKode

@Serializable
data class BeregningsPresentasjonV2(
    val id: BeregningId? = null,
    val periode: DatoIntervall,
    val status: BeregningsStatus = BeregningsStatus.BEREGNET,
    val statusflagg: List<BeregningsStatusflagg> = emptyList(),
    val overfoeres: LocalDateJson? = null,
    val fagomraade: FagomraadeKode,
    val gjelder: AktoerId,
    val beregnet: LocalDateTimeJson,
    val utbetalesTil: AktoerId? = null,
    val delberegninger: List<DelBeregningsPresentasjonV2> = emptyList(),
    val beloepTilUtbetaling: BigDecimalJson = BigDecimal.ZERO,
    val ekstraInfo: Set<EkstraInfo> = emptySet(),
) {
    constructor(beregning: Beregning) : this(
        id = beregning.id,
        periode = beregning.periode,
        status = beregning.status,
        statusflagg = beregning.statusflagg,
        overfoeres = beregning.overfoeres,
        fagomraade = beregning.fagomraade,
        gjelder = beregning.gjelder,
        beregnet = beregning.beregnet,
        utbetalesTil = beregning.utbetalesTil,
        delberegninger = beregning.delberegning.map(::DelBeregningsPresentasjonV2),
        beloepTilUtbetaling = beregning.delberegning.sumOf { it.sum },
        ekstraInfo = beregning.ekstraInfo,
    )
}
