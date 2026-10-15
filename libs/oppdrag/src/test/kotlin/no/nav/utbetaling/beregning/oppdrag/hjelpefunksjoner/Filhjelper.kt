package no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner

object Filhjelper {
    internal fun lesFil(navn: String): String =
        Filhjelper::class.java.classLoader
            .getResourceAsStream(navn)
            ?.use { it.bufferedReader().readText() }
            ?: error("Fant ikke \"$navn\" på classpath")
}
