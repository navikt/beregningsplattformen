package no.nav.utbetaling.beregningsflyt.identitet

import java.util.UUID

import no.nav.sokos.dare.poc.aktoer.AktoerId
import no.nav.sokos.dare.poc.oppdrag.OppdragId
import no.nav.sokos.dare.poc.oppdrag.OppdragslinjekjedeId
import no.nav.sokos.dare.poc.oppdrag.YtelseKlasseKode
import no.nav.utbetaling.beregning.oppdrag.Fagområde
import no.nav.utbetaling.beregning.oppdrag.FagsystemId
import no.nav.utbetaling.beregning.oppdrag.Personidentifikator
import no.nav.utbetaling.beregning.oppdrag.UtbetalesTil
import no.nav.utbetaling.beregning.oppdrag.YtelseKlassekode

data class Oppdragsnokkel(
    val fagomrade: Fagområde,
    val fagsystemId: FagsystemId,
)

/** Unknown lookups throw [NoSuchElementException]; source identifiers must never be interpreted as numeric actor IDs. */
interface Beregningsidentitet {
    fun oppdragId(nokkel: Oppdragsnokkel): OppdragId

    fun gjelderId(personidentifikator: Personidentifikator): AktoerId

    fun mottakerId(utbetalesTil: UtbetalesTil): AktoerId

    /** Resolves a source chain only when emitting result rows, not when constructing timeline views. */
    fun resultatKjedeId(kjedeId: UUID): OppdragslinjekjedeId
}

/** Implementations throw [NoSuchElementException] when the source class code is unknown. */
fun interface Klassekodeoppslag {
    fun finn(klassekode: YtelseKlassekode): YtelseKlasseKode
}
