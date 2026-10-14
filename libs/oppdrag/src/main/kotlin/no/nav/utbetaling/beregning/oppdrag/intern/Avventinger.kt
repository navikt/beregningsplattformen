package no.nav.utbetaling.beregning.oppdrag.intern

import no.nav.utbetaling.beregning.oppdrag.Avventingsårsak
import no.nav.utbetaling.beregning.oppdrag.Opplysning
import no.nav.utbetaling.beregning.oppdrag.SaksbehandlerId
import no.nav.utbetaling.beregning.oppdrag.xml.Oppdrag110
import no.nav.utbetaling.beregning.tid.Periode.LukketPeriode.Companion.til
import no.nav.utbetaling.beregning.tid.Tidslinje

internal object Avventinger {
    fun fra(oppdrag110: Oppdrag110): Tidslinje<Endring<Avventingsårsak>> {
        val builder = Tidslinje.Builder<Endring<Avventingsårsak>>()
        val saksbehandlerId = oppdrag110.saksbehId.mapVerdi { SaksbehandlerId(it) } ?: error("Mangler saksbehandler på oppdragNivå")
        oppdrag110.avvent118.forEach { avvent118 ->
            val fom = checkNotNull(avvent118.datoAvventFom) { "datoAvventFom må være satt på avventing" }
            val tom = checkNotNull(avvent118.datoAvventTom) { "datoAvventTom må være satt på avventing" }
            val årsak =
                when (avvent118.kodeArsak?.uppercase()) {
                    "AVAV" ->
                        Avventingsårsak.Avregning(
                            til =
                                checkNotNull(avvent118.datoOverfores) {
                                    "datoOverfores må være satt for avventing pga. avregning"
                                },
                        )
                    "AVRK" -> Avventingsårsak.Refusjonskrav(avvent118.datoOverfores)
                    else -> error("Kjenner ikke til avventingsårsak ${avvent118.kodeArsak}")
                }
            builder.periode(fom til tom, Endring.fra(avvent118.feilreg, Opplysning(saksbehandlerId, årsak)))
        }
        return builder.build()
    }
}
