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
import no.nav.utbetaling.beregning.lab.dto.Oppdragstidslinjer
import no.nav.utbetaling.beregning.lab.dto.toDTO
import no.nav.utbetaling.beregning.oppdrag.Oppdrag
import no.nav.utbetaling.beregning.oppdrag.Oppdragstidslinje

const val API_V1 = "/api/v1"

fun Route.labApi() {
    route(API_V1) {
        post("tolk-oppdrag") {
            val requestBody = call.receive<BeregningRequest>()
            val oppdrag2 = requestBody.xml.foldIndexed(Oppdragstidslinjer()) { innsending, sammenslåttOppdragstidslinjer, xml ->
                val oppdaterOppdragtidslinje = (Oppdrag.fraXml(xml, utgangspunkt = sammenslåttOppdragstidslinjer.oppdragstidslinje) as Oppdrag.Utbetaling).oppdragstidslinje
                val oppdatertDto = sammenslåttOppdragstidslinjer.oppdragstidslinjeDTO + oppdaterOppdragtidslinje.toDTO(innsending + 1)
                Oppdragstidslinjer(
                    oppdragstidslinjeDTO = oppdatertDto,
                    oppdragstidslinje = oppdaterOppdragtidslinje,
                )
            }
            call.respond(oppdrag2.oppdragstidslinjeDTO)
 /*           when (
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
            }*/
        }
    }

    fun ja() {
        println("ja")
    }
}
