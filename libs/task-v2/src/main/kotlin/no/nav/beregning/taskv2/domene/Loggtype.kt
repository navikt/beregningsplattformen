package no.nav.beregning.taskv2.domene

/** Type hendelse i task_logg. Portet fra familie-prosessering. */
enum class Loggtype {
    FERDIG,
    FEILET,
    PLUKKET,
    BEHANDLER,
    KLAR_TIL_PLUKK,
    UBEHANDLET,
    AVVIKSHANDTERT,
    MANUELL_OPPFOLGING,
    KOMMENTAR,
}
