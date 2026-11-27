package no.nav.utbetaling.beregning.oppdragsmottak.metrics

import io.micrometer.core.instrument.Tag
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

import no.nav.utbetaling.beregning.oppdrag.Oppdrag

const val MOTTAK_METRICS_NAMESPACE = "beregningsplattformen_beregningsplattform_mottak"

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

        oppdrag.metrikker.flatMap { it.forekomster }.forEach { forekomst ->
            val tagsUtenFelt = forekomst.tags.minus("felt")
            val felt = forekomst.tags.getValue("felt")
            val tags = (tagsUtenFelt + typeTag).map { (navn, verdi) -> Tag.of(navn, verdi) }

            prometheusMeterRegistryMottak.counter("${MOTTAK_METRICS_NAMESPACE}_$felt", tags).increment()
        }

        oppdrag.metrikker.forEach { metrikk ->
            metrikk.forekomster.forEach { forekomst ->
                val tags = (forekomst.tags + typeTag).map { (navn, verdi) -> Tag.of(navn, verdi) }
                prometheusMeterRegistryMottak.counter(metrikk.navn, tags).increment()
            }
        }
    }
}
