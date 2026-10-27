package no.nav.sokos.dare.poc.oppdrag

import java.time.LocalDateTime

import no.nav.sokos.dare.poc.aktoer.AktoerId
import no.nav.sokos.dare.poc.oppdrag25.Oppdrag110
import no.nav.sokos.dare.poc.oppdrag25.OppdragsLinje150
import no.nav.sokos.dare.poc.trekk.FagomraadeKode

object OppdragConverter {
    fun Oppdrag110.toOppdrag(
        aktoerId: AktoerId,
        oppdragId: OppdragId? = null,
        registrert: LocalDateTime = LocalDateTime.now(),
    ): Oppdrag =
        Oppdrag(
            id = oppdragId,
            fagsystemId = requireFagsystemId(),
            fagomraade = fagomraadeKode(),
            datoForfall = datoForfall,
            bilagsType = bilagstype113?.bilagsType.orEmpty(),
            bruker = saksbehId,
            tidspktReg = registrert,
            gjelderId = aktoerId,
        )

    fun Oppdrag110.toOppdragenhet(
        oppdragsId: OppdragId,
        registrert: LocalDateTime = LocalDateTime.now(),
    ): List<Oppdragsenhet> =
        oppdragsEnhet120.map { enhet ->
            Oppdragsenhet(
                oppdragsId = oppdragsId,
                enhet = enhet.enhet.orEmpty(),
                typeEnhet = enhet.typeEnhet,
                datoFom = enhet.datoEnhetFom,
                bruker = saksbehId,
                tidspktReg = registrert,
            )
        }

    fun Oppdrag110.toOppdragslinjer(
        ytelseKlasseKodeLookup: Map<YtelseKlasseKodeRef, YtelseKlasseKode> = emptyMap(),
        registrert: LocalDateTime = LocalDateTime.now(),
    ): List<Oppdragslinje> =
        oppdragsLinje150
            .mapIndexed { index, linje ->
                OppdragslinjeEndring(
                    endringskode = OppdragslinjeEndringskode.fromValue(linje.kodeEndringLinje),
                    linjestatus = linje.kodeStatusLinje,
                    datoStatusFom = linje.datoStatusFom,
                    linje =
                        linje.toOppdragslinje(
                            linjeId = index.toShort(),
                            ytelseKlasseKodeLookup = ytelseKlasseKodeLookup,
                            registrert = registrert,
                        ),
                    refDelytelseId = linje.refDelytelseId,
                )
            }.let(::normaliserUlagredeLinjer)

    private fun Oppdrag110.requireFagsystemId(): String =
        requireNotNull(fagsystemId?.takeIf(String::isNotBlank)) {
            "Oppdrag mangler fagsystemId"
        }

    private fun Oppdrag110.fagomraadeKode(): FagomraadeKode =
        try {
            FagomraadeKode.fromValue(kodeFagomraade)
        } catch (_: NoSuchElementException) {
            throw IllegalArgumentException("Ukjent kodeFagomraade: $kodeFagomraade")
        }

    private fun normaliserUlagredeLinjer(endringer: List<OppdragslinjeEndring>): List<Oppdragslinje> {
        val plan =
            OppdragEndringsmotor(OppdragslinjeOverlapPolicy())
                .planlegg(emptyList(), endringer)
        val linjer = linkedMapOf<String, Oppdragslinje>()
        // linjer som ikke er persistert i databasen får midlertidige negative kjedeId'er, som senere kan erstattes med reelle Id'er når de persisteres
        var nesteMidlertidigeKjedeId = -1L

        plan.mutasjoner.forEach { mutasjon ->
            when (mutasjon) {
                is OppdragslinjeMutasjon.Opprett -> {
                    val kjedeId =
                        mutasjon.refDelytelseId
                            ?.let(linjer::get)
                            ?.oppdragslinjekjedeId
                            ?: OppdragslinjekjedeId(nesteMidlertidigeKjedeId--)
                    linjer[mutasjon.linje.delytelseId] = mutasjon.linje.copy(oppdragslinjekjedeId = kjedeId)
                }

                is OppdragslinjeMutasjon.Avkort -> {
                    val linje = requireNotNull(linjer[mutasjon.delytelseId])
                    linjer[mutasjon.delytelseId] = linje.copy(datoVedtakTom = mutasjon.datoVedtakTom)
                }

                is OppdragslinjeMutasjon.Slett -> linjer.remove(mutasjon.delytelseId)
                is OppdragslinjeMutasjon.Oppdater -> linjer[mutasjon.linje.delytelseId] = mutasjon.linje
            }
        }
        return linjer.values.toList()
    }

    private fun OppdragsLinje150.toOppdragslinje(
        linjeId: Short,
        utbetalesTilId: AktoerId? = null,
        skyldnerId: AktoerId? = null,
        ytelseKlasseKodeLookup: Map<YtelseKlasseKodeRef, YtelseKlasseKode> = emptyMap(),
        registrert: LocalDateTime = LocalDateTime.now(),
    ): Oppdragslinje =
        Oppdragslinje(
            linjeId = linjeId,
            delytelseId = delytelseId.orEmpty(),
            sats = sats,
            typeSats = TypeSats.fromValue(typeSats),
            brukKjoreplan =
                when (brukKjoreplan?.uppercase()) {
                    "J" -> true
                    "N" -> false
                    else -> null
                },
            datoVedtakFom = datoVedtakFom,
            datoVedtakTom = datoVedtakTom,
            vedtakId = vedtakId.orEmpty(),
            henvisning = henvisning,
            utbetalesTilId = utbetalesTilId,
            kodeKlasse =
                YtelseKlasseKodeRef(kodeKlassifik).let {
                    requireNotNull(ytelseKlasseKodeLookup[it]) { "Ukjent ytelseklassekode: $it" }
                },
            skyldnerId = skyldnerId,
            kid = kid,
            typeSoknad = typeSoknad,
            maksDato = refusjonsinfo156?.maksDato,
            refunderesId = refusjonsinfo156?.refunderesId,
            bruker = saksbehId,
            tidspktReg = registrert,
            vedtakssats = vedtakssats157?.vedtakssats,
        )
}
