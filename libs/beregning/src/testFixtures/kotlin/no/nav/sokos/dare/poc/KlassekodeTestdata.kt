package no.nav.sokos.dare.poc

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

import no.nav.sokos.dare.poc.oppdrag.YtelseKlasseKode
import no.nav.sokos.dare.poc.oppdrag.YtelseKlasseKodeRef

object KlassekodeTestdata {
    val klassekodeLookup: Map<YtelseKlasseKodeRef, YtelseKlasseKode> by lazy {
        val json = Json { ignoreUnknownKeys = true }
        val ressurs = "testsupport/integrasjon/klassekoder.json"
        val innhold =
            requireNotNull(javaClass.classLoader.getResourceAsStream(ressurs)) {
                "Ressursen $ressurs finnes ikke"
            }.bufferedReader().use { it.readText() }
        json
            .decodeFromString(ListSerializer(YtelseKlasseKodeSerializer), innhold)
            .associateBy { it.kode }
    }
}
