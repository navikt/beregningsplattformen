package no.nav.utbetaling.beregning.oppdrag

import no.nav.utbetaling.beregning.oppdrag.intern.FraXmlFormat
import no.nav.utbetaling.beregning.oppdrag.xml.Oppdrag110

sealed interface Oppdrag {
    data class Utbetaling(
        val oppdragstidslinje: Oppdragstidslinje,
    ) : Oppdrag

    data class Simulering(
        val oppdragstidslinje: Oppdragstidslinje,
    ) : Oppdrag

    data class UgyldigOppdrag(
        val feil: Throwable,
    ) : Oppdrag

    data class UgyldigKombinasjon(
        val feil: Throwable,
    ) : Oppdrag

    companion object {
        fun fraXml(vararg xml: String) =
            lagOppdrag {
                FraXmlFormat.oppdrag110FraXml(*xml)
            }

        fun fraJson(vararg json: String) =
            lagOppdrag {
                FraXmlFormat.oppdrag110FraJson(*json)
            }

        private fun lagOppdrag(oppdrag110: () -> List<Oppdrag110>): Oppdrag {
            val oppdrag110 =
                runCatching { oppdrag110() }.getOrElse { feil ->
                    return UgyldigOppdrag(feil)
                }

            val aksjonskoder = oppdrag110.map { it.kodeAksjon.trim() }.toSet()

            when {
                aksjonskoder.isEmpty() -> return UgyldigOppdrag(IllegalArgumentException("Det er ikke sendt inn noen oppdrag!"))
                aksjonskoder.size > 1 -> return UgyldigKombinasjon(IllegalArgumentException("Det er sendt inn aksjonskoder på tvers av utbetaling og simulering!"))
            }

            val oppdragstidslinje =
                runCatching { FraXmlFormat.fraOppdrag110(oppdrag110) }.getOrElse { feil ->
                    return UgyldigOppdrag(feil)
                }

            return when (aksjonskoder.single()) {
                SIMULERING -> Simulering(oppdragstidslinje)
                else -> Utbetaling(oppdragstidslinje)
            }
        }

        private const val SIMULERING = "3"
    }
}
