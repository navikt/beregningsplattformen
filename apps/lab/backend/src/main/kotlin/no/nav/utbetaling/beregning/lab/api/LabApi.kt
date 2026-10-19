package no.nav.utbetaling.beregning.lab.api

import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

import no.nav.utbetaling.beregning.lab.service.OppdragTolkningService
import no.nav.utbetaling.beregning.oppdrag.Oppdrag

const val LAB_API_PATH = "api/lab/v1"

fun Route.labApi(oppdragTolkningService: OppdragTolkningService) {
    route(LAB_API_PATH) {
        post("tolkOppdrag") {
            val oppdrag = Oppdrag.fraXml(call.receiveText())
            val oppdragstidslinje = oppdragTolkningService.tolkOppdrag(oppdrag)
            call.respond(oppdragstidslinje)
        }
    }
}
