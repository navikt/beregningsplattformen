package no.nav.utbetaling.beregning.lab

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

import no.nav.utbetaling.beregning.lab.config.ApplicationState
import no.nav.utbetaling.beregning.lab.config.PropertiesConfig
import no.nav.utbetaling.beregning.lab.config.applicationLifecycleConfig
import no.nav.utbetaling.beregning.lab.config.commonConfig
import no.nav.utbetaling.beregning.lab.config.loadEnv
import no.nav.utbetaling.beregning.lab.config.routingConfig
import no.nav.utbetaling.beregning.lab.config.securityConfig
import no.nav.utbetaling.beregning.log.Log

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module).start(true)
}

private fun Application.module() {
    PropertiesConfig.load(environment.config.loadEnv())
    val applicationState = ApplicationState()

    applicationLifecycleConfig(applicationState)
    commonConfig()
    securityConfig(PropertiesConfig.azureAdProperties)
    routingConfig(applicationState)

    Log.info("Application started with profile: ${PropertiesConfig.applicationProperties.profile}")
}
