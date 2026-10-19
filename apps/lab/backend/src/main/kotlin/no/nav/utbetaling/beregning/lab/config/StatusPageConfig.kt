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

import no.nav.utbetaling.beregning.log.secureLog

fun StatusPagesConfig.statusPageConfig() {
    exception<RequestValidationException> { call, cause ->
        call.logOgResponder(HttpStatusCode.BadRequest, cause.reasons.toString(), cause)
    }

    exception<BadRequestException> { call, cause ->
        call.logOgResponder(HttpStatusCode.BadRequest, cause.message, cause)
    }

    exception<Throwable> { call, cause ->
        call.logOgResponder(
            HttpStatusCode.InternalServerError,
            cause.message ?: "En teknisk feil har oppstått. Ta kontakt med utviklerne",
            cause,
        )
    }
}

private suspend fun ApplicationCall.logOgResponder(
    status: HttpStatusCode,
    message: String?,
    cause: Throwable,
) {
    logOgResponder(status, createApiError(status, message, this), cause)
}

private suspend fun ApplicationCall.logOgResponder(
    status: HttpStatusCode,
    apiError: ApiError,
    cause: Throwable,
) {
    if (status.value >= 500) {
        secureLog.error(
            "HTTP-forespørsel feilet: status=${status.value}, metode=${request.httpMethod.value}, path=${request.path()}",
            cause,
        )
    } else {
        secureLog.warn(
            "HTTP-forespørsel avvist: status=${status.value}, metode=${request.httpMethod.value}, path=${request.path()}",
            cause,
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
