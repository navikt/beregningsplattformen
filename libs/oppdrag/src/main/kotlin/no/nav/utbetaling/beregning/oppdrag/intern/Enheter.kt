package no.nav.utbetaling.beregning.oppdrag.intern

import no.nav.utbetaling.beregning.oppdrag.BehandlendeEnhet
import no.nav.utbetaling.beregning.oppdrag.Bostedsenhet
import no.nav.utbetaling.beregning.oppdrag.Opplysning
import no.nav.utbetaling.beregning.oppdrag.SaksbehandlerId
import no.nav.utbetaling.beregning.oppdrag.xml.Oppdrag110
import no.nav.utbetaling.beregning.tid.Periode
import no.nav.utbetaling.beregning.tid.Tidslinje

internal object Enheter {
    private const val BOSTEDSENHET = "BOS"
    private const val BEHANDLENDE_ENHET = "BEH"

    fun bostedsenheterFra(oppdrag110: Oppdrag110): Tidslinje<Bostedsenhet> {
        val builder = Tidslinje.Builder<Bostedsenhet>()
        oppdrag110.oppdragsEnhet120.filter { it.typeEnhet == BOSTEDSENHET }.sortedBy { it.datoEnhetFom }.forEach { enhetOppdrag110 ->
            val enhetsnummer = enhetOppdrag110.enhet ?: error("Mangler enhet på oppdragsnivå")
            builder.periode(
                periode = Periode.ÅpenPeriode(enhetOppdrag110.datoEnhetFom),
                verdi = Bostedsenhet(enhetsnummer),
            )
        }
        return builder.build().validerEnheter(oppdrag110, BOSTEDSENHET)
    }

    fun behandlendeEnheterFra(oppdrag110: Oppdrag110): Tidslinje<FeilregistrerbarOpplysning<BehandlendeEnhet>> {
        val builder = Tidslinje.Builder<FeilregistrerbarOpplysning<BehandlendeEnhet>>()
        val saksbehandlerId = SaksbehandlerId(oppdrag110.saksbehId)
        oppdrag110.oppdragsEnhet120.filter { it.typeEnhet == BEHANDLENDE_ENHET }.sortedBy { it.datoEnhetFom }.forEach { enhetOppdrag110 ->
            val opplysning =
                when (val enhetsnummer = enhetOppdrag110.enhet) {
                    null -> FeilregistrerbarOpplysning.Feilregistrert
                    else -> FeilregistrerbarOpplysning.Registrert(Opplysning(saksbehandlerId, BehandlendeEnhet(enhetsnummer)))
                }
            builder.periode(
                periode = Periode.ÅpenPeriode(enhetOppdrag110.datoEnhetFom),
                verdi = opplysning,
            )
        }
        return builder.build().validerEnheter(oppdrag110, BEHANDLENDE_ENHET)
    }

    private fun <T> Tidslinje<T>.validerEnheter(
        oppdrag110: Oppdrag110,
        enhetstype: String,
    ): Tidslinje<T> {
        if (oppdrag110.oppdragsLinje150.any { it.datoVedtakTom == null }) return this
        val linjerOppgittTom = oppdrag110.oppdragsLinje150.maxOf { it.datoVedtakTom!! }
        forEach { (endretEnhetPeriode, _) ->
            require(endretEnhetPeriode.fom <= linjerOppgittTom) {
                "Oppgitt $enhetstype-enhet fra og med $endretEnhetPeriode, men siste linje slutter $linjerOppgittTom."
            }
        }
        return this
    }
}
