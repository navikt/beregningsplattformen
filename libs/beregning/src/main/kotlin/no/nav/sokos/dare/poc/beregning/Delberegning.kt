package no.nav.sokos.dare.poc.beregning

import java.math.BigDecimal

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

import no.nav.sokos.dare.poc.beregning.Delberegning.Companion.COLUMN_HEADER_WIDTH
import no.nav.sokos.dare.poc.beregning.Delberegning.Companion.COLUMN_WIDTH
import no.nav.sokos.dare.poc.beregning.dataklasser.Satstype
import no.nav.sokos.dare.poc.oppdrag.OppdragId
import no.nav.sokos.dare.poc.oppdrag.OppdragslinjekjedeId
import no.nav.sokos.dare.poc.structural.IdValueClass
import no.nav.sokos.dare.poc.trekk.TrekkvedtakId

@Serializable
data class Delberegning(
    val id: DelberegningId? = null,
    val beregningId: BeregningId? = null,
    val oppdragId: OppdragId? = null,
    val linjer: List<Delberegningslinje>,
    val datoIntervall: DatoIntervall,
    val eksternRef: Long? = null,
    val ekstraInfo: Set<EkstraInfo> = emptySet(),
) {
    private val bokforendeLinjer: List<Delberegningslinje> by lazy {
        linjer.filter { it.bokforende }
    }

    val sums: Delberegningslinje by lazy {
        val linjerMedDagbeloep = bokforendeLinjer.filterNot { it.dagsbeloep.isEmpty() }
        Delberegningslinje(
            id = null,
            delberegningId = null,
            type = DelberegningType.SUM,
            dagsbeloep =
                datoIntervall.dager.mapIndexed { i, dag ->
                    Dagbeloep(dato = dag, beloep = linjerMedDagbeloep.sumOf { it.dagsbeloep[i].beloep })
                },
            beloep = bokforendeLinjer.sumOf { it.beloep },
            ekstra = emptySet<EkstraInfo>(),
        )
    }
    val sumColumn: List<BigDecimal> by lazy {
        linjer.map { if (it.bokforende) it.sum else BigDecimal.ZERO }
    }

    init {
        val linjerMedDagbeloep = linjer.filterNot { it.dagsbeloep.isEmpty() }
        require(
            linjerMedDagbeloep.all {
                it.dagsbeloep.size == datoIntervall.dager.size || it.dagsbeloep.isEmpty()
            },
        ) { "En delberegningslinje må ha ingen eller like mange dagbeløp som antall dager i delperioden." }
    }

    constructor(linjer: List<Delberegningslinje>, datoIntervall: DatoIntervall, ekstraInfo: List<EkstraInfo> = emptyList()) : this(
        null,
        null,
        null,
        linjer,
        datoIntervall,
    )

    val sum: BigDecimal by lazy {
        bokforendeLinjer.sumOf { it.sum }
    }

    override fun toString(): String =
        buildString {
            append(" ".repeat(COLUMN_HEADER_WIDTH))
            append(datoIntervall.dager.joinToString("") { it.dagStr().padStart(COLUMN_WIDTH) })
            append(" ".repeat(COLUMN_WIDTH))
            append("sum".padStart(COLUMN_WIDTH))
            append("\n")
            linjer.forEach {
                append(it.format(datoIntervall.dager.size))
            }
            append(sums.format(datoIntervall.dager.size))
        }

    companion object {
        const val COLUMN_HEADER_WIDTH = 14
        const val COLUMN_WIDTH = 8
    }
}

