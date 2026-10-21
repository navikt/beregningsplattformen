package no.nav.utbetaling.beregning.lab.config

import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.routing

import no.nav.utbetaling.beregning.lab.api.labApi

fun Application.routingConfig(applicationState: ApplicationState) {
    routing {
        internalNaisRoutes(applicationState)
        authenticate(AUTHENTICATION_NAME) {
            labApi()
        }
    }
}
