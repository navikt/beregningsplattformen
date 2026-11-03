package no.nav.sokos.dare.poc.beregning.fordeler

import kotlinx.serialization.Serializable

import no.nav.sokos.dare.poc.skattekort.domain.ForskuddstrekkKode
import no.nav.sokos.dare.poc.skattekort.domain.Skattekort
import no.nav.sokos.dare.poc.trekk.Faggruppe

@Serializable
data class SkatteKontekst(
    val skattekort: Skattekort?,
    val allokeringer: Map<ForskuddstrekkKode, Faggruppe>,
)
