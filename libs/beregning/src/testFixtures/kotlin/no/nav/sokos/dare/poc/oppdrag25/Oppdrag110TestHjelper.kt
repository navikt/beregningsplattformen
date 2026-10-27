package no.nav.sokos.dare.poc.oppdrag25

import no.nav.sokos.dare.poc.avvent.Avvent
import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.oppdrag.OppdragId

private val KANSELLERINGS_VERDIER = setOf("j", "ja", "y", "yes")

fun Oppdrag110.tilAvventPerioder(oppdragId: OppdragId = OppdragId(0)): List<Avvent> =
    avvent118
        .filter { it.feilreg?.trim()?.lowercase() !in KANSELLERINGS_VERDIER }
        .map { avvent ->
            Avvent(
                oppdragId = oppdragId,
                periode = DatoIntervall(avvent.datoAvventFom!!, avvent.datoAvventTom!!),
                kodeArsak = avvent.kodeArsak!!.uppercase(),
                datoOverfores = avvent.datoOverfores,
                bruker = saksbehId,
            )
        }
