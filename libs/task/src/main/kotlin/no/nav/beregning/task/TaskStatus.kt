package no.nav.beregning.task

/**
 * Livssyklus for en task. Samme verdier som motors `JobbStatus`.
 *
 * KLAR    - lagt i kø, venter på å bli plukket
 * PLUKKET - en worker har tatt tasken og kjører den nå
 * FERDIG  - utført uten feil, kan arkiveres
 * FEILET  - utføreren kastet; venter på nytt forsøk eller manuell håndtering
 * AVBRUTT - stoppet manuelt, skal ikke plukkes på nytt
 */
enum class TaskStatus {
    KLAR,
    PLUKKET,
    FERDIG,
    FEILET,
    AVBRUTT,
}
