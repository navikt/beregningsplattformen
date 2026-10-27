package no.nav.sokos.dare.poc.beregning.dataklasser

import kotlinx.serialization.Serializable

import no.nav.sokos.dare.poc.beregning.Beregning
import no.nav.sokos.dare.poc.beregning.fordeler.BeregningsSegment

@Serializable
enum class BeregningsStatusflagg {
    MANUELL,
    DATOSTYRT,
    MANGLER_SKATTEKORT,
}

fun List<Beregning>.harStatusflagg(status: BeregningsStatusflagg): Boolean = any { it.statusflagg.contains(status) }

fun BeregningsSegment.harStatusflagg(status: BeregningsStatusflagg): Boolean = beregningsStatusflagg.contains(status)
