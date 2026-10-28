package no.nav.utbetaling.beregning.lab.dto

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

import no.nav.utbetaling.beregning.oppdrag.Avventingsårsak
import no.nav.utbetaling.beregning.oppdrag.Oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.Opplysning
import no.nav.utbetaling.beregning.oppdrag.UtbetalesTil
import no.nav.utbetaling.beregning.tid.Periode

@Serializable
data class OppdragstidslinjeDTO(
    val kjeder: List<KjedeDTO>,
) {
    operator fun plus(other: OppdragstidslinjeDTO): OppdragstidslinjeDTO {
        val eksisterendePerKjedeId = kjeder.associateBy { it.id }
        val nyePerKjedeId = other.kjeder.associateBy { it.id }
        val oppdaterteKjeder =
            (kjeder.map { it.id } + other.kjeder.map { it.id }).distinct().map { kjedeId ->
                val eksisterendeKjede = eksisterendePerKjedeId[kjedeId]
                val nyKjede = nyePerKjedeId[kjedeId]
                when {
                    eksisterendeKjede != null && nyKjede != null ->
                        eksisterendeKjede.copy(
                            kjedeVersjoner = nyKjede.kjedeVersjoner + eksisterendeKjede.kjedeVersjoner,
                        )
                    eksisterendeKjede != null -> eksisterendeKjede
                    else -> checkNotNull(nyKjede)
                }
            }
        return OppdragstidslinjeDTO(kjeder = oppdaterteKjeder)
    }
}

@Serializable
data class KjedeDTO(
    @Contextual val id: UUID,
    val fagområde: String,
    val fagsystemId: String,
    val kjedeVersjoner: List<KjedeVersjonDTO>,
)

@Serializable
data class KjedeVersjonDTO(
    val innsending: Int,
    val kjedePerioder: List<KjedePerioderDTO>,
)

@Serializable
data class OpplysningDTO(
    val saksbehandlerId: String,
    val opplysning: String,
)

@Serializable
data class KjedePerioderDTO(
    @Contextual val fraDato: LocalDate,
    @Contextual val tilDato: LocalDate?,
    val delytelseId: String,
    val status: OpplysningDTO,
    val sats: OpplysningDTO,
    val avventingsårsak: OpplysningDTO?,
    val forfall: OpplysningDTO?,
    val utbetalesTil: OpplysningDTO?,
    val gjelderId: OpplysningDTO?,
    val henvisning: OpplysningDTO?,
    val stønadId: OpplysningDTO?,
    val utbetalingsfrekvens: OpplysningDTO?,
    val bilagstype: OpplysningDTO?,
    val kidnummer: OpplysningDTO?,
    val bostedsenhet: OpplysningDTO?,
    val behandlendeEnhet: OpplysningDTO?,
    val vedtakId: OpplysningDTO?,
    val brukKjøreplan: OpplysningDTO,
    val ompostering: OpplysningDTO?,
    val ytelseKlassekode: OpplysningDTO,
)

private fun <T> Opplysning<T>.toDto(stringverdi: (T) -> String) =
    OpplysningDTO(
        saksbehandlerId = saksbehandlerId.id,
        opplysning = stringverdi(opplysning),
    )

private val DATO_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

private val TIDSPUNKT_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss").withZone(ZoneId.of("Europe/Oslo"))

fun Oppdragstidslinje.toDTO(innsending: Int): OppdragstidslinjeDTO {
    val kjeder =
        tidslinjer.map { (kjedeId, tidslinje) ->
            val fagområde =
                tidslinje
                    .first()
                    .second.fagområde.fagområde
            val fagsystemId =
                tidslinje
                    .first()
                    .second.fagsystemId.id
            KjedeDTO(
                id = kjedeId,
                fagområde = fagområde,
                fagsystemId = fagsystemId,
                kjedeVersjoner =
                    listOf(
                        KjedeVersjonDTO(
                            innsending = innsending,
                            kjedePerioder =
                                tidslinje.map { (periode, opplysning) ->
                                    KjedePerioderDTO(
                                        fraDato = periode.fom,
                                        tilDato = (periode as? Periode.LukketPeriode)?.tom,
                                        status = opplysning.status.toDto { opplysning.status.opplysning.name },
                                        utbetalesTil =
                                            opplysning.utbetalesTil?.toDto {
                                                when (it) {
                                                    is UtbetalesTil.Organisasjon -> "Organisasjon ${it.organisasjonsnummer.nummer}"
                                                    is UtbetalesTil.Person -> "Person ${it.personidentifikator.identifikator}"
                                                    is UtbetalesTil.Samhandler -> "Samhandler ${it.samhandlerId.tssId}"
                                                }
                                            },
                                        forfall = opplysning.forfall?.toDto { it.format(DATO_FORMAT) },
                                        avventingsårsak =
                                            opplysning.avventingsårsak?.toDto {
                                                when (it) {
                                                    is Avventingsårsak.Avregning -> "Avregning - avventer til ${it.til.format(DATO_FORMAT)}"
                                                    is Avventingsårsak.Refusjonskrav ->
                                                        when (val til = it.til) {
                                                            null -> "Refusjonskrav uten sluttdato"
                                                            else -> "Refusjonskrav avventer til ${til.format(DATO_FORMAT)}"
                                                        }
                                                }
                                            },
                                        gjelderId = opplysning.gjelderId?.toDto { it.identifikator },
                                        delytelseId = opplysning.delytelseId.id,
                                        sats = opplysning.sats.toDto { it.toPlainString() },
                                        henvisning = opplysning.henvisning?.toDto { it.henvisning },
                                        stønadId = opplysning.stønadId?.toDto { it.id },
                                        utbetalingsfrekvens = opplysning.utbetalingsfrekvens?.toDto { it.name },
                                        bilagstype = opplysning.bilagstype?.toDto { it.type },
                                        kidnummer = opplysning.kidnummer?.toDto { it.nummer },
                                        bostedsenhet = opplysning.bostedsenhet?.toDto { it.enhet },
                                        behandlendeEnhet = opplysning.behandlendeEnhet?.toDto { it.enhet },
                                        vedtakId = opplysning.vedtakId?.toDto { it.id },
                                        brukKjøreplan = opplysning.brukKjøreplan.toDto { it.name },
                                        ompostering = opplysning.ompostering?.toDto { "Registrert ${TIDSPUNKT_FORMAT.format(it.registrert)}" },
                                        ytelseKlassekode = opplysning.ytelseKlassekode.toDto { it.kode },
                                    )
                                },
                        ),
                    ),
            )
        }
    return OppdragstidslinjeDTO(kjeder = kjeder)
}
