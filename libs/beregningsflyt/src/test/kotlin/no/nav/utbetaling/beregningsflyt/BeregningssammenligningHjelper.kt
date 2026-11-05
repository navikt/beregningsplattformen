package no.nav.utbetaling.beregningsflyt

import kotlinx.serialization.json.Json

import org.opentest4j.AssertionFailedError

import no.nav.sokos.dare.poc.beregning.dto.BeregningsmotorKontekstPresentasjon

internal val prettyPrintJson =
    Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        prettyPrint = true
        prettyPrintIndent = "  "
    }

internal fun sammenliknBeregning(
    scenario: Scenario,
    resultat: BeregningsmotorKontekstPresentasjon,
) {
    val forventet = prettyPrintJson.decodeFromString<BeregningsmotorKontekstPresentasjon>(scenario.resultat)
    try {
        BeregningsmotorKontekstKomparator.sammenlign(
            faktisk = resultat,
            forventet = forventet,
        )
    } catch (e: Throwable) {
        val diffFeil =
            AssertionFailedError(
                "Sammenlikning feilet: ${scenario.navn}",
                prettyPrintJson.encodeToString(forventet),
                prettyPrintJson.encodeToString(resultat),
            )
        diffFeil.addSuppressed(e)
        throw diffFeil
    }
}
