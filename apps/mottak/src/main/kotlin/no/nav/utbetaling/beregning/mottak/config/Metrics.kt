package no.nav.utbetaling.beregning.mottak.config

import io.micrometer.core.instrument.Tag
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

import no.nav.utbetaling.beregning.oppdrag.Oppdrag

private const val NAMESPACE = "beregningsplattform_mottak"

object Metrics {
    val prometheusMeterRegistry =
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

        oppdrag.metrikker.forEach { metrikk ->
            val tags = (metrikk.tags + typeTag).map { (navn, verdi) -> Tag.of(navn, verdi) }

            prometheusMeterRegistry.counter("${NAMESPACE}_${metrikk.felt}", tags).increment()
        }
    }
}
