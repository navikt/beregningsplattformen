package no.nav.utbetaling.beregning.oppdrag.intern

import java.util.UUID

import kotlin.uuid.Uuid

import no.nav.utbetaling.beregning.oppdrag.Fagområde
import no.nav.utbetaling.beregning.oppdrag.FagsystemId
import no.nav.utbetaling.beregning.oppdrag.Oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.Opplysninger
import no.nav.utbetaling.beregning.oppdrag.UgyldigInnsendingException.Companion.valider
import no.nav.utbetaling.beregning.tid.Tidslinje
import no.nav.utbetaling.beregning.tid.Tidslinjesamling

internal object FraTidslinjesamling {
    internal fun opprett(tidslinjesamling: Tidslinjesamling<UUID, Opplysninger>): Oppdragstidslinje {
        val tidslinjerPerKjede =
            tidslinjesamling.tidslinjer
                .filterValues { !it.isEmpty() }
                .mapKeys { (kjedeId, opplysinger) ->
                    val (fagområde, fagsystemId) = fagområdeOgFagsytemId(opplysinger)
                    Kjede(
                        id = Uuid.parse(kjedeId.toString()),
                        fagområde = fagområde,
                        fagsystemId = fagsystemId,
                        delytelseIder =
                            opplysinger
                                .flatMap { (_, oppdragsdag) ->
                                    listOfNotNull(oppdragsdag.delytelseId, oppdragsdag.refDelytelseId)
                                }.toSet(),
                    )
                }

        val oppdragsverdier =
            tidslinjerPerKjede.entries
                .groupBy({ (kjede, _) -> kjede.oppdragId }, { (_, tidslinje) -> tidslinje.first().second })
                .mapValues { (_, førsteDager) ->
                    val oppdragsdag = førsteDager.first()
                    Oppdragsverdier(
                        stønadId = oppdragsdag.stønadId,
                        forfall = oppdragsdag.forfall,
                        utbetalingsfrekvens = oppdragsdag.utbetalingsfrekvens,
                        bilagstype = oppdragsdag.bilagstype,
                    )
                }

        val kjeder =
            tidslinjerPerKjede.mapValues { (_, tidslinje) ->
                tidslinje.mapVerdier { oppdragsdag ->
                    OpplysningerIntern(
                        delytelseId = oppdragsdag.delytelseId,
                        refDelytelseId = oppdragsdag.refDelytelseId,
                        gjelderId = oppdragsdag.gjelderId,
                        sats = oppdragsdag.sats,
                        henvisning = oppdragsdag.henvisning,
                        avventingsårsakEndring = oppdragsdag.avventingsårsak?.let { FeilregistrerbarOpplysning.Registrert(it) },
                        kidnummer = oppdragsdag.kidnummer,
                        status = oppdragsdag.status,
                        bostedsenhet = oppdragsdag.bostedsenhet,
                        behandlendeEnhetEndring = oppdragsdag.behandlendeEnhet?.let { FeilregistrerbarOpplysning.Registrert(it) },
                        utbetalesTil = oppdragsdag.utbetalesTil,
                        vedtakId = oppdragsdag.vedtakId,
                    )
                }
            }

        return Oppdragstidslinje(oppdragsverdier = oppdragsverdier, kjeder = kjeder)
    }

    private fun fagområdeOgFagsytemId(tidslinje: Tidslinje<Opplysninger>): Pair<Fagområde, FagsystemId> {
        val førsteOpplysninger = tidslinje.first().second
        val fagområde = førsteOpplysninger.fagområde
        val fagsystemId = førsteOpplysninger.fagsystemId
        valider(tidslinje.all { (_, opplysninger) -> opplysninger.fagområde == fagområde && opplysninger.fagsystemId == fagsystemId }) {
            "Kjeden kombinerer flere fagområder eller fagsystemer: ${tidslinje.map { (_, opplysninger) -> "${opplysninger.fagområde}:${opplysninger.fagsystemId}" }}"
        }
        // DelytelseIder kan ikke valideres ettersom en linje kan bli overskrevet i sin helhet, og man da mangler data til å gjenskap den linkede listen.
        return fagområde to fagsystemId
    }
}
