package no.nav.sokos.dare.poc.aktoer

import kotlinx.serialization.Serializable

import no.nav.sokos.dare.poc.structural.IdValueClass

@Serializable
@JvmInline
value class AktoerId(
    override val id: Long,
) : IdValueClass
