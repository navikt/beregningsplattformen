package no.nav.sokos.dare.poc.oppdrag

import java.time.LocalDate

import no.nav.sokos.dare.poc.oppdrag25.TkodeStatusLinje

class OppdragEndringsmotor(
    private val overlapPolicy: OppdragslinjeOverlapPolicy,
) {
    fun planlegg(
        eksisterendeLinjer: List<Oppdragslinje>,
        endringer: List<OppdragslinjeEndring>,
    ): OppdragEndringsplan {
        val grupperte = grupperLinjer(endringer)
        validerReferanseorden(grupperte.nye)
        val tilstand = PlanleggingsTilstand(eksisterendeLinjer)

        grupperte.nye.forEach(tilstand::leggTil)
        grupperte.oppdaterte.forEach(tilstand::oppdater)
        tilstand.validerIngenOverlapp()
        grupperte.oppdaterte.forEach(tilstand::behandleInaktivStatus)

        return tilstand.tilPlan()
    }

    private inner class PlanleggingsTilstand(
        eksisterendeLinjer: List<Oppdragslinje>,
    ) {
        private val linjePerDelytelseId = eksisterendeLinjer.associateBy(Oppdragslinje::delytelseId).toMutableMap()
        private val kjedePerDelytelseId: MutableMap<String, KjedeNokkel> =
            eksisterendeLinjer
                .associate { linje ->
                    linje.delytelseId to KjedeNokkel.Eksisterende(requireNotNull(linje.oppdragslinjekjedeId))
                }.toMutableMap()
        private val linjerPerKjede: MutableMap<KjedeNokkel, MutableList<Oppdragslinje>> =
            eksisterendeLinjer
                .groupBy { linje -> kjedePerDelytelseId.getValue(linje.delytelseId) }
                .mapValues { (_, kjedelinjer) -> kjedelinjer.sortertEtterPeriode().toMutableList() }
                .toMutableMap()
        private val mutasjoner = mutableListOf<OppdragslinjeMutasjon>()

        fun leggTil(endring: OppdragslinjeEndring) {
            val nyLinje = endring.linje
            val referertLinje = endring.refDelytelseId?.let(linjePerDelytelseId::get)
            val kjede =
                referertLinje
                    ?.let { linje ->
                        kjedePerDelytelseId[linje.delytelseId]
                            ?: throw IllegalArgumentException(
                                "Fant ikke oppdragslinjekjede for refererte linje ${linje.delytelseId}",
                            )
                    }
                    ?: KjedeNokkel.Ny(nyLinje.delytelseId)
            val kjedelinjer = linjerPerKjede.getOrPut(kjede) { mutableListOf() }
            val overlap = overlapPolicy.vurder(nyLinje, finnRefererteLinjer(referertLinje, kjedelinjer))

            mutasjoner += OppdragslinjeMutasjon.Opprett(nyLinje, endring.refDelytelseId)
            overlap.slettes.forEach(::slett)
            overlap.avkortes?.let { avkort(it, requireNotNull(it.datoVedtakTom)) }

            linjePerDelytelseId[nyLinje.delytelseId] = nyLinje
            kjedePerDelytelseId[nyLinje.delytelseId] = kjede
            kjedelinjer += nyLinje
            kjedelinjer.sorterEtterPeriode()
        }

        fun oppdater(endring: OppdragslinjeEndring) {
            val eksisterende =
                linjePerDelytelseId[endring.linje.delytelseId]
                    ?: throw IllegalArgumentException("Fant ikke oppdragslinje med delytelseId ${endring.linje.delytelseId}")
            val oppdatert =
                sjekkStatusendring(
                    endring.linje.copy(
                        id = eksisterende.id,
                        oppdragslinjekjedeId = eksisterende.oppdragslinjekjedeId,
                    ),
                    eksisterende,
                    endring.linjestatus,
                    endring.datoStatusFom,
                )
            oppdater(oppdatert)
        }

        fun oppdater(linje: Oppdragslinje) {
            mutasjoner += OppdragslinjeMutasjon.Oppdater(linje)
            linjePerDelytelseId[linje.delytelseId] = linje
            kjedelinjerFor(linje.delytelseId).erstattIKjede(linje)
        }

        fun validerIngenOverlapp() {
            require(linjerPerKjede.values.none { it.harOverlappendePerioder() }) {
                "En oppdragslinjekjede kan ikke persisteres med overlappende perioder"
            }
        }

        fun behandleInaktivStatus(endring: OppdragslinjeEndring) {
            if (endring.linjestatus !in inaktiveLinjeStatuser) {
                return
            }

            val statusFom =
                requireNotNull(endring.datoStatusFom) {
                    "datoStatusFom må være satt ved statusendring"
                }
            val kjedelinjer = kjedelinjerFor(endring.linje.delytelseId)
            require(!statusFom.isBefore(kjedelinjer.first().datoVedtakFom)) {
                "Statusendringsdato kan kan ikke settes før linjekjedens startdato"
            }

            val yngreLinjer = kjedelinjer.filter { it.datoVedtakFom > statusFom && !it.annullert }
            yngreLinjer.forEach { oppdater(it.copy(annullert = true)) }

            val avkortetLinje = kjedelinjer.first { it.periode.contains(statusFom) }
            avkort(avkortetLinje, statusFom)
        }

        fun tilPlan(): OppdragEndringsplan = OppdragEndringsplan(mutasjoner)

        private fun slett(linje: Oppdragslinje) {
            mutasjoner += OppdragslinjeMutasjon.Slett(linje.delytelseId)
            linjePerDelytelseId.remove(linje.delytelseId)
            val kjede = kjedePerDelytelseId.remove(linje.delytelseId)
            kjede?.let { linjerPerKjede.getValue(it).remove(linje) }
        }

        private fun avkort(
            linje: Oppdragslinje,
            datoVedtakTom: LocalDate,
        ) {
            mutasjoner += OppdragslinjeMutasjon.Avkort(linje.delytelseId, datoVedtakTom)
            linjePerDelytelseId[linje.delytelseId] = linje
            kjedelinjerFor(linje.delytelseId).erstattIKjede(linje)
        }

        private fun kjedelinjerFor(delytelseId: String): MutableList<Oppdragslinje> {
            val kjede = kjedePerDelytelseId.getValue(delytelseId)
            return linjerPerKjede.getValue(kjede)
        }

        private fun finnRefererteLinjer(
            referertLinje: Oppdragslinje?,
            kjedelinjer: List<Oppdragslinje>,
        ): List<Oppdragslinje> {
            if (referertLinje == null) {
                return emptyList()
            }

            val referertIndeks = kjedelinjer.indexOfFirst { it.delytelseId == referertLinje.delytelseId }
            check(referertIndeks >= 0) {
                "Oppdragslinje ${referertLinje.delytelseId} mangler i oppdragslinjekjeden"
            }
            return kjedelinjer.take(referertIndeks + 1).asReversed()
        }

        private fun MutableList<Oppdragslinje>.erstattIKjede(linje: Oppdragslinje) {
            val index = indexOfFirst { it.delytelseId == linje.delytelseId }
            check(index >= 0) {
                "Oppdragslinje ${linje.delytelseId} mangler i oppdragslinjekjeden"
            }
            this[index] = linje
            sorterEtterPeriode()
        }

        private fun MutableList<Oppdragslinje>.sorterEtterPeriode() {
            val sorterte = sortertEtterPeriode()
            clear()
            addAll(sorterte)
        }
    }

    private fun sjekkStatusendring(
        endretLinje: Oppdragslinje,
        eksisterendeLinje: Oppdragslinje,
        linjestatus: TkodeStatusLinje?,
        datoStatusFom: LocalDate?,
    ): Oppdragslinje {
        if (linjestatus == null) return endretLinje

        val statusFom = requireNotNull(datoStatusFom) { "datoStatusFom må være satt ved statusendring" }
        return when (linjestatus) {
            TkodeStatusLinje.HVIL,
            TkodeStatusLinje.SPER,
            TkodeStatusLinje.OPPH,
            -> {
                require(endretLinje.datoVedtakTom == null || !endretLinje.datoVedtakTom.isBefore(statusFom)) {
                    "linjens datoVedtakTom kan ikke være før datoStatusFom ved statusendring"
                }
                if (statusFom.isBefore(eksisterendeLinje.datoVedtakFom)) {
                    endretLinje.copy(annullert = true)
                } else {
                    endretLinje.copy(datoVedtakTom = statusFom)
                }
            }

            // TODO: Hvordan ser informasjonselementene til en reaktivert linje ut?
            // TODO: Implementasjon på håndtering av statusendring REAKT
            TkodeStatusLinje.REAK -> {
                require(endretLinje.datoVedtakTom == null || !endretLinje.datoVedtakTom.isBefore(statusFom)) {
                    "datoVedtakTom kan ikke være før datoStatusFom ved statusendring"
                }
                endretLinje
            }
        }
    }

    private fun grupperLinjer(endringer: List<OppdragslinjeEndring>): GroupedOppdragslinjeChanges {
        val nye = endringer.filter { it.endringskode == OppdragslinjeEndringskode.NY }
        val oppdaterte = endringer.filter { it.endringskode == OppdragslinjeEndringskode.ENDR }
        return GroupedOppdragslinjeChanges(nye, oppdaterte)
    }

    private fun validerReferanseorden(nyeLinjer: List<OppdragslinjeEndring>) {
        val indeksPerDelytelseId =
            nyeLinjer
                .mapIndexed { index, endring -> endring.linje.delytelseId to index }
                .toMap()

        nyeLinjer.forEachIndexed { index, endring ->
            val referertDelytelseId = endring.refDelytelseId ?: return@forEachIndexed
            val referertIndeks = indeksPerDelytelseId[referertDelytelseId] ?: return@forEachIndexed

            require(referertIndeks < index) {
                "Oppdragslinje ${endring.linje.delytelseId} refererer til $referertDelytelseId, " +
                    "som må mottas tidligere i samme melding"
            }
        }
    }

    private sealed interface KjedeNokkel {
        data class Eksisterende(
            val id: OppdragslinjekjedeId,
        ) : KjedeNokkel

        data class Ny(
            val rotDelytelseId: String,
        ) : KjedeNokkel
    }
}

data class OppdragEndringsplan(
    val mutasjoner: List<OppdragslinjeMutasjon>,
)

sealed interface OppdragslinjeMutasjon {
    data class Opprett(
        val linje: Oppdragslinje,
        val refDelytelseId: String?,
    ) : OppdragslinjeMutasjon

    data class Avkort(
        val delytelseId: String,
        val datoVedtakTom: java.time.LocalDate,
    ) : OppdragslinjeMutasjon

    data class Slett(
        val delytelseId: String,
    ) : OppdragslinjeMutasjon

    data class Oppdater(
        val linje: Oppdragslinje,
    ) : OppdragslinjeMutasjon
}

data class GroupedOppdragslinjeChanges(
    val nye: List<OppdragslinjeEndring>,
    val oppdaterte: List<OppdragslinjeEndring>,
)
