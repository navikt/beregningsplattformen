package no.nav.utbetaling.beregning.oppdragsmottak

import java.time.LocalDateTime

import no.nav.utbetaling.beregning.oppdrag.Fagområde
import no.nav.utbetaling.beregning.oppdrag.FagsystemId
import no.nav.utbetaling.beregning.oppdrag.Oppdragstidslinje

class Oppdragsmottak(
    config: Map<String, String> = System.getenv(),
) {
    private val oppdragXmlConsumer = OppdragXmlConsumer(config)

    fun hentOppdragstidslinje(
        fagområde: Fagområde,
        fagsystemId: FagsystemId,
        beregningstidspunkt: LocalDateTime,
    ): Oppdragstidslinje {
        return Oppdragstidslinje() // TODO hente fra database
    }

    fun start() {
        oppdragXmlConsumer.start()
    }

    fun stop() {
        oppdragXmlConsumer.close()
    }
}
