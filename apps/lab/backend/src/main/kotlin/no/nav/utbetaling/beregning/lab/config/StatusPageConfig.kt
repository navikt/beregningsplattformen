package no.nav.utbetaling.beregning.lab.config

import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.serialization.Serializable

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.requestvalidation.RequestValidationException
import io.ktor.server.plugins.statuspages.StatusPagesConfig
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.response.respond

import no.nav.utbetaling.beregning.log.Log

fun StatusPagesConfig.statusPageConfig() {
    exception<RequestValidationException> { call, _ ->
        call.logOgResponder(HttpStatusCode.BadRequest, "Ugyldig forespørsel.")
    }

    exception<BadRequestException> { call, _ ->
        call.logOgResponder(HttpStatusCode.BadRequest, "Ugyldig forespørsel.")
    }

    exception<Throwable> { call, _ ->
        call.logOgResponder(
            HttpStatusCode.InternalServerError,
            "En teknisk feil har oppstått. Ta kontakt med utviklerne",
        )
    }
}

private suspend fun ApplicationCall.logOgResponder(
    status: HttpStatusCode,
    message: String?,
) {
    logOgResponder(status, createApiError(status, message, this))
}

private suspend fun ApplicationCall.logOgResponder(
    status: HttpStatusCode,
    apiError: ApiError,
) {
    if (status.value >= 500) {
        Log.error(
            "HTTP-forespørsel feilet: status=${status.value}, metode=${request.httpMethod.value}, path=${request.path()}",
        )
    } else {
        Log.warn(
            "HTTP-forespørsel avvist: status=${status.value}, metode=${request.httpMethod.value}, path=${request.path()}",
        )
    }
    respond(status, apiError)
}

private fun createApiError(
    status: HttpStatusCode,
    message: String?,
    call: ApplicationCall,
): ApiError =
    ApiError(
        Clock.System.now(),
        status.value,
        status.description,
        message,
        call.request.path(),
    )

@Serializable
data class ApiError(
    val timestamp: Instant,
    val status: Int,
    val error: String,
    val message: String?,
    val path: String,
)
