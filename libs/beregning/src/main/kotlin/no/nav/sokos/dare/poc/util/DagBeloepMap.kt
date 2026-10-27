package no.nav.sokos.dare.poc.util

import java.math.BigDecimal
import java.time.LocalDate

/**
 * Utilityklasse som vedlikeholder et map mellom en dato og en BigDecimal. Kan brukes til å gjøre addisjon/substraksjon mellom sett av dato-beløp tupler.
 **/
class DagBeloepMap(
    val beloep: Map<LocalDate, BigDecimal>,
) : Map<LocalDate, BigDecimal> by beloep {
    fun sum(): BigDecimal = values.sumOf { it }

    // Skiller seg fra klassisk map ved at alle datoer ikke i map'en returnerer ZERO, ikke null.
    override operator fun get(key: LocalDate) = beloep[key] ?: BigDecimal.ZERO

    operator fun plus(beloep: DagBeloepMap): DagBeloepMap = DagBeloepMap(MutableDagBeloepMap(this).plus(beloep).toMap())

    operator fun minus(beloep: DagBeloepMap): DagBeloepMap = DagBeloepMap(MutableDagBeloepMap(this).minus(beloep).toMap())

    override fun toString(): String =
        buildString {
            append("BeloepPerDag[")
            append(keys.toList().sorted().joinToString { key -> "$key:${(beloep[key] ?: BigDecimal.ZERO).toPlainString()}" })
            append("]")
        }
}

class MutableDagBeloepMap(
    val beloep: MutableMap<LocalDate, BigDecimal>,
) : MutableMap<LocalDate, BigDecimal> by beloep {
    constructor() : this(mutableMapOf<LocalDate, BigDecimal>())
    constructor(beloepPerDag: DagBeloepMap) : this(beloepPerDag.toMutableMap())

    fun sum(): BigDecimal = values.sumOf { it }

    override operator fun get(key: LocalDate) = beloep[key] ?: BigDecimal.ZERO

    operator fun plus(beloepPerDag: DagBeloepMap): MutableDagBeloepMap {
        beloepPerDag.forEach { entry ->
            beloep[entry.key] = (beloep[entry.key] ?: BigDecimal.ZERO) + entry.value
        }
        return this
    }

    operator fun minus(beloepPerDag: DagBeloepMap): MutableDagBeloepMap {
        beloepPerDag.forEach { entry ->
            beloep[entry.key] = (beloep[entry.key] ?: BigDecimal.ZERO) - entry.value
        }
        return this
    }

    override fun toString(): String =
        buildString {
            append("MutableBeloepPerDag[")
            append(keys.toList().sorted().joinToString { key -> "$key:${(beloep[key] ?: BigDecimal.ZERO).toPlainString()}" })
            append("]")
        }
}
