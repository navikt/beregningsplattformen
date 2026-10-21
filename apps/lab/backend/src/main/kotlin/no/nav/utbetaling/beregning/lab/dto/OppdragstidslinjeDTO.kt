package no.nav.utbetaling.beregning.lab.dto

import java.time.LocalDate
import java.util.UUID

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

import no.nav.utbetaling.beregning.oppdrag.Oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.UtbetalesTil
import no.nav.utbetaling.beregning.tid.Periode

@Serializable
data class OppdragstidslinjeDTO(
    val kjeder: List<KjedeDTO>,
)

@Serializable
data class KjedeDTO(
    @Contextual val id: UUID,
    val kjedeVersjoner: List<KjedeVersjonDTO>,
)

@Serializable
data class KjedeVersjonDTO(
    val kjedePerioder: List<KjedePerioderDTO>,
)

@Serializable
data class KjedePerioderDTO(
    @Contextual val fraDato: LocalDate,
    @Contextual val tilDato: LocalDate?,
    val utbetalesTil: String,
)

fun Oppdragstidslinje.toDTO(): OppdragstidslinjeDTO {
    val kjeder =
        tidslinjer.map { (kjedeUUID, tidslinje) ->
            KjedeDTO(
                id = kjedeUUID,
                kjedeVersjoner =
                    listOf(
                        KjedeVersjonDTO(
                            kjedePerioder =
                                tidslinje.map { (periode, opplysning) ->
                                    KjedePerioderDTO(
                                        fraDato = periode.fom,
                                        tilDato = (periode as? Periode.LukketPeriode)?.tom,
                                        utbetalesTil =
                                            when (val utbetalesTil = opplysning.utbetalesTil?.opplysning) {
                                                is UtbetalesTil.Person -> utbetalesTil.personidentifikator.identifikator
                                                is UtbetalesTil.Organisasjon -> utbetalesTil.organisasjonsnummer.nummer
                                                is UtbetalesTil.Samhandler -> utbetalesTil.samhandlerId.tssId
                                                null -> "Ukjent mottaker"
                                            },
                                    )
                                },
                        ),
                    ),
            )
        }
    return OppdragstidslinjeDTO(kjeder = kjeder)
}
