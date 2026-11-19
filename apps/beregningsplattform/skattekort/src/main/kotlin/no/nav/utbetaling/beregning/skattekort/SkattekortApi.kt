package no.nav.utbetaling.beregning.skattekort

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

private const val DUMMY_RESPONSE = """{"status":"dummy"}"""

fun Route.skattekortApi() {
    get("/api/skattekort") {
        call.respondText(DUMMY_RESPONSE, ContentType.Application.Json, HttpStatusCode.OK)
    }
}
