package no.nav.utbetaling.beregningsflyt.testhjelpere

import java.math.BigDecimal

import no.nav.utbetaling.beregning.oppdrag.Avventingsårsak
import no.nav.utbetaling.beregning.oppdrag.BrukKjøreplan
import no.nav.utbetaling.beregning.oppdrag.DelytelseId
import no.nav.utbetaling.beregning.oppdrag.Fagområde
import no.nav.utbetaling.beregning.oppdrag.FagsystemId
import no.nav.utbetaling.beregning.oppdrag.Kidnummer
import no.nav.utbetaling.beregning.oppdrag.Opplysning
import no.nav.utbetaling.beregning.oppdrag.Opplysninger
import no.nav.utbetaling.beregning.oppdrag.Personidentifikator
import no.nav.utbetaling.beregning.oppdrag.SaksbehandlerId
import no.nav.utbetaling.beregning.oppdrag.Sats
import no.nav.utbetaling.beregning.oppdrag.Status
import no.nav.utbetaling.beregning.oppdrag.UtbetalesTil
import no.nav.utbetaling.beregning.oppdrag.YtelseKlassekode
import no.nav.utbetaling.beregning.tid.DefaultTidslinjesamling
import no.nav.utbetaling.beregning.tid.Periode
import no.nav.utbetaling.beregning.tid.Tidslinje

private val saksbehandler = SaksbehandlerId("Z999999")

private fun <T> opplysning(verdi: T) = Opplysning(saksbehandler, verdi)

fun opplysninger(
    fagområde: String = "TEST",
    fagsystemId: String = "fagsystem-1",
    delytelseId: String = "delytelse-1",
    utbetalesTil: UtbetalesTil? = null,
    kidnummer: String? = null,
    avventingsårsak: Avventingsårsak? = null,
) = Opplysninger(
    fagområde = Fagområde(fagområde),
    fagsystemId = FagsystemId(fagsystemId),
    delytelseId = DelytelseId(delytelseId),
    refDelytelseId = null,
    gjelder = opplysning(Personidentifikator("12345678910")),
    utbetalesTil = utbetalesTil?.let(::opplysning),
    sats = opplysning(Sats(BigDecimal("1000"), "MND")),
    vedtakssats = null,
    skyldner = null,
    refunderesTil = null,
    maksdato = null,
    søknadstype = null,
    henvisning = null,
    stønadId = null,
    forfall = null,
    utbetalingsfrekvens = null,
    bilagstype = null,
    avventingsårsak = avventingsårsak?.let(::opplysning),
    kidnummer = kidnummer?.let { opplysning(Kidnummer(it)) },
    status = opplysning(Status.Aktiv),
    bostedsenhet = null,
    behandlendeEnhet = null,
    vedtakId = null,
    brukKjøreplan = opplysning(BrukKjøreplan.Ja),
    ompostering = null,
    ytelseKlassekode = opplysning(YtelseKlassekode("KLASSE")),
)

fun oppdragstidslinje(vararg linjer: Pair<Periode, Opplysninger>) =
    DefaultTidslinjesamling(
        linjer.withIndex().associate { (indeks, linje) ->
            indeks to Tidslinje.Builder<Opplysninger>().periode(linje.first, linje.second).build()
        },
    )
