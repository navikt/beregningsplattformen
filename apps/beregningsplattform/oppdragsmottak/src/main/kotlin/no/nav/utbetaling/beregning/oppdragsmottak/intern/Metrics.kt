package no.nav.utbetaling.beregning.oppdragsmottak.metrics

import io.micrometer.core.instrument.Tag
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

import no.nav.utbetaling.beregning.oppdrag.Oppdrag

internal const val MOTTAK_METRICS_NAMESPACE = "beregningsplattformen_beregningsplattform_mottak"

object Metrics {
    val prometheusMeterRegistryMottak =
        PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    fun registrerOppdrag(oppdrag: Oppdrag) {
        val typeTag =
            "oppdragstype" to
                when (oppdrag) {
                    is Oppdrag.Simulering -> "simulering"
                    is Oppdrag.UgyldigKombinasjon -> "ugyldig_kombinasjon"
                    is Oppdrag.UgyldigOppdrag -> "ugyldig_oppdrag"
                    is Oppdrag.Utbetaling -> "utbetaling"
                }

        // Hvert felt er sin egen counter
        oppdrag.metrikker.filter { it.navn == "innsendte_felt" }.flatMap { it.forekomster }.forEach { forekomst ->
            val tagsUtenFelt = forekomst.tags.minus("felt").filter { (_, verdi) -> verdi.isNotBlank() }
            val felt = forekomst.tags.getValue("felt")
            val tags = (tagsUtenFelt + typeTag).map { (navn, verdi) -> Tag.of(navn, verdi) }

            prometheusMeterRegistryMottak.counter("${MOTTAK_METRICS_NAMESPACE}_$felt", tags).increment()
        }

        // En counter per metrikk, feltene i innsendte_felt er en tag istedenfor en egen counter
        oppdrag.metrikker.forEach { metrikk ->
            metrikk.forekomster.forEach { forekomst ->
                val tags = (forekomst.tags + typeTag).map { (navn, verdi) -> Tag.of(navn, verdi) }
                prometheusMeterRegistryMottak.counter(metrikk.navn, tags).increment()
            }
        }
    }
}
