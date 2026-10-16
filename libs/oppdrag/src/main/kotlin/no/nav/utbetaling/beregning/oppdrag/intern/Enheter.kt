package no.nav.utbetaling.beregning.oppdrag.intern

import no.nav.utbetaling.beregning.oppdrag.BehandlendeEnhet
import no.nav.utbetaling.beregning.oppdrag.Bostedsenhet
import no.nav.utbetaling.beregning.oppdrag.Opplysning
import no.nav.utbetaling.beregning.oppdrag.SaksbehandlerId
import no.nav.utbetaling.beregning.oppdrag.xml.Oppdrag110
import no.nav.utbetaling.beregning.tid.Periode
import no.nav.utbetaling.beregning.tid.Tidslinje

internal object Enheter {
    fun bostedsenheterFra(oppdrag110: Oppdrag110) = enheterFra(oppdrag110, typeEnhet = "BOS", ::Bostedsenhet)

    fun behandlendeEnheterFra(oppdrag110: Oppdrag110) = enheterFra(oppdrag110, typeEnhet = "BEH", ::BehandlendeEnhet)

    private fun <T> enheterFra(
        oppdrag110: Oppdrag110,
        typeEnhet: String,
        tilEnhet: (enhetsnummer: String) -> T,
    ): Tidslinje<Opplysning<T>> {
        val builder = Tidslinje.Builder<Opplysning<T>>()
        val saksbehandlerId = SaksbehandlerId(oppdrag110.saksbehId)
        oppdrag110.oppdragsEnhet120.filter { it.typeEnhet == typeEnhet }.sortedBy { it.datoEnhetFom }.forEach { enhetOppdrag110 ->
            val enhetsnummer = enhetOppdrag110.enhet ?: error("Mangler enhet på oppdragsnivå")
            builder.periode(
                periode = Periode.ÅpenPeriode(enhetOppdrag110.datoEnhetFom),
                verdi = Opplysning(saksbehandlerId, tilEnhet(enhetsnummer)),
            )
        }
        return builder.build()
    }
}
