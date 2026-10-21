package no.nav.utbetaling.beregning.lab.api

import kotlin.time.Clock

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.path
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

import no.nav.utbetaling.beregning.lab.config.ApiError
import no.nav.utbetaling.beregning.lab.dto.toDTO
import no.nav.utbetaling.beregning.oppdrag.Oppdrag

const val API_V1 = "/api/v1"

fun Route.labApi() {
    route(API_V1) {
        post("tolk-oppdrag") {
            val requestBody = call.receive<BeregningRequest>()
            when (
                val oppdrag = Oppdrag.fraXml(*requestBody.xml.toTypedArray())
            ) {
                is Oppdrag.UgyldigOppdrag -> {
                    val status = HttpStatusCode.BadRequest
                    call.respond(
                        status,
                        ApiError(
                            timestamp = Clock.System.now(),
                            status = status.value,
                            error = status.description,
                            message = oppdrag.feil.message,
                            path = call.request.path(),
                        ),
                    )
                }

                is Oppdrag.UgyldigKombinasjon -> {
                    val status = HttpStatusCode.Conflict
                    call.respond(
                        status,
                        ApiError(
                            timestamp = Clock.System.now(),
                            status = status.value,
                            error = status.description,
                            message = oppdrag.feil.message,
                            path = call.request.path(),
                        ),
                    )
                }

                is Oppdrag.Simulering -> call.respond(
                    oppdrag.oppdragstidslinje.toDTO()
                )

                is Oppdrag.Utbetaling -> call.respond(
                    oppdrag.oppdragstidslinje.toDTO(),
                )
            }
        }
    }
}
