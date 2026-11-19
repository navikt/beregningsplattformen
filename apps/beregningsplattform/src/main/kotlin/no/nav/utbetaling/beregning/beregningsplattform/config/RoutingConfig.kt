package no.nav.utbetaling.beregning.beregningsplattform.config

import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.routing

import no.nav.utbetaling.beregning.beregningsplattform.AUTHENTICATION_NAME
import no.nav.utbetaling.beregning.beregningsplattform.ApplicationState
import no.nav.utbetaling.beregning.beregningsplattform.internalNaisRoutes
import no.nav.utbetaling.beregning.skattekort.skattekortApi

fun Application.routingConfig(applicationState: ApplicationState) {
    routing {
        internalNaisRoutes(applicationState)
        authenticate(AUTHENTICATION_NAME) {
            skattekortApi()
        }
    }
}
