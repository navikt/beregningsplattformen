package no.nav.utbetaling.beregning.lab.beregning

import java.util.Collections
import java.util.IdentityHashMap

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond

import no.nav.utbetaling.beregning.lab.config.createApiError

internal object Feilmelding {
    private fun fra(throwable: Throwable): String {
        val linjer = mutableListOf<String>()
        val sette = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())

        var gjeldende: Throwable? = throwable
        var nivå = 0
        while (gjeldende != null && sette.add(gjeldende)) {
            gjeldende.message?.takeIf { it.isNotBlank() }?.let { melding ->
                linjer.add("\t".repeat(nivå) + melding)
                nivå++
            }
            gjeldende = gjeldende.cause
        }

        return linjer.joinToString("\n")
    }

    suspend fun ApplicationCall.feilResponse(
        throwable: Throwable,
        status: HttpStatusCode,
        innsending: Int,
    ) = respond(
        status = status,
        message =
            createApiError(
                status = status,
                message = "Feil på innsending $innsending: ${fra(throwable)}",
                call = this,
            ),
    )
}
