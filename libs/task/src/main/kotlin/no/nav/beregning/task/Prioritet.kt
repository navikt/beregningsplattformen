package no.nav.beregning.task

/**
 * Prioritet avgjør hvilken task som plukkes når flere er klare samtidig.
 *
 * **Lavere verdi = høyere prioritet**, som `nice` i Unix. Skalaen går "feil" vei fordi
 * plukkespørringen sorterer `ORDER BY prioritet, neste_kjoring` stigende, slik at Postgres
 * kan lese radene ferdig sortert rett ut av plukkeindeksen uten et eget sorteringssteg.
 *
 * Verdiene ligger med luft mellom seg så nye nivåer kan settes inn uten migrasjon.
 * Prioritet er en vanlig Int, ikke en enum, nettopp for å tillate mellomverdier.
 */
object Prioritet {
    /** Saksbehandler eller bruker venter aktivt på resultatet. Bruk sparsomt. */
    const val KRITISK: Int = 10

    /** Brukerinitiert arbeid som bør skje raskt, men der ingen venter foran skjermen. */
    const val HOY: Int = 20

    /** Standard. */
    const val NORMAL: Int = 30

    /** Bakgrunnsarbeid som kan vente. */
    const val LAV: Int = 40
}
