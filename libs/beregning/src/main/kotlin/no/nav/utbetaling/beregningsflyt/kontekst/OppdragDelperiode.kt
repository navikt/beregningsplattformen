package no.nav.utbetaling.beregningsflyt.kontekst

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

import no.nav.sokos.dare.poc.aktoer.AktoerId
import no.nav.sokos.dare.poc.beregning.Beregning
import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.beregning.EkstraInfo
import no.nav.sokos.dare.poc.beregning.dataklasser.BeregningsStatus
import no.nav.sokos.dare.poc.beregning.fordeler.Delperiodegrunnlag
import no.nav.sokos.dare.poc.beregning.fordeler.TrekkvedtakForBeregning
import no.nav.sokos.dare.poc.beregning.isWeekday
import no.nav.sokos.dare.poc.oppdrag.OppdragId
import no.nav.sokos.dare.poc.trekk.FagomraadeKode
import no.nav.sokos.dare.poc.trekk.TrekkvedtakId
import no.nav.utbetaling.beregning.oppdrag.Opplysninger
import no.nav.utbetaling.beregning.oppdrag.Status
import no.nav.utbetaling.beregning.oppdrag.YtelseSatstype
import no.nav.utbetaling.beregning.tid.Tidslinjesamling
import no.nav.utbetaling.beregningsflyt.identitet.Beregningsidentitet
import no.nav.utbetaling.beregningsflyt.identitet.Klassekodeoppslag
import no.nav.utbetaling.beregningsflyt.identitet.Oppdragsnokkel

class OppdragDelperiode(
    oppdrag: Tidslinjesamling<UUID, Opplysninger>,
    val oppdragsnokkel: Oppdragsnokkel,
    override val oppdragId: OppdragId,
    override val fagomraade: FagomraadeKode,
    identitet: Beregningsidentitet,
    val klassekoder: Klassekodeoppslag,
    override val periode: DatoIntervall,
    override val konto: String,
    override val gjelder: AktoerId,
    override val beregnet: LocalDateTime,
    override val utbetalesTil: AktoerId?,
    override val status: BeregningsStatus,
    override val trekk: List<TrekkvedtakForBeregning>,
    ekstraInfo: MutableList<EkstraInfo> = mutableListOf(),
    override val overfoeringsdato: LocalDate?,
    override val tidligereTrukneTrekkvedtakIMaaneden: Set<TrekkvedtakId>,
    override val relaterteBeregninger: List<Beregning> = emptyList(),
    kildemottaker: AktoerId = utbetalesTil ?: gjelder,
) : Delperiodegrunnlag {
    val oppdrag: Tidslinjesamling<UUID, Opplysninger> = oppdrag.avgrensTil(periode)
    override val ekstraInfo: MutableList<EkstraInfo> = ekstraInfo.toMutableList()

    init {
        val kontekst = "$oppdragsnokkel $periode"
        require(periode.tom?.let { !it.isBefore(periode.fom) } == true) { "$kontekst: Expected a finite calculation interval" }
        val identifisertOppdrag = oppslagMedKontekst(kontekst) { identitet.oppdragId(oppdragsnokkel) }
        require(identifisertOppdrag == oppdragId) { "$kontekst: Conflicting assignment identity" }
        require(oppdragsnokkel.fagomrade.fagområde == fagomraade.name) { "$kontekst: Conflicting resolved benefit area" }
        this.oppdrag.tidslinjer.values.forEach { tidslinje ->
            tidslinje.forEach { (_, verdi) ->
                require(Oppdragsnokkel(verdi.fagområde, verdi.fagsystemId) == oppdragsnokkel) { "$kontekst: Conflicting logical assignment key" }
                require(verdi.sats.opplysning.satsType == YtelseSatstype.DAG.kode) { "$kontekst: Unsupported rate type: ${verdi.sats.opplysning.satsType}" }
                require(verdi.status.opplysning == Status.Aktiv) { "$kontekst: Unsupported source status: ${verdi.status.opplysning}" }
                val klassifisering = oppslagMedKontekst(kontekst) { klassekoder.finn(verdi.ytelseKlassekode.opplysning) }
                require(klassifisering.kode.kode == verdi.ytelseKlassekode.opplysning.kode) { "$kontekst: Conflicting resolved classification" }
                val person = requireNotNull(verdi.gjelderId) { "$kontekst: Missing source actor" }
                val identifisertPerson = oppslagMedKontekst(kontekst) { identitet.gjelderId(person.opplysning) }
                require(identifisertPerson == gjelder) { "$kontekst: Conflicting resolved actor" }
                val mottaker = verdi.utbetalesTil?.let { mottaker -> oppslagMedKontekst(kontekst) { identitet.mottakerId(mottaker.opplysning) } } ?: gjelder
                require(mottaker == kildemottaker) { "$kontekst: Conflicting resolved recipient" }
            }
        }
    }

    private inline fun <T> oppslagMedKontekst(
        kontekst: String,
        oppslag: () -> T,
    ): T =
        try {
            oppslag()
        } catch (feil: NoSuchElementException) {
            throw IllegalArgumentException("$kontekst: ${feil.message}", feil)
        }

    override fun forDag(dato: LocalDate): BigDecimal = if (dato.isWeekday()) oppdrag[dato].values.sumOf { it.sats.opplysning.verdi } else BigDecimal.ZERO

    override fun vedtakSatsForDag(dato: LocalDate): BigDecimal = if (dato.isWeekday()) oppdrag[dato].values.sumOf { it.vedtaksSats?.opplysning?.verdi ?: BigDecimal.ZERO } else BigDecimal.ZERO

    override fun trekkgrunnlag(periode: DatoIntervall): BigDecimal =
        periode.hverdager.sumOf { dato ->
            oppdrag[dato]
                .values
                .filter { klassekoder.finn(it.ytelseKlassekode.opplysning).grunnlagTrekk }
                .sumOf { it.sats.opplysning.verdi }
        }
}
