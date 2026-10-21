package no.nav.utbetaling.beregning.oppdrag.intern

import no.nav.utbetaling.beregning.oppdrag.DelytelseId
import no.nav.utbetaling.beregning.oppdrag.Fagområde
import no.nav.utbetaling.beregning.oppdrag.FagsystemId
import no.nav.utbetaling.beregning.oppdrag.intern.Kjede.Companion.grupperPåKjede
import no.nav.utbetaling.beregning.oppdrag.xml.Oppdrag110
import no.nav.utbetaling.beregning.oppdrag.xml.OppdragsLinje150
import no.nav.utbetaling.beregning.tid.Periode.ÅpenPeriode
import no.nav.utbetaling.beregning.tid.Tidslinje
import no.nav.utbetaling.beregning.tid.forrigeDag
import no.nav.utbetaling.beregning.tid.til

internal data class Linjer(
    val aktive: Tidslinje<OppdragsLinje150>,
    val alle: List<OppdragsLinje150>,
) {
    companion object {
        internal fun fra(oppdrag110: Oppdrag110): Map<Kjede, Linjer> {
            val fagområde = oppdrag110.kodeFagomraade.mapVerdi { Fagområde(it) } ?: error("Mangler kodeFagomraade")
            val fagsystemId = oppdrag110.fagsystemId.mapVerdi { FagsystemId(it) } ?: error("Mangler fagsystemId")
            val gruppertPåKjede =
                oppdrag110.oppdragsLinje150
                    .map { linje ->
                        Kjede(fagområde = fagområde, fagsystemId = fagsystemId, delytelseIder = linje.delytelseIder()) to linje
                    }.grupperPåKjede()

            return gruppertPåKjede.mapValues { (_, linjer) ->
                require(linjer.map { it.delytelseId() }.toSet().size == linjer.size) { "DelytelseId må være unik på tvers av linjene" }
                val pekere = linjer.mapNotNull { it.refDelytelseId }
                require(pekere.toSet().size == pekere.size) { "Flere linjer kan ikke peke på samme linje" }
                require(pekere.size == linjer.size || pekere.size == linjer.size - 1) { "Denne kjeden kan umulig gi mening!" }
                Linjer(
                    aktive = linjer.aktiveLinjer(),
                    alle = linjer,
                )
            }
        }

        private fun List<OppdragsLinje150>.sortertLinjerNyestFørst(): List<OppdragsLinje150> {
            if (size <= 1) return this

            val linjePerRefDelytelseId = associateBy { it.refDelytelseId() }
            val eldste =
                singleOrNull {
                    it.refDelytelseId == null
                } ?: first() // TODO: Trenger man en smarte måte å finne eldste linje annet enn å stole pårekkefølge?
            val eldsteFørst = mutableListOf(eldste)

            var forrige: OppdragsLinje150 = eldste

            while (eldsteFørst.size != this.size) {
                val nestEldste =
                    linjePerRefDelytelseId[forrige.delytelseId()]
                        ?: error("Fant ikke neste linje i kjeden")
                eldsteFørst.add(nestEldste)
                forrige = nestEldste
            }

            return eldsteFørst.reversed()
        }

        private fun List<OppdragsLinje150>.aktiveLinjer(): Tidslinje<OppdragsLinje150> {
            val nyesteFørst = sortertLinjerNyestFørst()
            return nyesteFørst.fold(Tidslinje.Builder<OppdragsLinje150>().build()) { sammenslått, linje ->
                val aktivDel =
                    when (
                        val begrensetTom =
                            sammenslått
                                .firstOrNull()
                                ?.first
                                ?.fom
                                ?.forrigeDag
                    ) {
                        null -> linje.somTidslinje()
                        else -> linje.somTidslinje().begrensetTil(begrensetTom)
                    }
                sammenslått.kombinerMed(aktivDel) { _, eksisterende, ny -> ny ?: eksisterende }
            }
        }

        private fun OppdragsLinje150.somTidslinje(): Tidslinje<OppdragsLinje150> {
            val periode =
                when (val tom = datoVedtakTom) {
                    null -> ÅpenPeriode(datoVedtakFom)
                    else -> datoVedtakFom til tom
                }
            return Tidslinje.Builder<OppdragsLinje150>().periode(periode, this).build()
        }

        private fun OppdragsLinje150.refDelytelseId() = refDelytelseId.mapVerdi { DelytelseId(it) }

        private fun OppdragsLinje150.delytelseIder() = listOfNotNull(delytelseId(), refDelytelseId()).toSet()

        fun OppdragsLinje150.delytelseId() = delytelseId.mapVerdi { DelytelseId(it) } ?: error("delytelseId må være satt!")
    }
}
