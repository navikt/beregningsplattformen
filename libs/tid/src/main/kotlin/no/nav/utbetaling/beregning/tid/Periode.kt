package no.nav.utbetaling.beregning.tid

import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.LocalDate
import java.time.format.DateTimeFormatter

sealed interface Periode {
    val fom: LocalDate

    operator fun contains(value: LocalDate): Boolean

    operator fun contains(other: Periode): Boolean = fom <= other.fom && rekkerTilOgMed(other.sisteDag)

    fun overlapperMed(other: Periode): Boolean = maxOf(fom, other.fom).let { it in this && it in other }

    fun inneholderNoenAv(datoer: Iterable<LocalDate>) = datoer.any { it in this }

    fun overlappendePeriode(other: Periode): Periode? {
        if (!overlapperMed(other)) return null
        val senesteFom = maxOf(fom, other.fom)
        val tidligsteTom = listOfNotNull(sisteDag, other.sisteDag).minOrNull()
        return periode(senesteFom, tidligsteTom)
    }

    fun omsluttende(other: Periode): Periode {
        val egenTom = sisteDag
        val annenTom = other.sisteDag
        val senesteTom = if (egenTom == null || annenTom == null) null else maxOf(egenTom, annenTom)
        return periode(minOf(fom, other.fom), senesteTom)
    }

    /** Siste dag i perioden, eller null når den er åpen. Privat fordi bare de delte operasjonene under trenger begrepet. */
    private val sisteDag: LocalDate?
        get() =
            when (this) {
                is ÅpenPeriode -> null
                is LukketPeriode -> tom
            }

    private fun rekkerTilOgMed(dato: LocalDate?): Boolean {
        val egenSisteDag = sisteDag ?: return true
        return dato != null && egenSisteDag >= dato
    }

    data class ÅpenPeriode(
        override val fom: LocalDate,
    ) : Periode {
        override fun contains(value: LocalDate) = value >= fom

        override fun toString() = "Fra og med " + fom.format(FORMATTER)
    }

    data class LukketPeriode(
        override val fom: LocalDate,
        val tom: LocalDate,
    ) : Periode,
        ClosedRange<LocalDate>,
        Iterable<LocalDate> {
        override val start: LocalDate get() = fom
        override val endInclusive: LocalDate get() = tom

        init {
            require(fom <= tom) { "fom ($fom) kan ikke være etter tom ($tom)" }
        }

        override fun contains(value: LocalDate) = value in fom..tom

        override fun iterator() = generateSequence(fom) { dag -> dag.nesteDag.takeIf { it <= tom } }.iterator()

        override fun toString(): String = fom.format(FORMATTER) + " til " + tom.format(FORMATTER)

        private fun merge(
            other: LukketPeriode,
            erForlengelse: (forrigeDag: LocalDate, nesteDag: LocalDate) -> Boolean,
        ): LukketPeriode? {
            val hengerSammen = overlapperMed(other) || erForlengelse(tom, other.fom)
            return LukketPeriode(fom, maxOf(tom, other.tom)).takeIf { hengerSammen }
        }

        companion object {
            infix fun LocalDate.til(tom: LocalDate) = LukketPeriode(this, tom)

            fun LocalDate.somLukketPeriode() = this til this

            val LocalDate.forrigeDag get() = minusDays(1)

            val LocalDate.nesteDag get() = plusDays(1)

            fun Iterable<Periode>.overlapper(): Boolean =
                sortedBy { it.fom }
                    .zipWithNext()
                    .any { (nåværende, neste) -> nåværende.overlapperMed(neste) }

            fun Iterable<LukketPeriode>.grupperSammenhengendePerioder() = merge(MERGE_KANT_I_KANT)

            fun Iterable<LukketPeriode>.grupperSammenhengendePerioderMedHensynTilHelg() = merge(MERGE_OVER_HELG)

            private fun Iterable<LukketPeriode>.merge(erForlengelse: (forrigeDag: LocalDate, nesteDag: LocalDate) -> Boolean): List<LukketPeriode> =
                sortedBy { it.fom }
                    .fold(mutableListOf()) { resultat, neste ->
                        when (val sammenslått = resultat.lastOrNull()?.merge(neste, erForlengelse)) {
                            null -> resultat.add(neste)
                            else -> resultat[resultat.lastIndex] = sammenslått
                        }
                        resultat
                    }

            private val MERGE_KANT_I_KANT = { venstre: LocalDate, høyre: LocalDate -> venstre.nesteDag == høyre }

            private val MERGE_OVER_HELG = { venstre: LocalDate, høyre: LocalDate -> venstre.erRettFørMedHensynTilHelg(høyre) }

            private fun LocalDate.erRettFørMedHensynTilHelg(other: LocalDate) = nesteDag == other || (nesteDag < other && (nesteDag til other.forrigeDag).all { it.erHelg })

            private val LocalDate.erHelg get() = dayOfWeek == SATURDAY || dayOfWeek == SUNDAY
        }
    }

    private companion object {
        val FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

        fun periode(
            fom: LocalDate,
            tom: LocalDate?,
        ): Periode = if (tom == null) ÅpenPeriode(fom) else LukketPeriode(fom, tom)
    }
}
