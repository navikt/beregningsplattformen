package no.nav.sokos.dare.poc.oppdrag

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

import kotlinx.serialization.Serializable

import no.nav.sokos.dare.poc.aktoer.AktoerId
import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.structural.IdValueClass

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
) {
    @kotlinx.serialization.Transient
    val periode: DatoIntervall by lazy { DatoIntervall(datoVedtakFom, datoVedtakTom) }
}

@Serializable
@JvmInline
value class OppdragslinjeId(
    override val id: Long,
) : IdValueClass
