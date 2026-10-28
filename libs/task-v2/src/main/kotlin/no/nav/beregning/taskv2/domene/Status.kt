package no.nav.beregning.taskv2.domene

/** Livssyklus for en task. Portet fra familie-prosessering (8 statuser). */
enum class Status {
    FERDIG,
    FEILET,
    PLUKKET,
    BEHANDLER,
    KLAR_TIL_PLUKK,
    UBEHANDLET,
    AVVIKSHANDTERT,
    MANUELL_OPPFOLGING,
}
