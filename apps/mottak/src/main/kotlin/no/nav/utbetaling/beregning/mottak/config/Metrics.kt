package no.nav.utbetaling.beregning.mottak.config

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

object Metrics {
    val prometheusMeterRegistry =
        PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
}
