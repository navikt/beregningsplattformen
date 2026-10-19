package no.nav.utbetaling.beregning.lab.service

import no.nav.utbetaling.beregning.oppdrag.Oppdrag
import no.nav.utbetaling.beregning.oppdrag.Oppdragstidslinje

class OppdragTolkningService {
    fun tolkOppdrag(oppdrag: Oppdrag): Oppdragstidslinje {
        val oppdragstidslinje =
            when (oppdrag) {
                is Oppdrag.Utbetaling -> oppdrag.oppdragstidslinje
                is Oppdrag.Simulering -> oppdrag.oppdragstidslinje
                is Oppdrag.UgyldigOppdrag -> throw IllegalArgumentException("Ugyldig oppdrag: ${oppdrag.feil.message}", oppdrag.feil)
                is Oppdrag.UgyldigKombinasjon -> throw IllegalArgumentException("Ugyldig kombinasjon av oppdrag: ${oppdrag.feil.message}", oppdrag.feil)
            }
        return oppdragstidslinje
    }
}
