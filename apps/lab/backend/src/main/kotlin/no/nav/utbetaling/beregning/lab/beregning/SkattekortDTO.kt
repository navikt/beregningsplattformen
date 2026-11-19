package no.nav.utbetaling.beregning.lab.beregning

import java.math.BigDecimal

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

import no.nav.sokos.dare.poc.skattekort.domain.ForskuddstrekkKode
import no.nav.sokos.dare.poc.skattekort.domain.ForskuddstrekkType
import no.nav.sokos.dare.poc.trekk.Faggruppe

@Serializable
data class SkattekortDTO(
    val inntektsaar: Int,
    val forskuddstrekk: List<ForskuddstrekkRequest>,
)

@Serializable
data class ForskuddstrekkRequest(
    val type: ForskuddstrekkType,
    val trekkode: ForskuddstrekkKode,
    @Contextual val prosentsats: BigDecimal? = null,
    val tabellnummer: String? = null,
    val frikortbeloep: Int? = null,
    val faggruppe: Faggruppe? = null,
)
