package no.nav.utbetaling.beregning.oppdrag

import java.time.LocalDate

import no.nav.utbetaling.beregning.oppdrag.Kjedbar.Companion.grupperPåKjede
import no.nav.utbetaling.beregning.oppdrag.xml.Oppdrag110
import no.nav.utbetaling.beregning.oppdrag.xml.XmlOppdrag
import no.nav.utbetaling.beregning.tid.DefaultTidslinjesamling
import no.nav.utbetaling.beregning.tid.Periode
import no.nav.utbetaling.beregning.tid.Periode.LukketPeriode.Companion.til
import no.nav.utbetaling.beregning.tid.Tidslinje
import no.nav.utbetaling.beregning.tid.Tidslinjesamling

class Oppdragstidslinje private constructor(
    // Verdier som er lik for hele oppdraget på tvers av alle kjeder
    private val oppdragsverdier: Map<InternOppdragId, Oppdragsverdier>,
    // Tidslinjene for hver enkelt kjede
    private val kjeder: Map<Kjede, Tidslinje<InternOppdragsdag>>,
    // Endringer fra dagens oppdrag110 som påvirker hele oppdraget, ikke bare for den enkelte innsendingen
    private val oppdragsendringer: Map<InternOppdragId, Oppdragsendringer>,
    // Endringer fra dagens oppdrag110 som påvirker hele kjeden, ikke bare den kjededelen i den enkelte innsendingen
    private val kjedeendringer: Map<Kjede, Kjedeendringer>,
    // Det eksponerte 'API'-et til Oppdragstidslinje som alle utenom dette biblioteket forholder seg til
    tidslinjer: Map<Kjede, Tidslinje<Oppdragsdag>>,
) : Tidslinjesamling<Kjede, Oppdragsdag> by DefaultTidslinjesamling(tidslinjer) {
    internal constructor(
        oppdragsverdier: Map<InternOppdragId, Oppdragsverdier> = emptyMap(),
        kjeder: Map<Kjede, Tidslinje<InternOppdragsdag>> = emptyMap(),
        oppdragsendringer: Map<InternOppdragId, Oppdragsendringer> = emptyMap(),
        kjedeendringer: Map<Kjede, Kjedeendringer> = emptyMap(),
    ) : this(
        oppdragsverdier = oppdragsverdier,
        kjeder = kjeder.filterValues { !it.isEmpty() },
        tidslinjer =
            kjeder.filterValues { !it.isEmpty() }.mapValues { (kjede, tidslinje) ->
                tidslinje.mapVerdier { somOppdragsdag(kjede, oppdragsverdier[kjede.internOppdragId], it) }
            },
        oppdragsendringer = oppdragsendringer,
        kjedeendringer = kjedeendringer,
    )

    init {
        require(
            this.kjeder.keys ==
                this.kjeder.entries
                    .map { (kjede) -> Kjedbar(kjede, Any()) }
                    .grupperPåKjede()
                    .keys,
        ) {
            "Kjedene i Oppdragstidslinje er ikke gruppert på riktig måte!"
        }
    }

    private fun kombinerMed(
        other: Oppdragstidslinje,
        transform: (
            kjede: Kjede,
            eksisterende: Tidslinje<InternOppdragsdag>,
            ny: Tidslinje<InternOppdragsdag>,
        ) -> Tidslinje<InternOppdragsdag>,
    ): Oppdragstidslinje {
        // Her leter vi etter kjeder som hører sammen, så her finner vi ting som hører i samme kjede på en annen måte enn KjedetTidslinjesamling som
        // kun sjekker likhet på allerede etablerte kjeder
        val kjeder =
            (this.kjeder.kjedbar() + other.kjeder.kjedbar())
                .grupperPåKjede()
                .mapValues { (kjede, tidslinjer) ->
                    val tom = Tidslinje.Builder<InternOppdragsdag>().build()
                    val (eksisterende, ny) =
                        when (tidslinjer.size) {
                            1 -> if (kjede in this.kjeder.keys) tidslinjer.single() to tom else tom to tidslinjer.single()
                            2 -> tidslinjer.first() to tidslinjer.last()
                            else -> error("Kan ikke ha mer enn 2 tidslinjer som hører til samme kjede")
                        }
                    transform(kjede, eksisterende, ny)
                }
        return Oppdragstidslinje(
            oppdragsverdier = this.oppdragsverdier.medNyeVedierFra(other.oppdragsverdier),
            kjeder = kjeder,
            oppdragsendringer = other.oppdragsendringer,
            kjedeendringer = other.kjedeendringer,
        )
    }

    private fun kjedeendringerFor(kjeden: Kjede) =
        kjedeendringer.firstNotNullOfOrNull { (kjede, kjedeendringer) ->
            kjedeendringer.takeIf { kjeden.hørerTil(kjede) }
        } ?: Kjedeendringer(sluttdato = null, kjedestatus = Tidslinje.Builder<Statusendring>().build())

    operator fun plus(other: Oppdragstidslinje): Oppdragstidslinje {
        val endretOppdragstidslinje = this - other

        return kombinerMed(endretOppdragstidslinje) { kjede, eksisterendeTidslinje, endretTidslinje ->
            val oppdragstatus =
                endretOppdragstidslinje.oppdragsendringer[kjede.internOppdragId]?.oppdragstatus
                    ?: Tidslinje.Builder<Statusendring>().build()
            val (kjedeSluttdato, kjedestatus) = endretOppdragstidslinje.kjedeendringerFor(kjede)
            val endretPeriode = endretTidslinje.periode()
            val utgangspunkt =
                when (kjedeSluttdato) {
                    null -> eksisterendeTidslinje
                    else -> eksisterendeTidslinje.begrensetTil(kjedeSluttdato)
                }

            utgangspunkt
                .kombinerMed(endretTidslinje) { periode, eksisterendeOppdragsdag, nyOppdragsdag ->
                    InternOppdragsdag.velg(eksisterendeOppdragsdag, nyOppdragsdag, periode, endretPeriode)
                }.kombinerMed(oppdragstatus) { _, oppdragsdag, statusendring ->
                    InternOppdragsdag.velgStatus(oppdragsdag, statusendring)
                }.kombinerMed(kjedestatus) { _, oppdragsdag, statusendring ->
                    InternOppdragsdag.velgStatus(oppdragsdag, statusendring)
                }
        }
    }

    operator fun minus(other: Oppdragstidslinje) =
        kombinerMed(other) { _, eksisterendeTidslinje, nyTidslinje ->
            eksisterendeTidslinje.kombinerMed(nyTidslinje) { _, eksisterendeOppdragsdag, nyOppdragsdag ->
                nyOppdragsdag.takeUnless { it == eksisterendeOppdragsdag }
            }
        }

    override fun equals(other: Any?) = this === other || (other is Oppdragstidslinje && kjeder == other.kjeder)

    override fun hashCode() = kjeder.hashCode()

    override fun toString(): String {
        if (isEmpty()) return "Tom Oppdragstidslinje"
        return buildString {
            kjeder.forEach { (fagområde, fagsystemId), tidslinje ->
                appendLine("FagsystemId ${fagsystemId.id} for ${fagområde.fagområde}:")
                append(tidslinje.toString())
            }
        }
    }

    companion object {
        private fun Map<Kjede, Tidslinje<InternOppdragsdag>>.kjedbar() = map { (kjede, tidslinje) -> Kjedbar(kjede, tidslinje) }

        private fun Map<InternOppdragId, Oppdragsverdier>.medNyeVedierFra(ny: Map<InternOppdragId, Oppdragsverdier>): Map<InternOppdragId, Oppdragsverdier> =
            (keys + ny.keys).associateWith { internOppdragId ->
                val eksisterende = this[internOppdragId]
                val nyVerdi = ny[internOppdragId]
                when {
                    eksisterende != null && nyVerdi != null -> eksisterende.medNyeVedierFra(nyVerdi)
                    else -> eksisterende ?: nyVerdi!!
                }
            }

        private fun <T> Tidslinje<T>.periode() =
            firstOrNull()?.first?.fom?.let { fom ->
                val tom = (last().first as? Periode.LukketPeriode)?.tom
                if (tom == null) Periode.ÅpenPeriode(fom) else fom til tom
            } ?: Periode.ÅpenPeriode(LocalDate.MAX)

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

        private fun somOppdragsdag(
            kjede: Kjede,
            oppdragsverdier: Oppdragsverdier?,
            internOppdragsdag: InternOppdragsdag,
        ) = Oppdragsdag(
            fagområde = kjede.fagområde,
            fagsystemId = kjede.fagsystemId,
            delytelseId = internOppdragsdag.delytelseId,
            refDelytelseId = internOppdragsdag.refDelytelseId,
            sats = internOppdragsdag.sats,
            oppdragId = oppdragsverdier?.oppdragId,
            henvisning = internOppdragsdag.henvisning,
            stønadId = oppdragsverdier?.stønadId,
            forfall = oppdragsverdier?.forfall,
            utbetalingsfrekvens = oppdragsverdier?.utbetalingsfrekvens,
            bilagstype = oppdragsverdier?.bilagstype,
            avventingsårsak = internOppdragsdag.avventingsårsak,
            ompostering = internOppdragsdag.ompostering,
            kidnummer = internOppdragsdag.kidnummer,
            status = internOppdragsdag.status,
        )

        private val utbetalingsfrekvenser =
            mapOf(
                "DAG" to Utbetalingsfrekvens.Daglig,
                "UKE" to Utbetalingsfrekvens.Ukentlig,
                "MND" to Utbetalingsfrekvens.Månedlig,
                "14DG" to Utbetalingsfrekvens.PerFjortendeDag,
                "ENG" to Utbetalingsfrekvens.Engangsutbetaling,
            )

        internal fun fraXmlOppdrag(xmlOppdrag: XmlOppdrag): Oppdragstidslinje {
            val oppdrag110 = xmlOppdrag.oppdrag110 ?: return Oppdragstidslinje()
            return fraOppdrag110(oppdrag110)
        }

        internal fun fraOppdrag110(oppdrag110: Oppdrag110): Oppdragstidslinje {
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
                                InternOppdragsdag.Builder(
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
                            .kombinerMed(avventingerFra(oppdrag110)) { _, builder, avventing ->
                                builder?.copy(avventingsårsakEndring = avventing)
                            }.kombinerMed(omposteringerFra(oppdrag110)) { _, builder, ompostering ->
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

        internal fun fraTidslinjer(tidslinjer: List<Tidslinje<Oppdragsdag>>): Oppdragstidslinje {
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
                            oppdragId = oppdragsdag.oppdragId,
                            stønadId = oppdragsdag.stønadId,
                            forfall = oppdragsdag.forfall,
                            utbetalingsfrekvens = oppdragsdag.utbetalingsfrekvens,
                            bilagstype = oppdragsdag.bilagstype,
                        )
                    }

            val kjeder =
                tidslinjerPerKjede.mapValues { (_, tidslinje) ->
                    tidslinje.mapVerdier { oppdragsdag ->
                        InternOppdragsdag(
                            delytelseId = oppdragsdag.delytelseId,
                            refDelytelseId = oppdragsdag.refDelytelseId,
                            sats = oppdragsdag.sats,
                            henvisning = oppdragsdag.henvisning,
                            avventingsårsakEndring = oppdragsdag.avventingsårsak?.let { Endring.Satt(it) },
                            omposteringEndring = oppdragsdag.ompostering?.let { Endring.Satt(it) },
                            kidnummer = oppdragsdag.kidnummer,
                            status = oppdragsdag.status,
                        )
                    }
                }

            return Oppdragstidslinje(oppdragsverdier = oppdragsverdier, kjeder = kjeder)
        }
    }
}
