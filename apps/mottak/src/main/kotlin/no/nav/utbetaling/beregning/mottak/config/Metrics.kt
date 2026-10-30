package no.nav.utbetaling.beregning.mottak.config

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.prometheus.metrics.core.metrics.Counter

import no.nav.utbetaling.beregning.oppdrag.Oppdrag

private const val METRICS_NAMESPACE = "beregningsplattform_mottak"

object Metrics {
    private val feltverdier =
        listOf(
            "oppdrag_110_kodeAksjon",
            "oppdrag_110_kodeEndring",
            "oppdrag_110_fagsystemId",
            "oppdrag_110_oppdrags_linje_150_kodeEndringLinje",
            "oppdrag_110_oppdrags_linje_150_delytelseId",
            "oppdrag_110_oppdrags_linje_150_refFagsystemId",
        )

    val prometheusMeterRegistry =
        PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    val oppdragFeltCounter: Counter =
        Counter
            .builder()
            .name("${METRICS_NAMESPACE}_oppdrag_felt_total")
            .help("Antall behandlingsforsøk per unik kombinasjon av felt, verdi, fagområde og klassifikasjon")
            .labelNames("felt", "verdi", "kodeFagomraade", "kodeKlassifik")
            .withoutExemplars()
            .register(prometheusMeterRegistry.prometheusRegistry)

    val oppdragFeilCounter: Counter =
        Counter
            .builder()
            .name("${METRICS_NAMESPACE}_oppdrag_feil_total")
            .help("Antall ugyldige parseresultater og exceptions under meldingsbehandling")
            .labelNames("type")
            .withoutExemplars()
            .register(prometheusMeterRegistry.prometheusRegistry)

    fun registrerOppdrag(oppdrag: Oppdrag) {
        when (oppdrag) {
            is Oppdrag.UgyldigOppdrag -> oppdragFeilCounter.labelValues("ugyldig_oppdrag").inc()
            is Oppdrag.UgyldigKombinasjon -> oppdragFeilCounter.labelValues("ugyldig_kombinasjon").inc()
            is Oppdrag.Utbetaling, is Oppdrag.Simulering ->
                oppdrag.metrikker
                    .filter { it.felt in feltverdier }
                    .map { metrikk ->
                        MetrikkDetaljer(
                            metrikk.felt,
                            metrikk.tags["verdi"]?.takeIf { it.isNotBlank() } ?: "ukjent",
                            metrikk.tags["kodeFagomraade"]?.takeIf { it.isNotBlank() } ?: "ukjent",
                            metrikk.tags["kodeKlassifik"]?.takeIf { it.isNotBlank() } ?: "ukjent",
                        )
                    }.distinct()
                    .forEach { (felt, verdi, fagomraade, klassifik) ->
                        oppdragFeltCounter.labelValues(felt, verdi, fagomraade, klassifik).inc()
                    }
        }
    }
}

internal data class MetrikkDetaljer(
    val felt: String,
    val verdi: String?,
    val kodeFagomraade: String?,
    val kodeKlassifik: String?,
)
