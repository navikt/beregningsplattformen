package no.nav.utbetaling.beregning.lab.beregning

import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

import no.nav.utbetaling.beregning.lab.beregning.Feilmelding.feilResponse

internal const val API_V1 = "/api/v1"

internal fun Route.beregningApi() {
    route(API_V1) {
        post("beregn") {
            val beregningsRequest = call.receive<BeregningRequest>()

            val (oppdragstidslinjeDTO, oppdragstidslinje) =
                when (val oppdragstidslinjeresultat = Oppdragstidslinjer.fra(beregningsRequest.xml)) {
                    is Oppdragstidslinjeresultat.Ugyldig ->
                        return@post call.feilResponse(oppdragstidslinjeresultat.feil, oppdragstidslinjeresultat.status, oppdragstidslinjeresultat.innsending)
                    is Oppdragstidslinjeresultat.Gyldig -> oppdragstidslinjeresultat.oppdragstidslinjer
                }

            val beregningstidslinjeDTO =
                BeregningstidslinjeDTO.fra(
                    oppdragstidslinje = oppdragstidslinje,
                    fom = beregningsRequest.fom,
                    tom = beregningsRequest.tom,
                )

            call.respond(
                BeregningResponse(
                    oppdragstidslinje = oppdragstidslinjeDTO,
                    beregningstidslinje = beregningstidslinjeDTO,
                ),
            )
        }
    }
}
