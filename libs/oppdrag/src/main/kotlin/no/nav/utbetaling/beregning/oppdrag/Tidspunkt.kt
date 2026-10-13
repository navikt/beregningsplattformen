package no.nav.utbetaling.beregning.oppdrag

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal object Tidspunkt {
    private val OSLO = ZoneId.of("Europe/Oslo")
    private val TIDSPUNKTFORMATER =
        listOf(
            DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss[.SSSSSS][.SSS]"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
        )

    fun fra(verdi: String) =
        TIDSPUNKTFORMATER
            .firstNotNullOfOrNull { runCatching { LocalDateTime.parse(verdi.trim(), it) }.getOrNull() }
            .let { checkNotNull(it) { "Skjønner ikke tidspunktet $verdi" } }
            .atZone(OSLO)
            .toInstant()
}
