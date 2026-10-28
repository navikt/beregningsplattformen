package no.nav.beregning.taskv2

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

private val log = KotlinLogging.logger {}

/**
 * 🔴 RØD SONE — leder-valg via Nais leader-elector-sidecar.
 *
 * Sidecaren eksponerer leder-podens navn på $ELECTOR_PATH (f.eks. "elector:4040/"). Vi er leder når
 * svaret matcher vårt eget $HOSTNAME.
 *
 * Fail-safe: enhver feil (sidecar nede, timeout, parsefeil) returnerer false. Da hopper vi over
 * vedlikehold heller enn å risikere at to podder kjører det samtidig.
 */
class LeaderElection(
    private val electorPath: String? = System.getenv("ELECTOR_PATH"),
    private val hostname: String? = System.getenv("HOSTNAME"),
    private val client: HttpClient = HttpClient.newHttpClient(),
) {
    private val mapper = ObjectMapper()

    fun erLeder(): Boolean {
        if (electorPath.isNullOrBlank() || hostname.isNullOrBlank()) {
            log.warn { "ELECTOR_PATH eller HOSTNAME mangler, antar ikke-leder" }
            return false
        }
        return try {
            val url = if (electorPath.startsWith("http")) electorPath else "http://$electorPath"
            val request = HttpRequest.newBuilder(URI.create(url)).GET().build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            val leder = mapper.readTree(response.body()).get("name")?.asText()
            leder == hostname
        } catch (e: Exception) {
            log.warn(e) { "Klarte ikke avgjøre leder, antar ikke-leder" }
            false
        }
    }
}
