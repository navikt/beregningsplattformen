package no.nav.utbetaling.beregning.oppdrag.intern

import no.nav.utbetaling.beregning.oppdrag.Organisasjonsnummer
import no.nav.utbetaling.beregning.oppdrag.Personidentifikator
import no.nav.utbetaling.beregning.oppdrag.SamhandlerId

internal fun String.samhandlerId() = SamhandlerId(this).takeIf { this.matches(samhandlerRegex) }

internal fun String.personidentifikator() = Personidentifikator(this).takeIf { this.matches(personidentifikatorRegex) }

internal fun String.organisasjonsnummer() = Organisasjonsnummer(this).takeIf { this.matches(organisasjonsnummerRegex) }

private val samhandlerRegex = Regex("8\\d{10}")
private val personidentifikatorRegex = Regex("\\d{11}")
private val organisasjonsnummerRegex = Regex("\\d{9}")