@Serializable
data class Delberegningslinje(
    val id: DelberegningslinjeId? = null,
    val delberegningId: DelberegningId? = null,
    val type: DelberegningType,
    val dagsbeloep: List<Dagbeloep>,
    @Contextual val beloep: BigDecimal, // TODO: Fra beregning
    val ekstra: Set<EkstraInfo> = emptySet(),
    @Transient val kapasitet: Kapasitet? = null, // TODO: detaljer som ikke lagres?
    val satsType: Satstype? = null,
    @Contextual val sats: BigDecimal? = null,
    val trekkvedtakId: TrekkvedtakId? = null,
    val oppdragslinjekjedeId: OppdragslinjekjedeId? = null,
    val skattType: String? = null,
    val eksternRef: Long? = null,
    val kodeKlasse: LinjeKlassekode? = null,
    @SerialName("reskontro_konto")
    val reskontroKonto: String? = null,
) {
    val bokforende: Boolean
        get() = type.bokforende

    // Bare verdi per dag for delberegningslinje
    constructor(type: DelberegningType, dagsbeloep: List<Dagbeloep>, ekstra: Set<EkstraInfo>, capacity: Kapasitet? = null) : this(
        id = null,
        delberegningId = null,
        type = type,
        dagsbeloep = dagsbeloep,
        beloep = BigDecimal.ZERO,
        ekstra = ekstra,
    )

    // Bare én verdi for delberegsningslinjen
    constructor(type: DelberegningType, rowValue: BigDecimal, ekstra: Set<EkstraInfo>, capacity: Kapasitet? = null) : this(type, rowValue, emptyList(), ekstra)

    // Verdi per dag og verdi for linjen. TODO: Denne forsvinner kanskje pga. måten en beregning deler opp delberegninger.
    constructor(
        type: DelberegningType,
        rowValue: BigDecimal,
        dagsbeloep: List<Dagbeloep>,
        ekstra: Set<EkstraInfo>,
        capacity: Kapasitet? = null,
    ) : this(id = null, delberegningId = null, type = type, dagsbeloep = dagsbeloep, beloep = rowValue, ekstra = ekstra)

    fun hasColumnValues() = !(dagsbeloep.isEmpty() || dagsbeloep.all { it == BigDecimal.ZERO })

    val sum: BigDecimal by lazy {
        if (hasColumnValues()) (dagsbeloep.sum() + beloep) else beloep
    }

    fun format(columns: Int): String =
        buildString {
            val label = if (kodeKlasse != null) "${type.name} ${kodeKlasse.visningskode}" else type.name
            append(label.padEnd(COLUMN_HEADER_WIDTH))
            if (hasColumnValues()) {
                append(dagsbeloep.joinToString("") { it.beloep.toPlainString().padStart(COLUMN_WIDTH) })
            } else {
                append(" ".repeat(COLUMN_WIDTH * columns))
            }
            append(beloep.toPlainString().padStart(COLUMN_WIDTH))
            append(sum.toPlainString().padStart(COLUMN_WIDTH)).append('\n')
        }

    override fun toString(): String = format(0)

    operator fun unaryMinus() = copy(dagsbeloep = dagsbeloep.map { -it }, beloep = -beloep)

    companion object {
        /**
         * Opprett fra en BeregningsBeloep-kilde.
         * Hvis kilden har dagsverdier, populeres dagsbeloep.
         * Hvis kilden kun har periodeBeloep, brukes beloep (tom dagsbeloep).
         */
        fun create(
            periode: DatoIntervall, // TODO: Det er ingen grunn til at Delberegningslinje skal trenge å få periode inn
            type: DelberegningType,
            kilde: BeregningsBeloep,
            ekstraInfo: Set<EkstraInfo>,
            kapasitet: Kapasitet,
            satstype: Satstype? = null,
            sats: BigDecimal? = null,
            oppdragslinjekjedeId: OppdragslinjekjedeId? = null,
            trekkvedtakId: TrekkvedtakId? = null,
            kodeKlasse: LinjeKlassekode? = null,
            reskontroKonto: String? = null,
        ): Delberegningslinje {
            // Kunne ha vært en when på implementasjonen av kilde, men vi har klasser som
            // har både dag-verdier og periodebeløp inntil vi har refaktorert litt mer
            val hasDagValues = periode.dager.any { kilde.forDag(it).compareTo(BigDecimal.ZERO) != 0 }
            return if (hasDagValues) {
                Delberegningslinje(
                    type = type,
                    dagsbeloep = periode.dager.map { Dagbeloep(dato = it, beloep = kilde.forDag(it)) },
                    beloep = kilde.periodeBeloep,
                    ekstra = ekstraInfo,
                    kapasitet = kapasitet,
                    satsType = satstype,
                    sats = sats,
                    oppdragslinjekjedeId = oppdragslinjekjedeId,
                    trekkvedtakId = trekkvedtakId,
                    kodeKlasse = kodeKlasse,
                    reskontroKonto = reskontroKonto,
                )
            } else {
                Delberegningslinje(
                    type = type,
                    dagsbeloep = emptyList(),
                    beloep = kilde.periodeBeloep,
                    ekstra = ekstraInfo,
                    kapasitet = kapasitet,
                    satsType = satstype,
                    sats = sats,
                    oppdragslinjekjedeId = oppdragslinjekjedeId,
                    trekkvedtakId = trekkvedtakId,
                    kodeKlasse = kodeKlasse,
                    reskontroKonto = reskontroKonto,
                )
            }
        }
    }
}

/**
 * Denne enumen er også å finne som en constraint delberegningslinje_gyldig_type i databasen fordi den brukes til å lage constraints, som at trekk må peke på trekkperiode/trekkvedtak
 */
@Serializable
enum class DelberegningType {
    SUM,
    YTELSE,
    TIDLIGERE_UTBETALT,
    SKATT,
    TREKK,
    FRIKORT_BRUK,
    ;

    val bokforende: Boolean
        get() =
            when (this) {
                SUM,
                YTELSE,
                TIDLIGERE_UTBETALT,
                SKATT,
                TREKK,
                -> true
                FRIKORT_BRUK -> false
            }
}

operator fun List<Delberegningslinje>.unaryMinus() = map { -it }

@Serializable
@JvmInline
value class DelberegningId(
    override val id: Long,
) : IdValueClass

@Serializable
@JvmInline
value class DelberegningslinjeId(
    override val id: Long,
) : IdValueClass
