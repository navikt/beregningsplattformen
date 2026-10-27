package no.nav.sokos.dare.poc.beregning

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import java.util.TreeSet

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

import com.fasterxml.jackson.annotation.JsonIgnore

import no.nav.sokos.dare.poc.LocalDateJson

@Serializable
data class DatoIntervall(
    val fom: LocalDateJson,
    val tom: LocalDateJson? = null,
) {
    @Transient
    @get:JsonIgnore
    val dager: List<LocalDate> by lazy {
        val tilOgMed = requireNotNull(tom) { "Åpent intervall kan ikke liste dager" }
        fom.datesUntil(tilOgMed.plusDays(1)).toList()
    }

    @Transient
    @get:JsonIgnore
    val hverdager: List<LocalDate> by lazy {
        dager.filter(LocalDate::isWeekday)
    }

    constructor(fom: String, tom: String? = null) : this(
        LocalDate.parse(fom),
        if (tom != null) LocalDate.parse(tom) else null,
    )

    fun contains(dag: LocalDate): Boolean = !dag.isBefore(fom) && (tom == null || (!tom.isBefore(dag)))

    fun overlaps(that: DatoIntervall) = !disjunct(that)

    // Divide this interval by other intervals. Returns an ordered list of contiguous intervals that covers the old interval.
    fun divide(vararg others: List<DatoIntervall>): List<DatoIntervall> {
        val delingsgrenser = TreeSet<LocalDate>()
        others.forEach {
            it.forEach { other ->
                if (fom.isBefore(other.fom) && (tom == null || tom.isAfter(other.fom))) {
                    delingsgrenser.add(other.fom)
                }
                if (other.tom != null) {
                    if (this.tom == null || other.tom.isBefore(this.tom)) {
                        val dayAfter = other.tom.plusDays(1)
                        delingsgrenser.add(dayAfter)
                    }
                }
            }
        }
        val segments = mutableListOf<DatoIntervall>()
        var current = fom

        delingsgrenser.forEach { cut ->
            if (cut.isAfter(current)) {
                segments.add(DatoIntervall(current, cut.minusDays(1)))
                current = cut
            }
        }
        segments.add(DatoIntervall(current, tom))
        return segments
    }

    // A contains B if all of B is inside of A
    fun contains(other: DatoIntervall): Boolean =
        !fom.isAfter(other.fom) &&
            when {
                tom == null -> true
                other.tom == null -> false
                else -> !tom.isBefore(other.tom)
            }

    // An intersection is the part of two intervals that overlap
    fun intersection(other: DatoIntervall): DatoIntervall? =
        if (disjunct(other)) {
            null
        } else {
            DatoIntervall(
                fom = maxOf(fom, other.fom),
                tom =
                    when {
                        tom == null -> other.tom // This is open, use other's boundary
                        other.tom == null -> tom // Other is open, use this boundary
                        else -> minOf(tom, other.tom) // Both closed → minimum
                    },
            )
        }

    fun partsNotOverlappingWith(other: DatoIntervall): List<DatoIntervall> {
        if (!overlaps(other)) return listOf(this)

        val o = intersection(other) ?: return listOf(this)

        val parts = mutableListOf<DatoIntervall>()

        if (fom.isBefore(o.fom)) {
            val leftEnd = o.fom.minusDays(1)
            parts.add(DatoIntervall(fom, leftEnd))
        }

        val overlapEnd = o.tom

        if (this.tom == null) {
            if (overlapEnd != null) {
                val rightStart = overlapEnd.plusDays(1)
                parts.add(DatoIntervall(rightStart, null))
            }
        } else {
            if (overlapEnd != null) {
                val rightStart = overlapEnd.plusDays(1)
                if (!rightStart.isAfter(this.tom)) {
                    parts.add(DatoIntervall(rightStart, this.tom))
                }
            }
        }
        return parts
    }

    fun disjunct(that: DatoIntervall) =
        when {
            that.tom != null && that.tom.isBefore(this.fom) -> true
            this.tom != null && this.tom.isBefore(that.fom) -> true
            else -> false
        }

    fun isOpen(): Boolean = tom == null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DatoIntervall) return false
        return fom == other.fom && tom == other.tom
    }

    override fun hashCode(): Int {
        var result = fom.hashCode()
        result = 31 * result + (tom?.hashCode() ?: 0)
        return result
    }

    override fun toString() = "Date[$fom -> $tom]"

    fun years(): List<DatoIntervall> {
        require(tom != null) { "Åpent intervall har uendelige år" }
        return (fom.year..tom.year).map { year ->
            DatoIntervall(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31))
        }
    }

    fun months(): List<DatoIntervall> {
        require(tom != null) { "Åpent intervall har uendelige måneder" }
        var start = fom.withDayOfMonth(1)
        return buildList {
            while (!start.isAfter(tom)) {
                val end = start.plusMonths(1)
                add(DatoIntervall(start, end.minusDays(1)))
                start = end
            }
        }
    }

    companion object {
        fun omsluttendeIntervall(intervaller: List<DatoIntervall>): DatoIntervall {
            val fom = intervaller.minOf { it.fom }
            val tom =
                if (intervaller.any { it.isOpen() }) {
                    null
                } else {
                    intervaller.maxOf { it.tom ?: LocalDate.MAX } // MAX brukes ikke, vi returnerer åpent intervall
                }
            return DatoIntervall(fom, tom)
        }
    }
}

fun List<DatoIntervall>.omsluttendeIntervall() = DatoIntervall.omsluttendeIntervall(this)

fun LocalDate.isWeekday(): Boolean = dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY

fun LocalDate.dagStr(): String = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.of("NO"))

fun LocalDate.isAfter(di: DatoIntervall): Boolean = di.tom != null && this.isAfter(di.tom)

fun LocalDate.nesteFredag(): LocalDate {
    val daysUntilFriday = (DayOfWeek.FRIDAY.value - dayOfWeek.value + 7) % 7
    return this.plusDays(daysUntilFriday.toLong())
}

fun LocalDate.sisteDagIMnd(): LocalDate = this.withDayOfMonth(this.lengthOfMonth())
