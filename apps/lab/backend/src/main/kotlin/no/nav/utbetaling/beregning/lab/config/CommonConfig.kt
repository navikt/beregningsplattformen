package no.nav.utbetaling.beregning.lab.config

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.path
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.binder.system.UptimeMetrics
import org.slf4j.event.Level

import no.nav.utbetaling.beregning.kotlinx.KotlinxJson
import no.nav.utbetaling.beregning.lab.metrics.Metrics
import no.nav.utbetaling.beregning.log.appLog

fun Application.commonConfig() {
    install(CallLogging) {
        logger = appLog
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/api") }
        disableDefaultColors()
    }
    install(ContentNegotiation) {
        json(KotlinxJson)
    }
    install(StatusPages) {
        statusPageConfig()
    }
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
