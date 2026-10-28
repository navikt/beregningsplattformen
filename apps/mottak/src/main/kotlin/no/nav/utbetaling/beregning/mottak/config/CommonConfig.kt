package no.nav.utbetaling.beregning.mottak.config

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.binder.system.UptimeMetrics

fun Application.commonConfig() {
    install(MicrometerMetrics) {
        registry = Metrics.prometheusMeterRegistry
        meterBinders =
            listOf(
                UptimeMetrics(),
                JvmMemoryMetrics(),
                JvmGcMetrics(),
                JvmThreadMetrics(),
                ProcessorMetrics(),
            )
    }
}

fun Routing.internalNaisRoutes(
    applicationState: ApplicationState,
    readynessCheck: () -> Boolean = { applicationState.ready },
    alivenessCheck: () -> Boolean = { applicationState.alive },
) {
    route("internal") {
        get("isAlive") {
            if (alivenessCheck()) {
                call.respondText("OK")
            } else {
                call.respondText(
                    text = "Not alive",
                    status = HttpStatusCode.InternalServerError,
                )
            }
        }
        get("isReady") {
            if (readynessCheck()) {
                call.respondText("OK")
            } else {
                call.respondText(
                    text = "Not ready",
                    status = HttpStatusCode.InternalServerError,
                )
            }
        }
        get("metrics") {
            call.respondText(Metrics.prometheusMeterRegistry.scrape())
        }
    }
}
