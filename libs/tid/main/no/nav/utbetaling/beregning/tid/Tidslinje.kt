package no.nav.utbetaling.beregning.tid

import no.nav.utbetaling.beregning.tid.Periode.LukketPeriode
import no.nav.utbetaling.beregning.tid.Periode.LukketPeriode.Companion.forrigeDag
import no.nav.utbetaling.beregning.tid.Periode.LukketPeriode.Companion.grupperSammenhengendePerioder
import no.nav.utbetaling.beregning.tid.Periode.LukketPeriode.Companion.nesteDag
import no.nav.utbetaling.beregning.tid.Periode.LukketPeriode.Companion.overlapper
import no.nav.utbetaling.beregning.tid.Periode.LukketPeriode.Companion.somLukketPeriode
import no.nav.utbetaling.beregning.tid.Periode.LukketPeriode.Companion.til
import no.nav.utbetaling.beregning.tid.Periode.ÅpenPeriode
import java.time.LocalDate
import java.util.TreeMap

class Tidslinje<T> private constructor(
    private val knekkpunkter: TreeMap<LocalDate, T?>,
) : Iterable<Pair<Periode, T>> {
    operator fun contains(dato: LocalDate) = get(dato) != null

    fun isEmpty() = knekkpunkter.isEmpty()

    operator fun get(dato: LocalDate): T? = knekkpunkter.floorEntry(dato)?.value

    fun begrensetTil(dato: LocalDate): Tidslinje<T> {
        if (isEmpty()) return this
        val (sisteDato, sisteVerdi) = knekkpunkter.lastEntry()
        if (sisteVerdi == null && sisteDato.forrigeDag <= dato) return this
        val nye = TreeMap(knekkpunkter.headMap(dato.nesteDag))
        nye[dato.nesteDag] = null
        return Tidslinje(nye.komprimert())
    }

    fun <R> mapVerdier(transform: (T) -> R?): Tidslinje<R> {
        val nye = TreeMap<LocalDate, R?>()
        knekkpunkter.forEach { (dato, verdi) -> nye[dato] = verdi?.let(transform) }
        return Tidslinje(nye.komprimert())
    }

    fun <U, R> kombinerMed(
        other: Tidslinje<U>,
        transform: (periode: Periode, denne: T?, andre: U?) -> R?,
    ): Tidslinje<R> {
        val grenser =
            sortedSetOf<LocalDate>().apply {
                addAll(knekkpunkter.keys)
                addAll(other.knekkpunkter.keys)
            }
        val nye = TreeMap<LocalDate, R?>()
        grenser.forEach { dato ->
            val periode = grenser.higher(dato)?.let { dato til it.forrigeDag } ?: ÅpenPeriode(dato)
            nye[dato] = transform(periode, this[dato], other[dato])
        }
        return Tidslinje(nye.komprimert())
    }

    override fun iterator(): Iterator<Pair<Periode, T>> =
        knekkpunkter.entries
            .asSequence()
            .mapNotNull { (fom, verdi) -> verdi?.let { periodeFra(fom) to it } }
            .iterator()

    private fun periodeFra(fom: LocalDate): Periode = knekkpunkter.higherKey(fom)?.let { fom til it.forrigeDag } ?: ÅpenPeriode(fom)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Tidslinje<*>) return false
        return this.knekkpunkter == other.knekkpunkter
    }

    override fun hashCode() = knekkpunkter.hashCode()

    override fun toString(): String {
        val type = firstOrNull()?.second?.let { it::class.simpleName } ?: return "Tom Tidslinje"
        return joinToString(prefix = "Tidslinje med $type\n", separator = "") { (periode, verdi) -> "$periode\n\t $verdi\n" }
    }

    class Builder<T> {
        private val lukkedePerioder = mutableMapOf<T, MutableList<LukketPeriode>>()
        private val åpnePerioder = mutableListOf<Pair<ÅpenPeriode, T>>()

        fun dato(
            dato: LocalDate,
            verdi: T,
        ) = periode(dato.somLukketPeriode(), verdi)

        fun periode(
            periode: Periode,
            verdi: T,
        ) = apply {
            when (periode) {
                is LukketPeriode -> lukkedePerioder.getOrPut(verdi) { mutableListOf() }.add(periode)
                is ÅpenPeriode -> åpnePerioder.add(periode to verdi)
            }
        }

        fun build(): Tidslinje<T> {
            val åpneSortert = åpnePerioder.sortedBy { it.first.fom }
            krevÉnVerdiPerÅpneStart(åpneSortert)
            val hale = åpneSortert.lastOrNull()

            // Alle åpne perioder utenom halen lukkes dagen før den neste åpne starter
            val lukketFraÅpne =
                åpneSortert
                    .zipWithNext()
                    .filterNot { (denne, neste) -> denne.first.fom == neste.first.fom }
                    .map { (denne, neste) -> denne.second to (denne.first.fom til neste.first.fom.forrigeDag) }

            val lukkede =
                (lukkedePerioder.flatMap { (verdi, perioder) -> perioder.map { verdi to it } } + lukketFraÅpne)
                    .groupBy({ it.first }, { it.second })
                    .flatMap { (verdi, perioder) -> perioder.grupperSammenhengendePerioder().map { it to verdi } }
                    .sortedBy { it.first.fom }

            require(!lukkede.map { it.first }.overlapper()) {
                "Overlappende perioder for ulike verdier!"
            }

            val sisteTom = lukkede.maxOfOrNull { it.first.tom }
            if (hale != null && sisteTom != null) {
                require(hale.first.fom > sisteTom) {
                    "En åpen periode må starte etter siste lukkede periode! Siste er $sisteTom, den åpne starter ${hale.first.fom}"
                }
            }

            val knekkpunkter = TreeMap<LocalDate, T?>()
            lukkede.forEach { (periode, verdi) ->
                knekkpunkter[periode.fom] = verdi
                knekkpunkter[periode.tom.nesteDag] = null
            }
            hale?.let { (periode, verdi) -> knekkpunkter[periode.fom] = verdi }

            return Tidslinje(knekkpunkter.komprimert())
        }

        private fun krevÉnVerdiPerÅpneStart(åpneSortert: List<Pair<ÅpenPeriode, T>>) {
            åpneSortert.zipWithNext().forEach { (denne, neste) ->
                if (denne.first.fom == neste.first.fom) {
                    require(denne.second == neste.second) {
                        "To åpne perioder kan ikke starte på samme dato med ulik verdi! Gjelder ${denne.first.fom}"
                    }
                }
            }
        }
    }

    private companion object {
        fun <T> TreeMap<LocalDate, T?>.komprimert(): TreeMap<LocalDate, T?> {
            val resultat = TreeMap<LocalDate, T?>()
            var forrigeVerdi: T? = null
            forEach { (dato, verdi) ->
                if (verdi != forrigeVerdi) {
                    resultat[dato] = verdi
                    forrigeVerdi = verdi
                }
            }
            return resultat
        }
    }
}
