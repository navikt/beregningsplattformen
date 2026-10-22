package no.nav.utbetaling.beregning.oppdrag

import java.time.LocalDate
import java.util.UUID

import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

import no.nav.utbetaling.beregning.oppdrag.intern.Kjede
import no.nav.utbetaling.beregning.oppdrag.intern.Kjede.Companion.grupperPåKjede
import no.nav.utbetaling.beregning.oppdrag.intern.Kjedeendringer
import no.nav.utbetaling.beregning.oppdrag.intern.OppdragId
import no.nav.utbetaling.beregning.oppdrag.intern.Oppdragsendringer
import no.nav.utbetaling.beregning.oppdrag.intern.Oppdragsverdier
import no.nav.utbetaling.beregning.oppdrag.intern.OpplysningerIntern
import no.nav.utbetaling.beregning.oppdrag.intern.OpplysningerIntern.Companion.viderefør
import no.nav.utbetaling.beregning.oppdrag.intern.Statusendring
import no.nav.utbetaling.beregning.tid.DefaultTidslinjesamling
import no.nav.utbetaling.beregning.tid.Periode
import no.nav.utbetaling.beregning.tid.Tidslinje
import no.nav.utbetaling.beregning.tid.Tidslinjesamling
import no.nav.utbetaling.beregning.tid.til

