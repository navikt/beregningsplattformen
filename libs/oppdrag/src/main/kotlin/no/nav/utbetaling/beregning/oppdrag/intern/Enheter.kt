package no.nav.utbetaling.beregning.oppdrag.intern

import no.nav.utbetaling.beregning.oppdrag.BehandlendeEnhet
import no.nav.utbetaling.beregning.oppdrag.Bostedsenhet
import no.nav.utbetaling.beregning.oppdrag.Opplysning
import no.nav.utbetaling.beregning.oppdrag.SaksbehandlerId
import no.nav.utbetaling.beregning.oppdrag.xml.Oppdrag110
import no.nav.utbetaling.beregning.tid.Periode
import no.nav.utbetaling.beregning.tid.Tidslinje

internal object Enheter {
    fun bostedsenheterFra(oppdrag110: Oppdrag110): Tidslinje<Opplysning<Bostedsenhet>> {
        val builder = Tidslinje.Builder<Opplysning<Bostedsenhet>>()
        oppdrag110.oppdragsEnhet120.filter { it.typeEnhet == "BOS" }.sortedBy { it.datoEnhetFom }.forEach { enhetOppdrag110 ->
            val saksbehandlerId = SaksbehandlerId(oppdrag110.saksbehId)
            val enhetsnummer = enhetOppdrag110.enhet ?: error("Mangler enhet på oppdragsnivå")
            builder.periode(
                periode = Periode.ÅpenPeriode(enhetOppdrag110.datoEnhetFom),
                verdi = Opplysning(saksbehandlerId, Bostedsenhet(enhetsnummer)),
            )
        }
        return builder.build()
    }

    fun behandlendeEnheterFra(oppdrag110: Oppdrag110): Tidslinje<Opplysning<BehandlendeEnhet>> {
        val builder = Tidslinje.Builder<Opplysning<BehandlendeEnhet>>()
        oppdrag110.oppdragsEnhet120.filter { it.typeEnhet == "BEH" }.sortedBy { it.datoEnhetFom }.forEach { enhetOppdrag110 ->
            val saksbehandlerId = SaksbehandlerId(oppdrag110.saksbehId)
            val enhetsnummer = enhetOppdrag110.enhet ?: error("Mangler enhet på oppdragsnivå")
            builder.periode(
                periode = Periode.ÅpenPeriode(enhetOppdrag110.datoEnhetFom),
                verdi = Opplysning(saksbehandlerId, BehandlendeEnhet(enhetsnummer)),
            )
        }
        return builder.build()
    }
}
