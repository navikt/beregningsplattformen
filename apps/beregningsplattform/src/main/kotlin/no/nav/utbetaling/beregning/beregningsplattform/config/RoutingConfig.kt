package no.nav.utbetaling.beregning.beregningsplattform.config

import javax.sql.DataSource

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.routing

import no.nav.utbetaling.beregning.jdbc.isHealthy
import no.nav.utbetaling.beregning.skattekort.skattekortApi

fun Application.routingConfig(
    applicationState: ApplicationState,
    dataSource: DataSource,
) {
    routing {
        internalNaisRoutes(
            applicationState,
            readynessCheck = { applicationState.ready && withContext(Dispatchers.IO) { dataSource.isHealthy() } },
        )
        authenticate(AUTHENTICATION_NAME) {
            skattekortApi()
        }
    }
}
