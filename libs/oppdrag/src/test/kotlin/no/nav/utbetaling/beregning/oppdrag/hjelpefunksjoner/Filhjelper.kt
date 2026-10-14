package no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner

import no.nav.utbetaling.beregning.oppdrag.intern.Oppdrag
import no.nav.utbetaling.beregning.oppdrag.Oppdragstidslinje

object Filhjelper {
    internal fun lesFil(navn: String): String =
        Filhjelper::class.java.classLoader
            .getResourceAsStream(navn)
            ?.use { it.bufferedReader().readText() }
            ?: error("Fant ikke \"$navn\" på classpath")

    internal fun fraJson(vararg json: String): Oppdragstidslinje =
        when (val oppdrag = Oppdrag.fraJson(*json)) {
            is Oppdrag.Utbetaling -> oppdrag.oppdragstidslinje
            is Oppdrag.Simulering -> oppdrag.oppdragstidslinje
            is Oppdrag.UgyldigFormat -> error(oppdrag.feil)
            is Oppdrag.UgyldigKombinasjon -> error(oppdrag.feil)
            is Oppdrag.UgyldigOppdrag -> error(oppdrag.feil)
        }
}
