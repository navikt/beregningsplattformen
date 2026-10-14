package no.nav.utbetaling.beregning.oppdrag.intern

import no.nav.utbetaling.beregning.oppdrag.Bilagstype
import no.nav.utbetaling.beregning.oppdrag.DelytelseId
import no.nav.utbetaling.beregning.oppdrag.Fagområde
import no.nav.utbetaling.beregning.oppdrag.FagsystemId
import no.nav.utbetaling.beregning.oppdrag.Henvisning
import no.nav.utbetaling.beregning.oppdrag.Kidnummer
import no.nav.utbetaling.beregning.oppdrag.OppdragId
import no.nav.utbetaling.beregning.oppdrag.Oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.Opplysning
import no.nav.utbetaling.beregning.oppdrag.SaksbehandlerId
import no.nav.utbetaling.beregning.oppdrag.StønadId
import no.nav.utbetaling.beregning.oppdrag.Utbetalingsfrekvens
import no.nav.utbetaling.beregning.oppdrag.intern.Omposteringer.fra
import no.nav.utbetaling.beregning.oppdrag.xml.Oppdrag110
import no.nav.utbetaling.beregning.tid.Periode
import no.nav.utbetaling.beregning.tid.Tidslinje

internal object FraXmlFormat {
    fun opprett(oppdrag110: Oppdrag110): Oppdragstidslinje {
        val internOppdragId =
            InternOppdragId(
                fagområde = oppdrag110.kodeFagomraade.mapVerdi { Fagområde(it) } ?: error("Mangler kodeFagomraade på oppdragsnivå"),
                fagsystemId = oppdrag110.fagsystemId.mapVerdi { FagsystemId(it) } ?: error("Mangler fagsystemId på oppdragsnivå"),
            )

        val linjerPerKjede = Linjer.fra(oppdrag110)
        val kjedeendringer = kjedeendringerFra(linjerPerKjede)

        val kjeder =
            linjerPerKjede
                .mapValues { (kjede, linjer) ->
                    val statuser = kjedeendringer[kjede]?.kjedestatus ?: Tidslinje.Builder<Statusendring>().build()
                    linjer.aktive
                        // Verdier som entydig kan utledes fra verdier i linjen
                        .mapVerdier { oppdragsLinje150 ->
                            OpplysningerIntern.Builder(
                                delytelseId =
                                    oppdragsLinje150.delytelseId.mapVerdi { DelytelseId(it) }
                                        ?: error("Mangler delytelseId på linjenivå"),
                                linjeSaksbehandlerId =
                                    oppdragsLinje150.saksbehId.mapVerdi { SaksbehandlerId(it) }
                                        ?: error("Mangler saksbehandlerId på linjenivå"),
                                refDelytelseId = oppdragsLinje150.refDelytelseId.mapVerdi { DelytelseId(it) },
                                henvisning = oppdragsLinje150.henvisning.mapVerdi { Henvisning(it) },
                                sats = oppdragsLinje150.sats,
                                kidnummer = oppdragsLinje150.kid.mapVerdi { Kidnummer(it) },
                            )
                        }
                        // Periodierte verdier på linjen som er periodisert på et annet nivå enn linjen selv
                        .kombinerMed(statuser) { _, builder, statusendring ->
                            builder?.medStatus(statusendring)
                        }
                }
                // Periodiserte verdier fra oppdragsnivå som er periodisert på et annet nivå enn linjene
                .mapValues { (_, builderTidslinje) ->
                    builderTidslinje
                        .kombinerMed(Avventinger.fra(oppdrag110)) { _, builder, avventing ->
                            builder?.copy(avventingsårsakEndring = avventing)
                        }.kombinerMed(fra(oppdrag110)) { _, builder, ompostering ->
                            builder?.copy(omposteringEndring = ompostering)
                        }
                }
                // Bygger tidslinjen med kun linjenivå-verdier; oppdragsnivå løftes ut i oppdragsverdier
                .mapValues { (_, builderTidslinje) ->
                    builderTidslinje.mapVerdier { it.build() }
                }

        return Oppdragstidslinje(
            oppdragsverdier = mapOf(internOppdragId to oppdragsverdierFra(oppdrag110)),
            kjeder = kjeder,
            oppdragsendringer = mapOf(internOppdragId to oppdragsendringerFra(oppdrag110)),
            kjedeendringer = kjedeendringer,
        )
    }

    private fun oppdragsverdierFra(oppdrag110: Oppdrag110): Oppdragsverdier {
        val oppdragSaksbehandlerId =
            oppdrag110.saksbehId.mapVerdi { SaksbehandlerId(it) } ?: error("Mangler saksbehandler på oppdragNivå")
        return Oppdragsverdier(
            oppdragId = oppdrag110.oppdragsId?.let { Opplysning(oppdragSaksbehandlerId, OppdragId(it)) },
            stønadId = oppdrag110.stonadId.mapVerdi { StønadId(it) }?.let { Opplysning(oppdragSaksbehandlerId, it) },
            utbetalingsfrekvens =
                oppdrag110.utbetFrekvens
                    .mapVerdi { utbetalingsfrekvenser[it] ?: error("Kjenner ikke til utbetalingsfrekvens $it") }
                    ?.let { Opplysning(oppdragSaksbehandlerId, it) },
            bilagstype =
                oppdrag110.bilagstype113
                    ?.bilagsType
                    ?.mapVerdi { Bilagstype(it) }
                    ?.let { Opplysning(oppdragSaksbehandlerId, it) },
            forfall = oppdrag110.datoForfall?.let { Opplysning(oppdragSaksbehandlerId, it) },
        )
    }

    private fun oppdragsendringerFra(oppdrag110: Oppdrag110) =
        Oppdragsendringer(
            oppdragstatus = Statuser.fra(oppdrag110),
        )

    private fun kjedeendringerFra(linjerPerKjede: Map<Kjede, Linjer>) =
        linjerPerKjede.mapValues { (_, linjer) ->
            Kjedeendringer(
                sluttdato = (linjer.aktive.lastOrNull()?.first as? Periode.LukketPeriode)?.tom,
                kjedestatus = Statuser.fra(linjer),
            )
        }

    private val utbetalingsfrekvenser =
        mapOf(
            "DAG" to Utbetalingsfrekvens.Daglig,
            "UKE" to Utbetalingsfrekvens.Ukentlig,
            "MND" to Utbetalingsfrekvens.Månedlig,
            "14DG" to Utbetalingsfrekvens.PerFjortendeDag,
            "ENG" to Utbetalingsfrekvens.Engangsutbetaling,
        )
}
