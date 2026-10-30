package no.nav.utbetaling.beregning.oppdrag

enum class YtelseSatstype(
    val kode: String,
) {
    DAG("DAG"),
    UKE("UKE"),
    MND("MND"),
    FJORTEN_DAGER("14D"),
    AAR("AAR"),
    ENG("ENG"),
    AKTO("AKTO"),
    ;

    companion object {
        fun fraXml(kode: String): YtelseSatstype {
            val satstype = entries.singleOrNull { it.kode == kode }
            require(satstype != null) { "Ukjent satstype: $kode" }
            return satstype
        }
    }
}
