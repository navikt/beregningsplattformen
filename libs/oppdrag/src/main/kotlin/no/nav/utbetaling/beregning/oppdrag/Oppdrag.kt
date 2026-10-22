package no.nav.utbetaling.beregning.oppdrag

import tools.jackson.databind.JsonNode

import no.nav.utbetaling.beregning.oppdrag.intern.FraXmlFormat
import no.nav.utbetaling.beregning.oppdrag.intern.Metrikker
import no.nav.utbetaling.beregning.oppdrag.xml.Oppdrag110

sealed interface Oppdrag {
    val metrikker: List<Metrikk>

    data class Utbetaling(
        val oppdragstidslinje: Oppdragstidslinje,
        override val metrikker: List<Metrikk>,
    ) : Oppdrag

    data class Simulering(
        val oppdragstidslinje: Oppdragstidslinje,
        override val metrikker: List<Metrikk>,
    ) : Oppdrag

    data class UgyldigOppdrag(
        val feil: Throwable,
        override val metrikker: List<Metrikk>,
    ) : Oppdrag

    data class UgyldigKombinasjon(
        val feil: Throwable,
        override val metrikker: List<Metrikk>,
    ) : Oppdrag

    data class Metrikk(
        val felt: String,
        val tags: Map<String, String>,
    )

    companion object {
        fun fraXml(
            vararg xml: String,
            utgangspunkt: Oppdragstidslinje = Oppdragstidslinje(),
        ) = lagOppdrag(utgangspunkt) {
            FraXmlFormat.oppdrag110FraXml(*xml)
        }

        fun fraJson(
            vararg json: String,
            utgangspunkt: Oppdragstidslinje = Oppdragstidslinje(),
        ) = lagOppdrag(utgangspunkt) {
            FraXmlFormat.oppdrag110FraJson(*json)
        }

        private fun lagOppdrag(
            utgangspunkt: Oppdragstidslinje,
            oppdrag110: () -> Map<JsonNode, Oppdrag110>,
        ): Oppdrag {
            val oppdrag110 =
                runCatching { oppdrag110() }.getOrElse { feil ->
                    return UgyldigOppdrag(feil, emptyList())
                }
            val metrikker = Metrikker.fra(oppdrag110.keys)

            val aksjonskoder = oppdrag110.values.map { it.kodeAksjon.trim() }.toSet()

            when {
                aksjonskoder.isEmpty() -> return UgyldigOppdrag(IllegalArgumentException("Det er ikke sendt inn noen oppdrag!"), metrikker)
                aksjonskoder.size > 1 -> return UgyldigKombinasjon(IllegalArgumentException("Det er sendt inn aksjonskoder på tvers av utbetaling og simulering!"), metrikker)
            }

            val oppdragstidslinje =
                runCatching { FraXmlFormat.fraOppdrag110(oppdrag110.values, utgangspunkt) }.getOrElse { feil ->
                    return UgyldigOppdrag(feil, metrikker)
                }

            return when (aksjonskoder.single()) {
                SIMULERING -> Simulering(oppdragstidslinje, metrikker)
                else -> Utbetaling(oppdragstidslinje, metrikker)
            }
        }

        private const val SIMULERING = "3"
    }
}
