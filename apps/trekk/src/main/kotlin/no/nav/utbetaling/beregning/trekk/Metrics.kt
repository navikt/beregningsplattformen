package no.nav.utbetaling.beregning.trekk

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.prometheus.metrics.core.metrics.Counter

private const val NAMESPACE = "beregningsplattform_trekk"

object Metrics {
    val prometheusMeterRegistry =
        PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    val trekkOkCounter: Counter =
        Counter
            .builder()
            .name("${NAMESPACE}_trekk_mq_listener")
            .help("Antall mottatte trekk fra OS Z")
            .withoutExemplars()
            .register(prometheusMeterRegistry.prometheusRegistry)

    val trekkFeiletCounter: Counter =
        Counter
            .builder()
            .name("${NAMESPACE}_trekk_mq_listener_boq")
            .help("Antall mottatte trekk fra OS Z som feiler og sendes til BOQ")
            .withoutExemplars()
            .register(prometheusMeterRegistry.prometheusRegistry)
}