class Oppdragstidslinje private constructor(
    // Verdier som er lik for hele oppdraget på tvers av alle kjeder
    private val oppdragsverdier: Map<OppdragId, Oppdragsverdier>,
    // Tidslinjene for hver enkelt kjede
    private val kjeder: Map<Kjede, Tidslinje<OpplysningerIntern>>,
    // Endringer fra dagens oppdrag110 som påvirker hele oppdraget, ikke bare for den enkelte innsendingen
    private val oppdragsendringer: Map<OppdragId, Oppdragsendringer>,
    // Endringer fra dagens oppdrag110 som påvirker hele kjeden, ikke bare den kjededelen i den enkelte innsendingen
    private val kjedeendringer: Map<Kjede, Kjedeendringer>,
    // Det eksponerte 'API'-et til Oppdragstidslinje som alle utenom dette biblioteket forholder seg til
    tidslinjer: Map<UUID, Tidslinje<Opplysninger>>,
) : Tidslinjesamling<UUID, Opplysninger> by DefaultTidslinjesamling(tidslinjer) {
    internal constructor(
        oppdragsverdier: Map<OppdragId, Oppdragsverdier>,
        kjeder: Map<Kjede, Tidslinje<OpplysningerIntern>>,
        oppdragsendringer: Map<OppdragId, Oppdragsendringer> = emptyMap(),
        kjedeendringer: Map<Kjede, Kjedeendringer> = emptyMap(),
    ) : this(
        oppdragsverdier = oppdragsverdier,
        kjeder = kjeder.filterValues { !it.isEmpty() },
        tidslinjer =
            kjeder.entries.associate { (kjede, tidslinje) ->
                kjede.id.toJavaUuid() to
                    tidslinje.mapVerdier { opplysningerIntern ->
                        kombinerTilOpplysninger(
                            kjede = kjede,
                            oppdragsverdier = oppdragsverdier.getValue(kjede.oppdragId),
                            opplysningerIntern = opplysningerIntern,
                        )
                    }
            },
        oppdragsendringer = oppdragsendringer,
        kjedeendringer = kjedeendringer,
    )

    constructor(): this(
        oppdragsverdier = emptyMap(),
        kjeder = emptyMap(),
        oppdragsendringer = emptyMap(),
        kjedeendringer = emptyMap(),
    )

    private fun kombinerMed(
        other: Oppdragstidslinje,
        transform: (
            kjede: Kjede,
            eksisterende: Tidslinje<OpplysningerIntern>,
            ny: Tidslinje<OpplysningerIntern>,
        ) -> Tidslinje<OpplysningerIntern>,
    ): Oppdragstidslinje {
        // Her kombinerer vi kjeder som hører sammen basert på den linkede kjeden som bruker delytelseId og refDelytelseId.
        // Derfor gjøres det ikke en likhetssjekk for å finne rett kjede, men finner kjeder som hører sammen og kombinerer tidslinjene for disse.
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
                endretOppdragstidslinje.oppdragsendringer[kjede.oppdragId]?.oppdragstatus
                    ?: Tidslinje.Builder<Statusendring>().build()
            val (kjedeSluttdato, kjedestatus) = endretOppdragstidslinje.kjedeendringerFor(kjede)
            val endretPeriode = endretTidslinje.periode()
            val utgangspunkt =
                when (kjedeSluttdato) {
                    null -> eksisterendeTidslinje
                    else -> eksisterendeTidslinje.begrensetTil(kjedeSluttdato)
                }

            utgangspunkt
                .kombinerMed(endretTidslinje) { periode, eksisterendeOpplysninger, nyeOpplysninger ->
                    OpplysningerIntern.velg(eksisterendeOpplysninger, nyeOpplysninger, periode, endretPeriode)
                }.kombinerMed(oppdragstatus) { _, opplysninger, statusendring ->
                    OpplysningerIntern.velgStatus(opplysninger, statusendring)
                }.kombinerMed(kjedestatus) { _, opplysninger, statusendring ->
                    OpplysningerIntern.velgStatus(opplysninger, statusendring)
                }.kombinerMed(eksisterendeTidslinje.viderefør(OpplysningerIntern::gjelderId)) { _, opplysninger, gjelderId ->
                    OpplysningerIntern.videreførGjelderId(opplysninger, gjelderId)
                }.kombinerMed(eksisterendeTidslinje.viderefør(OpplysningerIntern::utbetalesTil)) { _, opplysninger, utbetalesTil ->
                    OpplysningerIntern.videreførUtbetalesTil(opplysninger, utbetalesTil)
                }
        }
    }

    operator fun minus(other: Oppdragstidslinje) =
        kombinerMed(other) { _, eksisterendeTidslinje, nyTidslinje ->
            eksisterendeTidslinje.kombinerMed(nyTidslinje) { _, eksisterendeOpplysninger, nyeOpplysninger ->
                nyeOpplysninger.takeUnless { it == eksisterendeOpplysninger }
            }
        }

    override fun equals(other: Any?) = this === other || (other is Oppdragstidslinje && kjeder == other.kjeder)

    override fun hashCode() = kjeder.hashCode()

    override fun toString(): String {
        if (isEmpty()) return "Tom Oppdragstidslinje"
        return buildString {
            kjeder.forEach { (id: Uuid, fagområde, fagsystemId), tidslinje ->
                appendLine("FagsystemId ${fagsystemId.id} for ${fagområde.fagområde} ($id):")
                append(tidslinje.toString())
            }
        }
    }

    companion object {
        private fun Map<OppdragId, Oppdragsverdier>.medNyeVerdierFra(ny: Map<OppdragId, Oppdragsverdier>): Map<OppdragId, Oppdragsverdier> =
            (keys + ny.keys).associateWith { oppdragId ->
                val eksisterende = this[oppdragId]
                val nyVerdi = ny[oppdragId]
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
            gjelderId = opplysningerIntern.gjelderId,
            sats = opplysningerIntern.sats,
            henvisning = opplysningerIntern.henvisning,
            stønadId = oppdragsverdier.stønadId,
            forfall = oppdragsverdier.forfall,
            utbetalingsfrekvens = oppdragsverdier.utbetalingsfrekvens,
            bilagstype = oppdragsverdier.bilagstype,
            avventingsårsak = opplysningerIntern.avventingsårsak,
            kidnummer = opplysningerIntern.kidnummer,
            status = opplysningerIntern.status,
            bostedsenhet = opplysningerIntern.bostedsenhet,
            behandlendeEnhet = opplysningerIntern.behandlendeEnhet,
            utbetalesTil = opplysningerIntern.utbetalesTil,
        )
    }
}
