package no.nav.utbetaling.beregning.oppdrag.intern

import no.nav.utbetaling.beregning.oppdrag.Organisasjonsnummer
import no.nav.utbetaling.beregning.oppdrag.Part
import no.nav.utbetaling.beregning.oppdrag.Personidentifikator
import no.nav.utbetaling.beregning.oppdrag.SamhandlerId

/**
 * Klassifiserer en id til en [Part] basert på formatet. Delt mellom utbetalesTil, skyldner og refunderes
 * slik at alle tre bruker samme regler.
 */
internal object Partklassifisering {
    fun fra(id: String): Part =
        when {
            id.matches(samhandlerRegex) -> Part.Samhandler(SamhandlerId(tssId = id))
            id.matches(personidentifikatorRegex) -> Part.Person(Personidentifikator(identifikator = id))
            id.matches(organisasjonsnummerRegex) -> Part.Organisasjon(Organisasjonsnummer(nummer = id))
            else -> error("Uventet id-format, kan ikke klassifiseres som person, organisasjon eller samhandler")
        }

    private val samhandlerRegex = Regex("8\\d{10}")
    private val personidentifikatorRegex = Regex("\\d{11}")
    private val organisasjonsnummerRegex = Regex("\\d{9}")
}
