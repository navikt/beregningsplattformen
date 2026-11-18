package no.nav.utbetaling.beregning.lab.beregning

import io.ktor.http.HttpStatusCode

import no.nav.utbetaling.beregning.oppdrag.Oppdrag
import no.nav.utbetaling.beregning.oppdrag.Oppdragstidslinje

sealed interface Oppdragstidslinjeresultat {
    data class Gyldig(
        val oppdragstidslinjer: Oppdragstidslinjer,
    ) : Oppdragstidslinjeresultat

    data class Ugyldig(
        val feil: Throwable,
        val status: HttpStatusCode,
        val innsending: Int,
    ) : Oppdragstidslinjeresultat
}

data class Oppdragstidslinjer private constructor(
    val oppdragstidslinjeDTO: OppdragstidslinjeDTO = OppdragstidslinjeDTO(kjeder = emptyList()),
    val oppdragstidslinje: Oppdragstidslinje = Oppdragstidslinje(),
) {
    companion object {
        fun fra(xml: List<String>): Oppdragstidslinjeresultat =
            Oppdragstidslinjeresultat.Gyldig(
                xml.foldIndexed(Oppdragstidslinjer()) { index, sammenslåttOppdragstidslinjer, xml ->
                    val innsending = index + 1

                    val oppdatertOppdragstidslinje =
                        when (val oppdrag = Oppdrag.fraXml(xml, utgangspunkt = sammenslåttOppdragstidslinjer.oppdragstidslinje)) {
                            is Oppdrag.UgyldigKombinasjon -> return Oppdragstidslinjeresultat.Ugyldig(oppdrag.feil, HttpStatusCode.Conflict, innsending)
                            is Oppdrag.UgyldigOppdrag -> return Oppdragstidslinjeresultat.Ugyldig(oppdrag.feil, HttpStatusCode.BadRequest, innsending)
                            is Oppdrag.Utbetaling -> oppdrag.oppdragstidslinje
                            is Oppdrag.Simulering -> oppdrag.oppdragstidslinje
                        }

                    Oppdragstidslinjer(
                        oppdragstidslinjeDTO = sammenslåttOppdragstidslinjer.oppdragstidslinjeDTO + oppdatertOppdragstidslinje.toDTO(innsending),
                        oppdragstidslinje = oppdatertOppdragstidslinje,
                    )
                },
            )
    }
}
