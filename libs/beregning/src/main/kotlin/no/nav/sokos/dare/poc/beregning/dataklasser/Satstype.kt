package no.nav.sokos.dare.poc.beregning.dataklasser

import kotlinx.serialization.Serializable

@Serializable
enum class Satstype {
    DAG,
    MND,
    LOP,
    SAL,
    LOP_DAG,
    LOP_MND,
    LOP_PROSENT,
    SAL_DAG,
    SAL_MND,
    SAL_PROSENT,
    TABELL,
    PROSENT,
    ENG,
}
