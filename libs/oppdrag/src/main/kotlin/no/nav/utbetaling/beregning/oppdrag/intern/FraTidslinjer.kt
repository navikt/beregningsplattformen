package no.nav.utbetaling.beregning.oppdrag.intern

import no.nav.utbetaling.beregning.oppdrag.Oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.Opplysninger
import no.nav.utbetaling.beregning.tid.Tidslinje

internal object FraTidslinjer {
    internal fun opprett(tidslinjer: List<Tidslinje<Opplysninger>>): Oppdragstidslinje {
        val tidslinjerPerKjede =
            tidslinjer
                .filterNot { it.isEmpty() }
                .associateBy { tidslinje ->
                    Kjede(
                        fagområde = tidslinje.first().second.fagområde,
                        fagsystemId = tidslinje.first().second.fagsystemId,
                        delytelseIder =
                            tidslinje
                                .flatMap { (_, oppdragsdag) ->
                                    listOfNotNull(oppdragsdag.delytelseId, oppdragsdag.refDelytelseId)
                                }.toSet(),
                    )
                }

        val oppdragsverdier =
            tidslinjerPerKjede.entries
                .groupBy({ (kjede, _) -> kjede.internOppdragId }, { (_, tidslinje) -> tidslinje.first().second })
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
                        omposteringEndring = oppdragsdag.ompostering?.let { FeilregistrerbarOpplysning.Registrert(it) },
                        kidnummer = oppdragsdag.kidnummer,
                        status = oppdragsdag.status,
                        bostedsenhet = oppdragsdag.bostedsenhet,
                        behandlendeEnhetEndring = oppdragsdag.behandlendeEnhet?.let { FeilregistrerbarOpplysning.Registrert(it) },
                        utbetalesTil = oppdragsdag.utbetalesTil,
                    )
                }
            }

        return Oppdragstidslinje(oppdragsverdier = oppdragsverdier, kjeder = kjeder)
    }
}
