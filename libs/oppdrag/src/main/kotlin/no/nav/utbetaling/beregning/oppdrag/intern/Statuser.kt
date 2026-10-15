package no.nav.utbetaling.beregning.oppdrag.intern

import java.time.LocalDate.EPOCH

import no.nav.utbetaling.beregning.oppdrag.DelytelseId
import no.nav.utbetaling.beregning.oppdrag.Opplysning
import no.nav.utbetaling.beregning.oppdrag.SaksbehandlerId
import no.nav.utbetaling.beregning.oppdrag.Status
import no.nav.utbetaling.beregning.oppdrag.intern.Linjer.Companion.delytelseId
import no.nav.utbetaling.beregning.oppdrag.xml.Oppdrag110
import no.nav.utbetaling.beregning.tid.Periode.ÅpenPeriode
import no.nav.utbetaling.beregning.tid.Tidslinje

internal data class Statusendring(
    val status: Opplysning<Status>,
    val unntatt: Set<DelytelseId>,
)

internal object Statuser {
    internal fun fra(oppdrag110: Oppdrag110): Tidslinje<Statusendring> {
        val builder = Tidslinje.Builder<Statusendring>()
        val kodeStatus = oppdrag110.kodeStatus ?: return builder.build()
        val status = oppdragstatuser[kodeStatus.name] ?: error("Forventet ikke å få status ${kodeStatus.name} på oppdraget")
        val saksbehandlerId = oppdrag110.saksbehId.mapVerdi { SaksbehandlerId(it) } ?: error("Mangler saksbehId på oppdragsnivå")
        val unntatt = oppdrag110.oppdragsLinje150.map { it.delytelseId() }.toSet()

        return builder
            .periode(
                periode = ÅpenPeriode(oppdrag110.datoStatusFom ?: EPOCH),
                verdi = Statusendring(Opplysning(saksbehandlerId, status), unntatt),
            ).build()
    }

    internal fun fra(linjer: Linjer): Tidslinje<Statusendring> {
        val (linjerMedStatus, linjerUtenStatus) = linjer.alle.partition { it.kodeStatusLinje != null }

        val builder = Tidslinje.Builder<Statusendring>()
        if (linjerMedStatus.isEmpty()) return builder.build()

        val unntatt = linjerUtenStatus.map { it.delytelseId() }.toSet()

        linjerMedStatus
            .groupBy { (it.datoStatusFom ?: EPOCH) }
            .mapValues { (fom, linjer) ->
                require(linjer.size == 1) { "Det finnes ${linjer.size} linjer med kodeStatusLinje satt fra og med $fom" }
                val linje = linjer.single()
                val kodeStatus = linje.kodeStatusLinje!!
                val status = linjestatuser[kodeStatus.name] ?: error("Forventet ikke å få ${kodeStatus.name} på linjenivå")
                val saksbehandlerId = linje.saksbehId.mapVerdi { SaksbehandlerId(it) } ?: error("Mangler saksbehId på linjenivå")
                Opplysning(saksbehandlerId, status)
            }.toSortedMap()
            .forEach { (fom, opplysning) ->
                builder.periode(
                    periode = ÅpenPeriode(fom),
                    verdi = Statusendring(opplysning, unntatt),
                )
            }

        return builder.build()
    }

    /**
     Oppdragssystemet opererer med følgende statuser:

     Sendes inn fra fagrutinen på linjenivå
     HVIL (hvilende) - settes av fagrutinen,medfører at oppdrag/oppdragslinje ikke tas med ved beregning
     SPER (sperret) - settes av fagrutinen,medfører at oppdrag/oppdragslinje ikke tas med ved beregning
     OPPH (opphørt) - settes av fagrutinen, medfører at oppdrag/oppdragslinje opphører/annulleres
     REAK (reaktivert)- settes av fagrutinen når en opphørt/hvilende/sperret oppdagslinje gjenåpnes (ikke tillatt på oppdragsnivå)  (endres automatisk til LOPE ved første beregning)

     Sendes inn fra fagrutinen på oppdragnivå
     OPNY (opphør og Ny(e) linje(r) - settes av fagrutinen, medfører at eksisterende aktive linjer opphører fra status-dato. Denne statuskoden krever at det medfølger minimum èn ID150 med Kode-endring = 'NY'.
     HVIL (hvilende) - settes av fagrutinen,medfører at oppdrag/oppdragslinje ikke tas med ved beregning
     SPER (sperret) - settes av fagrutinen,medfører at oppdrag/oppdragslinje ikke tas med ved beregning
     OPPH (opphørt) - settes av fagrutinen, medfører at oppdrag/oppdragslinje opphører/annulleres

     Settes internt i oppdrag
     NY- settes automatisk når nye oppdrag/oppragslinjer legges inn. (Endres automatisk til LOPE ved første beregning)
     LOPE (løpende) - settes automatisk ved første beregning/utbetaling
     FBER (ferdig beregnet)- settes automatisk når vedtak tomdato nås i beregning, eller andre kriterier for ferdigberegning er nådd
     IKAT (ikke attestert) - settes automatisk når gyldig attestasjon utløper/opphører
     ATTE (attestert)- settes automatisk når oppdragslinje får ny/gyldig attestasjon (endres automatisk til LOPE ved første beregning)
     ANNU (annullert)- settes automatisk når fagrutinene overfører status OPPH på oppdrag/oppdragslinje hvor det ikke er foretatt beregning. Settes også når ny oppdragslinje korrigerer tidligere linje fra  samme eller tidligere dato (ref.felter utfylt)
     KORR (korrigert)- settes automatisk når ny oppdragslinje korrigerer en annen oppdragslinje med virkning fra en senere dato (ref.felter utfylt)
     FEIL (feil ved berening)- settes automatisk når det oppstår en feilsituasjon under beregning av oppdraget
     */

    private val linjestatuser =
        mapOf(
            "HVIL" to Status.Hvilende,
            "SPER" to Status.Sperret,
            "OPPH" to Status.Opphørt,
            // Gir ikke mening å ha Reaktivert som status, det er mer en beskjed fra fagrutinen om å gjøre linjen(e) aktive igjen
            "REAK" to Status.Aktiv,
        )

    private val oppdragstatuser =
        mapOf(
            // Sidestilles med Opphørt. Uavhengig av status lar vi ikke nye linjer påvirkes av oppdragstatusen
            "OPNY" to Status.Opphørt,
            "OPPH" to Status.Opphørt,
            "HVIL" to Status.Hvilende,
            "SPER" to Status.Sperret,
        )
}
