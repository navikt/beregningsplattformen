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

    fun bostedsenheterFra(oppdrag110: Oppdrag110) = enheterFra(oppdrag110, typeEnhet = BOSTEDSENHET, ::Bostedsenhet)

    fun behandlendeEnheterFra(oppdrag110: Oppdrag110) = enheterFra(oppdrag110, typeEnhet = BEHANDLENDE_ENHET, ::BehandlendeEnhet)

    fun bostedsenheterFra(
        linjer: Linjer,
        bostedsenheterFraOppdrag: Tidslinje<Opplysning<Bostedsenhet>>,
    ) = enheterFra(linjer, typeEnhet = BOSTEDSENHET, ::Bostedsenhet).kombinerMed(bostedsenheterFraOppdrag) { _, bostedsenhetFraLinje, bostedsenhetFraOppdrag ->
        bostedsenhetFraLinje ?: bostedsenhetFraOppdrag
    }

    fun behandlendeEnheterFra(
        linjer: Linjer,
        behandlendeEnheterFraOppdrag: Tidslinje<Opplysning<BehandlendeEnhet>>,
    ) = enheterFra(linjer, typeEnhet = BEHANDLENDE_ENHET, ::BehandlendeEnhet).kombinerMed(behandlendeEnheterFraOppdrag) { _, behandlendeEnhetFraLinje, behandlendeEnhetFraOppdrag ->
        behandlendeEnhetFraLinje ?: behandlendeEnhetFraOppdrag
    }

    fun <T> enheterFra(
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

    private fun <T> enheterFra(
        linjer: Linjer,
        typeEnhet: String,
        tilEnhet: (enhetsnummer: String) -> T,
    ): Tidslinje<Opplysning<T>> {
        val builder = Tidslinje.Builder<Opplysning<T>>()
        linjer.alle.forEach { linje ->
            val saksbehandlerId = SaksbehandlerId(linje.saksbehId)
            linje.linjeEnhet160.filter { it.typeEnhet == typeEnhet }.sortedBy { it.datoEnhetFom }.forEach { linjeEnhet160 ->
                val enhetsnummer = linjeEnhet160.enhet ?: error("Mangler enhet på linjenivå") // TODO: Sjekk med Steinar
                builder.periode(
                    periode = Periode.ÅpenPeriode(linjeEnhet160.datoEnhetFom),
                    verdi = Opplysning(saksbehandlerId, tilEnhet(enhetsnummer)),
                )
            }
        }
        return builder.build()
    }
}
