package no.nav.utbetaling.beregning.http

import kotlinx.serialization.json.Json

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import no.nav.utbetaling.beregning.log.logger
import no.nav.utbetaling.beregning.log.secureLog

private val httpLog = logger("http")

object HttpClientFactory {
    fun new(
        json: Json,
        logLevel: LogLevel = LogLevel.INFO,
        retries: Int? = 3,
        requestTimeoutMs: Long? = 60_000,
        connectionTimeoutMs: Long? = 30_000,
    ): HttpClient {
        require(logLevel == LogLevel.INFO || logLevel == LogLevel.NONE) {
            "Only INFO and NONE are supported to prevent sensitive HTTP data from being logged."
        }

        return HttpClient(CIO) {
            install(Logging) {
                logger = ClientLogger(logLevel)
                level = logLevel
            }

            install(ContentNegotiation) {
                json(json)
            }

            retries?.let {
                install(HttpRequestRetry) {
                    retryOnServerErrors(retries)
                    retryOnException(maxRetries = retries, retryOnTimeout = true)
                    exponentialDelay()
                }
            }

            requestTimeoutMs?.let {
                install(HttpTimeout) {
                    requestTimeoutMillis = requestTimeoutMs
                    connectTimeoutMillis = connectionTimeoutMs
                }
            }
        }
    }
}

class ClientLogger(
    level: LogLevel,
) : Logger {
    override fun log(message: String) {
        log.info(message)
    }

    private val log =
        when (level) {

            /**
             * HTTP code, method and url is logged
             */
            LogLevel.INFO, LogLevel.NONE -> httpLog

            /**
             *  HTTP code, method, url, headers request body and response body is logged
             */
            else -> secureLog
        }
}
