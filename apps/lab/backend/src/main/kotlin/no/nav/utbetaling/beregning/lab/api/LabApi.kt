package no.nav.utbetaling.beregning.lab.api

import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

import no.nav.utbetaling.beregning.lab.service.OppdragTolkningService
import no.nav.utbetaling.beregning.oppdrag.Oppdrag

const val API_V1 = "/api/v1"

fun Route.labApi(oppdragTolkningService: OppdragTolkningService) {
    route(API_V1) {
        post("tolk-oppdrag") {
            val oppdrag = Oppdrag.fraXml(call.receiveText())
            val oppdragstidslinje = oppdragTolkningService.tolkOppdrag(oppdrag)
            call.respondText { oppdragstidslinje.toString() }
        }
    }
}
