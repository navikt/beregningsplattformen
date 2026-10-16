package no.nav.utbetaling.beregning.oppdrag

import java.time.LocalDate

import no.nav.utbetaling.beregning.oppdrag.intern.InternOppdragId
import no.nav.utbetaling.beregning.oppdrag.intern.Kjede
import no.nav.utbetaling.beregning.oppdrag.intern.Kjede.Companion.grupperPåKjede
import no.nav.utbetaling.beregning.oppdrag.intern.Kjedeendringer
import no.nav.utbetaling.beregning.oppdrag.intern.Oppdragsendringer
import no.nav.utbetaling.beregning.oppdrag.intern.Oppdragsverdier
import no.nav.utbetaling.beregning.oppdrag.intern.OpplysningerIntern
import no.nav.utbetaling.beregning.oppdrag.intern.Statusendring
import no.nav.utbetaling.beregning.tid.DefaultTidslinjesamling
import no.nav.utbetaling.beregning.tid.Periode
import no.nav.utbetaling.beregning.tid.Tidslinje
import no.nav.utbetaling.beregning.tid.Tidslinjesamling
import no.nav.utbetaling.beregning.tid.til

class Oppdragstidslinje private constructor(
    // Verdier som er lik for hele oppdraget på tvers av alle kjeder
    private val oppdragsverdier: Map<InternOppdragId, Oppdragsverdier>,
    // Tidslinjene for hver enkelt kjede
    private val kjeder: Map<Kjede, Tidslinje<OpplysningerIntern>>,
    // Endringer fra dagens oppdrag110 som påvirker hele oppdraget, ikke bare for den enkelte innsendingen
    private val oppdragsendringer: Map<InternOppdragId, Oppdragsendringer>,
    // Endringer fra dagens oppdrag110 som påvirker hele kjeden, ikke bare den kjededelen i den enkelte innsendingen
    private val kjedeendringer: Map<Kjede, Kjedeendringer>,
    // Det eksponerte 'API'-et til Oppdragstidslinje som alle utenom dette biblioteket forholder seg til
    tidslinjer: Map<Kjede, Tidslinje<Opplysninger>>,
) : Tidslinjesamling<Kjede, Opplysninger> by DefaultTidslinjesamling(tidslinjer) {
    internal constructor(
        oppdragsverdier: Map<InternOppdragId, Oppdragsverdier> = emptyMap(),
        kjeder: Map<Kjede, Tidslinje<OpplysningerIntern>> = emptyMap(),
        oppdragsendringer: Map<InternOppdragId, Oppdragsendringer> = emptyMap(),
        kjedeendringer: Map<Kjede, Kjedeendringer> = emptyMap(),
    ) : this(
        oppdragsverdier = oppdragsverdier,
        kjeder = kjeder.filterValues { !it.isEmpty() },
        tidslinjer =
            kjeder.filterValues { !it.isEmpty() }.mapValues { (kjede, tidslinje) ->
                tidslinje.mapVerdier { opplysningerIntern ->
                    kombinerTilOpplysninger(
                        kjede = kjede,
                        oppdragsverdier = oppdragsverdier.getValue(kjede.internOppdragId),
                        opplysningerIntern = opplysningerIntern,
                    )
                }
            },
        oppdragsendringer = oppdragsendringer,
        kjedeendringer = kjedeendringer,
    )

    private fun kombinerMed(
        other: Oppdragstidslinje,
        transform: (
            kjede: Kjede,
            eksisterende: Tidslinje<OpplysningerIntern>,
            ny: Tidslinje<OpplysningerIntern>,
        ) -> Tidslinje<OpplysningerIntern>,
    ): Oppdragstidslinje {
        // Her leter vi etter kjeder som hører sammen, så her finner vi ting som hører i samme kjede på en annen måte enn KjedetTidslinjesamling som
        // kun sjekker likhet på allerede etablerte kjeder
        val kjeder =
            (this.kjeder.toList() + other.kjeder.toList())
                .grupperPåKjede()
                .mapValues { (kjede, tidslinjer) ->
                    val tom = Tidslinje.Builder<OpplysningerIntern>().build()
                    val (eksisterende, ny) =
                        when (tidslinjer.size) {
                            1 -> if (kjede in this.kjeder.keys) tidslinjer.single() to tom else tom to tidslinjer.single()
                            2 -> tidslinjer.first() to tidslinjer.last()
                            else -> error("Kan ikke ha mer enn 2 tidslinjer som hører til samme kjede")
                        }
                    transform(kjede, eksisterende, ny)
                }
        return Oppdragstidslinje(
            oppdragsverdier = this.oppdragsverdier.medNyeVerdierFra(other.oppdragsverdier),
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
                    OpplysningerIntern.velg(eksisterendeOppdragsdag, nyOppdragsdag, periode, endretPeriode)
                }.kombinerMed(oppdragstatus) { _, oppdragsdag, statusendring ->
                    OpplysningerIntern.velgStatus(oppdragsdag, statusendring)
                }.kombinerMed(kjedestatus) { _, oppdragsdag, statusendring ->
                    OpplysningerIntern.velgStatus(oppdragsdag, statusendring)
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
        private fun Map<InternOppdragId, Oppdragsverdier>.medNyeVerdierFra(ny: Map<InternOppdragId, Oppdragsverdier>): Map<InternOppdragId, Oppdragsverdier> =
            (keys + ny.keys).associateWith { internOppdragId ->
                val eksisterende = this[internOppdragId]
                val nyVerdi = ny[internOppdragId]
                when {
                    eksisterende != null && nyVerdi != null -> eksisterende.medNyeVerdierFra(nyVerdi)
                    else -> eksisterende ?: nyVerdi!!
                }
            }

        private fun <T> Tidslinje<T>.periode() =
            firstOrNull()?.first?.fom?.let { fom ->
                val tom = (last().first as? Periode.LukketPeriode)?.tom
                if (tom == null) Periode.ÅpenPeriode(fom) else fom til tom
            } ?: Periode.ÅpenPeriode(LocalDate.MAX)

        private fun kombinerTilOpplysninger(
            kjede: Kjede,
            oppdragsverdier: Oppdragsverdier,
            opplysningerIntern: OpplysningerIntern,
        ) = Opplysninger(
            fagområde = kjede.fagområde,
            fagsystemId = kjede.fagsystemId,
            delytelseId = opplysningerIntern.delytelseId,
            refDelytelseId = opplysningerIntern.refDelytelseId,
            sats = opplysningerIntern.sats,
            oppdragId = oppdragsverdier.oppdragId,
            henvisning = opplysningerIntern.henvisning,
            stønadId = oppdragsverdier.stønadId,
            forfall = oppdragsverdier.forfall,
            utbetalingsfrekvens = oppdragsverdier.utbetalingsfrekvens,
            bilagstype = oppdragsverdier.bilagstype,
            avventingsårsak = opplysningerIntern.avventingsårsak,
            ompostering = opplysningerIntern.ompostering,
            kidnummer = opplysningerIntern.kidnummer,
            status = opplysningerIntern.status,
            bostedsenhet = opplysningerIntern.bostedsenhet ?: error("Mangler bostedsenhet for delytelse ${opplysningerIntern.delytelseId}"),
            nullableBehandlendeEnhet = opplysningerIntern.behandlendeEnhet,
        )
    }
}
