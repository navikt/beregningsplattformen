package no.nav.utbetaling.beregning.oppdrag.input_for_beregning

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@JvmInline
value class OppdragslinjeId(
    val id: Long,
)

enum class YtelseKlasseKode {
    TODO,
}

enum class TypeSats(
    private val satsNavn: String,
) {
    DAG("DAG"),
    UKE("UKE"), // På vei ut
    MND("MND"),
    FJORTEN_DAGER("14D"), // På vei ut
    AAR("AAR"), // på vei ut
    ENGANGS("ENG"),
    AKTO("AKTO"), // A-konto utbetaling (aldri brukt)
}

data class Oppdragslinje(
    val id: OppdragslinjeId? = null,
    val oppdragslinjekjedeId: OppdragslinjekjedeId? = null,
    val linjeId: Short,
    val delytelseId: String,
    val sats: BigDecimal,
    val typeSats: TypeSats,
    val brukKjoreplan: Boolean? = null,
    val datoVedtakFom: LocalDate,
    val datoVedtakTom: LocalDate? = null,
    val vedtakId: String,
    val henvisning: String? = null,
    val utbetalesTilId: AktoerId? = null,
    val kodeKlasse: YtelseKlasseKode,
    val skyldnerId: AktoerId? = null,
    val kid: String? = null,
    val typeSoknad: String? = null,
    // Brukes i forbindelse med refusjon av sykepenger/foreldrepenger, visstnok
    val maksDato: LocalDate? = null,
    val refunderesId: String? = null,
    val bruker: String,
    val tidspktReg: LocalDateTime,
    val vedtakssats: BigDecimal? = null,
    val annullert: Boolean = false,
)
