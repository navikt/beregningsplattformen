package no.nav.utbetaling.beregning.mottak.config

import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry

object Metrics {
    val prometheusMeterRegistry =
        PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
}
