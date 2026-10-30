package no.nav.utbetaling.beregningsflyt

import java.math.BigDecimal
import java.util.UUID

import no.nav.utbetaling.beregning.oppdrag.BrukKjøreplan
import no.nav.utbetaling.beregning.oppdrag.DelytelseId
import no.nav.utbetaling.beregning.oppdrag.Fagområde
import no.nav.utbetaling.beregning.oppdrag.FagsystemId
import no.nav.utbetaling.beregning.oppdrag.Oppdrag
import no.nav.utbetaling.beregning.oppdrag.Oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.Opplysning
import no.nav.utbetaling.beregning.oppdrag.Opplysninger
import no.nav.utbetaling.beregning.oppdrag.SaksbehandlerId
import no.nav.utbetaling.beregning.oppdrag.Sats
import no.nav.utbetaling.beregning.oppdrag.Status
import no.nav.utbetaling.beregning.oppdrag.YtelseKlassekode
import no.nav.utbetaling.beregning.oppdrag.YtelseSatstype
import no.nav.utbetaling.beregning.tid.DefaultTidslinjesamling
import no.nav.utbetaling.beregning.tid.Periode
import no.nav.utbetaling.beregning.tid.Tidslinje

/** Synthetic timelines for unit views only; integration and distributor tests use [lesTidslinje]. */
fun tidslinjeForTest(
    kjedeId: UUID,
    vararg perioder: Pair<Periode, BigDecimal>,
    delytelseId: DelytelseId = DelytelseId("1"),
    vedtakssats: BigDecimal? = null,
): DefaultTidslinjesamling<UUID, Opplysninger> {
    val saksbehandler = SaksbehandlerId("Z999999")
    val tidslinje =
        Tidslinje
            .Builder<Opplysninger>()
            .apply {
                perioder.forEach { (periode, sats) ->
                    periode(
                        periode,
                        Opplysninger(
                            fagområde = Fagområde("AAP"),
                            fagsystemId = FagsystemId("fagsystemId"),
                            delytelseId = delytelseId,
                            refDelytelseId = null,
                            gjelderId = null,
                            utbetalesTil = null,
                            sats = Opplysning(saksbehandler, Sats(sats, YtelseSatstype.DAG.kode)),
                            vedtaksSats = vedtakssats?.let { Opplysning(saksbehandler, Sats(it, "")) },
                            skyldner = null,
                            refusjonsinfo = null,
                            typeSøknad = null,
                            henvisning = null,
                            stønadId = null,
                            forfall = null,
                            utbetalingsfrekvens = null,
                            bilagstype = null,
                            avventingsårsak = null,
                            kidnummer = null,
                            status = Opplysning(saksbehandler, Status.Aktiv),
                            bostedsenhet = null,
                            behandlendeEnhet = null,
                            vedtakId = null,
                            brukKjøreplan = Opplysning(saksbehandler, BrukKjøreplan.Ja),
                            ompostering = null,
                            ytelseKlassekode = Opplysning(saksbehandler, YtelseKlassekode("AAPUAA")),
                        ),
                    )
                }
            }.build()
    return DefaultTidslinjesamling(mapOf(kjedeId to tidslinje))
}

fun lesTidslinje(
    json: String,
    beskrivelse: String? = null,
): Oppdragstidslinje {
    val prefiks = beskrivelse?.let { "$it: " }.orEmpty()
    return when (val oppdrag = Oppdrag.fraJson(json)) {
        is Oppdrag.Utbetaling -> oppdrag.oppdragstidslinje
        is Oppdrag.Simulering -> throw AssertionError("${prefiks}Expected payment, received simulation")
        is Oppdrag.UgyldigOppdrag -> throw AssertionError("${prefiks}Expected payment, received invalid assignment", oppdrag.feil)
        is Oppdrag.UgyldigKombinasjon -> throw AssertionError("${prefiks}Expected payment, received invalid combination", oppdrag.feil)
    }
}
