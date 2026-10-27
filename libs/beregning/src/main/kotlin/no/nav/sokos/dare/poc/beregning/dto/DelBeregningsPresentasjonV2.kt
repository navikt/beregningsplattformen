package no.nav.sokos.dare.poc.beregning.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import no.nav.sokos.dare.poc.BigDecimalJson
import no.nav.sokos.dare.poc.LocalDateJson
import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.beregning.Delberegning
import no.nav.sokos.dare.poc.beregning.DelberegningType
import no.nav.sokos.dare.poc.beregning.Delberegningslinje
import no.nav.sokos.dare.poc.beregning.EkstraInfo
import no.nav.sokos.dare.poc.beregning.Kapasitet
import no.nav.sokos.dare.poc.beregning.dataklasser.Satstype
import no.nav.sokos.dare.poc.oppdrag.OppdragId
import no.nav.sokos.dare.poc.oppdrag.OppdragslinjekjedeId

@Serializable
data class DelBeregningsPresentasjonV2(
    val oppdragId: OppdragId? = null,
    val columns: List<LocalDateJson>,
    val rows: List<Linje>,
    val datoIntervall: DatoIntervall,
    val ekstraInfo: Set<EkstraInfo> = emptySet(),
    val sums: Linje,
    val sumColumn: List<BigDecimalJson>,
) {
    constructor(delberegning: Delberegning) : this(
        oppdragId = delberegning.oppdragId,
        columns = delberegning.datoIntervall.dager,
        rows = delberegning.linjer.map { Linje(it) },
        datoIntervall = delberegning.datoIntervall,
        ekstraInfo = delberegning.ekstraInfo,
        sums = Linje(delberegning.sums),
        sumColumn = delberegning.sumColumn,
    )
}

@Serializable
data class Linje(
    val type: DelberegningType,
    val rowName: String,
    val columnValues: List<BigDecimalJson>,
    val rowValue: BigDecimalJson,
    val ekstra: Set<EkstraInfo>,
    val capacity: Capacity? = null,
    val satstype: Satstype? = null,
    val sats: BigDecimalJson? = null,
    val oppdragslinjekjedeId: OppdragslinjekjedeId? = null,
    val trekkVedtakId: Long? = null,
    val skattType: String? = null,
    val kodeKlasse: String? = null,
    @SerialName("reskontro_konto")
    val reskontroKonto: String? = null,
) {
    constructor(d: Delberegningslinje) : this(
        d.type,
        rowName =
            if (d.kodeKlasse != null) {
                "${d.type.name.lowercase().replaceFirstChar(Char::uppercase)} ${d.kodeKlasse.visningskode}"
            } else {
                d.type.name
                    .lowercase()
                    .replaceFirstChar(Char::uppercase)
            },
        columnValues = d.dagsbeloep.map { it.beloep },
        rowValue = d.beloep,
        ekstra = d.ekstra,
        capacity = d.kapasitet?.let { Capacity(it) },
        satstype = d.satsType,
        sats = d.sats,
        oppdragslinjekjedeId = d.oppdragslinjekjedeId,
        trekkVedtakId = d.trekkvedtakId?.id,
        skattType = d.skattType,
        kodeKlasse = d.kodeKlasse?.kode,
        reskontroKonto = d.reskontroKonto,
    )
}

@Serializable
data class Capacity(
    val dayCapacity: List<BigDecimalJson>? = null,
    val lumpCapacity: BigDecimalJson? = null,
) {
    constructor(kapasitet: Kapasitet) : this(
        kapasitet.dagbeloep.keys
            .sorted()
            .map { kapasitet.dagbeloep[it]!! },
        kapasitet.periodeBeloep,
    )
}
