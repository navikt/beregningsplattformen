package no.nav.utbetaling.beregning.lab

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import no.nav.utbetaling.beregning.lab.api.labApi
import no.nav.utbetaling.beregning.lab.config.commonConfig
import no.nav.utbetaling.beregning.lab.config.routingConfig

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::testModule).start(true)
}

private fun Application.testModule() {
    commonConfig()
    routing {
        labApi()
    }
}

