package no.nav.utbetaling.beregningsflyt

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import no.nav.utbetaling.beregning.oppdrag.Oppdrag
import no.nav.utbetaling.beregningsflyt.verifikasjon.Scenario

class Eksempeltest: FunSpec({
    withData(nameFn = { it.navn }, Scenario.alle) {
        when (val oppdrag = Oppdrag.fraJson(it.raw.oppdrag)) {
            is Oppdrag.Utbetaling -> println("OK")
            is Oppdrag.Simulering -> println("OK")
            is Oppdrag.UgyldigOppdrag -> println("Feil: ${oppdrag.feil.stackTraceToString()}")
            is Oppdrag.UgyldigKombinasjon -> println("Feil: ${oppdrag.feil.stackTraceToString()}")
        }
    }
})