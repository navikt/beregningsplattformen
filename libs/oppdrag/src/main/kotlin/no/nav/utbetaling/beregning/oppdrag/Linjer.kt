package no.nav.utbetaling.beregning.oppdrag

import no.nav.utbetaling.beregning.oppdrag.Kjedbar.Companion.grupperPåKjede
import no.nav.utbetaling.beregning.oppdrag.xml.Oppdrag110
import no.nav.utbetaling.beregning.oppdrag.xml.OppdragsLinje150
import no.nav.utbetaling.beregning.tid.Periode.LukketPeriode.Companion.forrigeDag
import no.nav.utbetaling.beregning.tid.Periode.LukketPeriode.Companion.til
import no.nav.utbetaling.beregning.tid.Periode.ÅpenPeriode
import no.nav.utbetaling.beregning.tid.Tidslinje

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
                        val kjede = Kjede(fagområde, fagsystemId, linje.delytelseIder())
                        Kjedbar(kjede, linje)
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

            val eldste = singleOrNull { it.refDelytelseId == null } ?: first() // TODO: Trenger man en smarte måte å finne eldste linje annet enn å stole pårekkefølge?
            val eldsteFørst = mutableListOf(eldste)

            var forrige: OppdragsLinje150 = eldste

            while (eldsteFørst.size != this.size) {
                val nestEldste = singleOrThrow { it.refDelytelseId() == forrige.delytelseId() }
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

        private fun <T> List<T>.singleOrThrow(predicate: (T) -> Boolean) =
            filter(predicate).let {
                check(it.size == 1) { "Fikk ${it.size} som matchet predikatet." }
                it.first()
            }

        private fun OppdragsLinje150.delytelseId() = delytelseId.mapVerdi { DelytelseId(it) } ?: error("delytelseId må være satt!")

        private fun OppdragsLinje150.refDelytelseId() = refDelytelseId.mapVerdi { DelytelseId(it) }

        private fun OppdragsLinje150.delytelseIder() = listOfNotNull(delytelseId(), refDelytelseId()).toSet()
    }
}
