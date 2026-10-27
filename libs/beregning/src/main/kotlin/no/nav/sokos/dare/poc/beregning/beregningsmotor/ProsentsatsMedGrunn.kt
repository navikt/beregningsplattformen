package no.nav.sokos.dare.poc.beregning.beregningsmotor

import java.math.BigDecimal

import no.nav.sokos.dare.poc.beregning.TekstEkstraInfo

internal data class ProsentsatsMedGrunn(
    val sats: BigDecimal,
    val ekstraInfo: TekstEkstraInfo? = null,
)
