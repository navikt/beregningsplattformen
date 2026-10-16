package no.nav.utbetaling.beregning.tid

import java.time.LocalDate

interface Tidslinjesamling<K, V> : Iterable<Pair<Periode, Map<K, V>>> {
    val tidslinjer: Map<K, Tidslinje<V>>

    operator fun get(dato: LocalDate): Map<K, V>

    operator fun contains(dato: LocalDate): Boolean

    fun isEmpty(): Boolean

    fun tidslinjeIterator(): Iterator<Tidslinje<V>>

    fun perioderIterator(): Iterable<Pair<Periode, List<V>>> = map { (periode, verdier) -> periode to verdier.values.toList() }

    fun <U, R> kombinerMed(
        other: Tidslinjesamling<K, U>,
        transform: (nøkkel: K, denne: V?, andre: U?) -> R?,
    ): Tidslinjesamling<K, R>
}

class DefaultTidslinjesamling<K, V>(
    override val tidslinjer: Map<K, Tidslinje<V>>,
) : Tidslinjesamling<K, V> {
    constructor(tidslinjer: List<Tidslinje<V>>, nøkkel: (V) -> K) : this(
        tidslinjer
            .filterNot { it.isEmpty() }
            .groupBy { tidslinje ->
                val nøkler = tidslinje.map { (_, verdi) -> nøkkel(verdi) }.toSet()
                require(nøkler.size == 1) {
                    "En tidslinje kan ikke grupperes på flere nøkler, men ble gruppert på: $nøkler"
                }
                nøkler.single()
            }.mapValues { (nøkkel, gruppe) ->
                require(gruppe.size == 1) {
                    "Flere tidslinjer ble gruppert på samme nøkkel: $nøkkel"
                }
                gruppe.single()
            },
    )

    private val kombinert: Tidslinje<Map<K, V>> by lazy {
        tidslinjer.entries.fold(Tidslinje.Builder<Map<K, V>>().build()) { samlet, (nøkkel, tidslinje) ->
            samlet.kombinerMed(tidslinje) { _, gjeldende, verdi ->
                (gjeldende.orEmpty() + listOfNotNull(verdi?.let { nøkkel to it })).takeIf { it.isNotEmpty() }
            }
        }
    }

    override fun iterator(): Iterator<Pair<Periode, Map<K, V>>> = kombinert.iterator()

    override operator fun get(dato: LocalDate): Map<K, V> = kombinert[dato].orEmpty()

    override operator fun contains(dato: LocalDate) = dato in kombinert

    override fun isEmpty() = kombinert.isEmpty()

    override fun tidslinjeIterator(): Iterator<Tidslinje<V>> = tidslinjer.values.iterator()

    override fun <U, R> kombinerMed(
        other: Tidslinjesamling<K, U>,
        transform: (nøkkel: K, denne: V?, andre: U?) -> R?,
    ): Tidslinjesamling<K, R> {
        val kombinert =
            (tidslinjer.keys + other.tidslinjer.keys).associateWith { nøkkel ->
                val denneTidslinjen = tidslinjer[nøkkel] ?: Tidslinje.Builder<V>().build()
                val andreTidslinjen = other.tidslinjer[nøkkel] ?: Tidslinje.Builder<U>().build()
                denneTidslinjen.kombinerMed(andreTidslinjen) { _, denne, andre -> transform(nøkkel, denne, andre) }
            }

        return DefaultTidslinjesamling(kombinert.filterValues { !it.isEmpty() })
    }

    override fun equals(other: Any?) = this === other || (other is DefaultTidslinjesamling<*, *> && tidslinjer == other.tidslinjer)

    override fun hashCode() = tidslinjer.hashCode()

    override fun toString() =
        when {
            isEmpty() -> "Tom tidslinjesamling"
            else -> kombinert.toString()
        }
}
