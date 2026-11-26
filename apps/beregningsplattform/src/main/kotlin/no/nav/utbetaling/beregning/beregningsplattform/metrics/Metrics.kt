package no.nav.utbetaling.beregning.beregningsplattform.metrics

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

private const val NAMESPACE = "beregningsplattformen_beregningsplattform"

object Metrics {
    val prometheusMeterRegistry =
        PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
}
