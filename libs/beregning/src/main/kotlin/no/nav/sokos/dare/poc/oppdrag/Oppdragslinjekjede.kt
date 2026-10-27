package no.nav.sokos.dare.poc.oppdrag

import java.time.LocalDate
import java.time.LocalDateTime

import kotlinx.serialization.Serializable

import no.nav.sokos.dare.poc.structural.IdValueClass

data class Oppdragslinjekjede(
    val id: OppdragslinjekjedeId? = null,
    val oppdragId: OppdragId,
    val tidspktReg: LocalDateTime,
    val linjer: List<Oppdragslinje> = emptyList(),
) {
    init {
        require(
            id == null ||
                !linjer.harOverlappendePerioder(),
        ) {
            "En persistert oppdragslinjekjede kan ikke inneholde overlappende perioder"
        }
    }

    val hode: Oppdragslinje?
        get() = ordnedeLinjer().lastOrNull()

    fun ordnedeLinjer(): List<Oppdragslinje> {
        val linjeIder = linjer.mapNotNull(Oppdragslinje::id)
        require(linjeIder.distinct().size == linjeIder.size) {
            "En oppdragslinjekjede kan ikke inneholde duplikate linje-ID-er"
        }
        require(linjer.map(Oppdragslinje::delytelseId).distinct().size == linjer.size) {
            "Delytelse-ID må være unik innenfor en oppdragslinjekjede"
        }
        return linjer.sortertEtterPeriode()
    }

    companion object {
        fun fraOppdragslinjer(
            oppdragId: OppdragId,
            linjer: List<Oppdragslinje>,
        ): List<Oppdragslinjekjede> =
            linjer
                .groupBy(Oppdragslinje::oppdragslinjekjedeId)
                .flatMap { (kjedeId, kjedelinjer) ->
                    if (kjedeId == null) {
                        kjedelinjer.map { linje -> nyKjede(oppdragId, null, listOf(linje)) }
                    } else {
                        listOf(nyKjede(oppdragId, kjedeId, kjedelinjer))
                    }
                }

        private fun nyKjede(
            oppdragId: OppdragId,
            kjedeId: OppdragslinjekjedeId?,
            linjer: List<Oppdragslinje>,
        ): Oppdragslinjekjede =
            Oppdragslinjekjede(
                id = kjedeId,
                oppdragId = oppdragId,
                tidspktReg = linjer.minOf(Oppdragslinje::tidspktReg),
                linjer = linjer,
            )
    }
}

internal fun Iterable<Oppdragslinje>.sortertEtterPeriode(): List<Oppdragslinje> =
    sortedWith(
        compareBy<Oppdragslinje>(
            Oppdragslinje::datoVedtakFom,
            { it.datoVedtakTom ?: LocalDate.MAX },
            Oppdragslinje::linjeId,
            { it.id?.id ?: Long.MAX_VALUE },
        ),
    )

internal fun List<Oppdragslinje>.harOverlappendePerioder(): Boolean =
    sortertEtterPeriode()
        .zipWithNext()
        .any { (forrige, neste) -> forrige.periode.overlaps(neste.periode) }

@Serializable
@JvmInline
value class OppdragslinjekjedeId(
    override val id: Long,
) : IdValueClass
