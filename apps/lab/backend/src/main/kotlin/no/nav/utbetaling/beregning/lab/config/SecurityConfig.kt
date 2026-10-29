package no.nav.utbetaling.beregning.lab.config

import java.net.URI
import java.util.concurrent.TimeUnit

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.server.application.Application
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt

import no.nav.utbetaling.beregning.http.HttpClientFactory
import no.nav.utbetaling.beregning.kotlinx.KotlinxJson
import no.nav.utbetaling.beregning.log.Log
import no.nav.utbetaling.beregning.log.httpLog

const val AUTHENTICATION_NAME = "azureAd"

fun Application.securityConfig(azureAdProperties: PropertiesConfig.AzureAdProperties) {
    val openIdMetadata: OpenIdMetadata = wellKnowConfig(azureAdProperties.wellKnownUrl)
    val jwkProvider = cachedJwkProvider(openIdMetadata.jwksUri)

    authentication {
        jwt(AUTHENTICATION_NAME) {
            realm = PropertiesConfig.applicationProperties.appName
            verifier(
                jwkProvider = jwkProvider,
                issuer = openIdMetadata.issuer,
            ) { acceptLeeway(1) }
            validate { credential ->
                try {
                    requireNotNull(credential.payload.audience) {
                        Log.info("Auth: Missing audience in token")
                        "Auth: Missing audience in token"
                    }
                    require(credential.payload.audience.contains(azureAdProperties.clientId)) {
                        Log.info("Auth: Valid audience not found in claims")
                        "Auth: Valid audience not found in claims"
                    }
                    JWTPrincipal(credential.payload)
                } catch (e: Exception) {
                    Log.warn("Client authentication failed", e)
                    null
                }
            }
        }
    }
}

private fun cachedJwkProvider(jwksUri: String): JwkProvider =
    JwkProviderBuilder(URI(jwksUri).toURL())
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()

@Serializable
data class OpenIdMetadata(
    @SerialName("jwks_uri") val jwksUri: String,
    @SerialName("issuer") val issuer: String,
    @SerialName("token_endpoint") val tokenEndpoint: String,
)

private fun wellKnowConfig(wellKnownUrl: String): OpenIdMetadata {
    val openIdMetadata: OpenIdMetadata by lazy {
        runBlocking {
            HttpClientFactory.new(KotlinxJson, logger = httpLog).use { client ->
                client.get(wellKnownUrl).body<OpenIdMetadata>()
            }
        }
    }
    return openIdMetadata
}
