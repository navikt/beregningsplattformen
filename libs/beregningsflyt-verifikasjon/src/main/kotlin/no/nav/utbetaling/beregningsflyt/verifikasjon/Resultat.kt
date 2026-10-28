package no.nav.utbetaling.beregningsflyt.verifikasjon

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Resultat(
    val periode: Periode,
    val beregninger: List<Beregning> = emptyList(),
    val restFrikortBeloep: Int? = null,
    val initielleSaldoBoetter: List<SaldoBoette> = emptyList(),
    val initielleSatsBoetter: List<SatsBoette> = emptyList(),
    val saldoBoetter: List<SaldoBoette> = emptyList(),
    val satsBoetter: List<SatsBoette> = emptyList(),
)

@Serializable
data class Beregning(
    val periode: Periode,
    val status: String,
    val statusflagg: List<String> = emptyList(),
    val overfoeres: String? = null,
    val fagomraade: String,
    val gjelder: Int,
    val beregnet: String,
    val beloepTilUtbetaling: Int? = null,
    val delberegninger: List<Delberegning> = emptyList(),
    val ekstraInfo: List<EkstraInfo> = emptyList(),
)

@Serializable
data class Delberegning(
    val oppdragId: Int,
    val columns: List<String> = emptyList(),
    val rows: List<Rad> = emptyList(),
    val ekstraInfo: List<EkstraInfo> = emptyList(),
    val datoIntervall: Periode? = null,
    val sumColumn: List<Int> = emptyList(),
    val sums: Rad? = null,
)

@Serializable
data class Rad(
    val type: String,
    val rowName: String,
    val columnValues: List<Int> = emptyList(),
    val rowValue: Int? = null,
    val ekstra: List<EkstraInfo> = emptyList(),
    val capacity: Kapasitet? = null,
    val satstype: String? = null,
    val sats: Double? = null,
    val oppdragslinjekjedeId: Int? = null,
    val kodeKlasse: String? = null,
    val grunnlag: Int? = null,
    @SerialName("reskontro_konto")
    val reskontroKonto: String? = null,
    val trekkVedtakId: Int? = null,
)

@Serializable
data class Kapasitet(
    val dayCapacity: List<Int> = emptyList(),
    val lumpCapacity: Int = 0,
)

@Serializable
data class SaldoBoette(
    val trekkvedtakId: Int,
    val opprinneligSaldo: Int,
    val restSaldo: Int,
)

@Serializable
data class SatsBoette(
    val trekkperiodeId: Int,
    val boette: String,
    val opprinneligSats: Int? = null,
    val restSats: Int,
)
