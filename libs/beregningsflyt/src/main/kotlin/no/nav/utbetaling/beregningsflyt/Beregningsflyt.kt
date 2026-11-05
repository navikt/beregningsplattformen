package no.nav.utbetaling.beregningsflyt

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

import no.nav.sokos.dare.poc.aktoer.AktoerId
import no.nav.sokos.dare.poc.avvent.Avvent
import no.nav.sokos.dare.poc.beregning.Beregning
import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Beregningsmotor
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Skattemotor
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Trekkmotor
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Ytelsesmotor
import no.nav.sokos.dare.poc.beregning.dataklasser.BeregningsStatus
import no.nav.sokos.dare.poc.beregning.fordeler.BeregningsSegment
import no.nav.sokos.dare.poc.beregning.fordeler.BeregningsmotorKontekst
import no.nav.sokos.dare.poc.beregning.fordeler.LinjeGroupKey
import no.nav.sokos.dare.poc.beregning.fordeler.OppdragDelperiode
import no.nav.sokos.dare.poc.beregning.fordeler.SaldoBoetter
import no.nav.sokos.dare.poc.beregning.fordeler.SatsBoetter
import no.nav.sokos.dare.poc.beregning.fordeler.SkatteKontekst
import no.nav.sokos.dare.poc.oppdrag.Oppdrag
import no.nav.sokos.dare.poc.oppdrag.OppdragId
import no.nav.sokos.dare.poc.oppdrag.Oppdragslinje
import no.nav.sokos.dare.poc.oppdrag.Oppdragslinjekjede
import no.nav.sokos.dare.poc.oppdrag.TypeSats
import no.nav.sokos.dare.poc.oppdrag.YtelseKlasseKode
import no.nav.sokos.dare.poc.oppdrag.YtelseKlasseKodeRef
import no.nav.sokos.dare.poc.trekk.FagomraadeKode
import no.nav.utbetaling.beregning.oppdrag.Avventingsårsak
import no.nav.utbetaling.beregning.oppdrag.BrukKjøreplan
import no.nav.utbetaling.beregning.oppdrag.Oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.Organisasjonsnummer
import no.nav.utbetaling.beregning.oppdrag.Personidentifikator
import no.nav.utbetaling.beregning.oppdrag.SamhandlerId
import no.nav.utbetaling.beregning.tid.Periode

class Beregningsflyt {
    private data class Segmentgruppering(
        val fagområde: FagomraadeKode,
        val fagsystemId: String,
        val utbetalesTil: AktoerId,
        val kid: String?,
    )

    fun beregn(
        beregningsperiode: Periode.LukketPeriode,
        oppdragstidslinje: Oppdragstidslinje,
        skatteKontekst: SkatteKontekst,
        beregnet: LocalDateTime,
        restFrikortBeloep: BigDecimal?,
    ): Pair<BeregningsmotorKontekst, List<Beregning>> {
        val fom = beregningsperiode.fom
        val tom = beregningsperiode.tom
        // todo finne splittpunkter basert på skattekontekst se beregningsfordeler.

        val delperioder =
            oppdragstidslinje.tidslinjer.flatMap { (_, tidslinje) ->
                val beregningstidslinje = beregningstidslinje(beregningsperiode, tidslinje, beregnet)

                beregningstidslinje.map { (periode, beregningsopplysninger) ->
                    val oppdragsopplysninger = beregningsopplysninger.oppdragopplysninger
                    val datoIntervall = DatoIntervall(periode.fom, (periode as? Periode.LukketPeriode)?.tom)
                    val oppdragId = OppdragId(0)
                    val gjelderId =
                        when (val gjelder = oppdragsopplysninger.gjelder!!.opplysning) {
                            is Personidentifikator -> AktoerId(gjelder.identifikator.toLong())
                        }
                    val utbetalesTil =
                        when (val utbetalesTil = oppdragsopplysninger.utbetalesTil!!.opplysning) {
                            is Organisasjonsnummer -> AktoerId(utbetalesTil.nummer.toLong())
                            is Personidentifikator -> AktoerId(utbetalesTil.identifikator.toLong())
                        }
                    val bruker = oppdragsopplysninger.sats.saksbehandlerId.id
                    OppdragDelperiode(
                        oppdrag =
                            Oppdrag(
                                id = oppdragId,
                                fagsystemId = oppdragsopplysninger.fagsystemId.id,
                                fagomraade = FagomraadeKode.fromValue(oppdragsopplysninger.fagområde.fagområde),
                                datoForfall = oppdragsopplysninger.forfall?.opplysning,
                                gjelderId = gjelderId,
                                datoTom = datoIntervall.tom,
                                bilagsType = oppdragsopplysninger.bilagstype?.opplysning?.type,
                                bruker = bruker,
                                tidspktReg = LocalDateTime.now(),
                                oppdragslinjekjeder =
                                    listOf(
                                        Oppdragslinjekjede(
                                            id = null,
                                            oppdragId = oppdragId,
                                            tidspktReg = LocalDateTime.now(),
                                            linjer =
                                                listOf(
                                                    Oppdragslinje(
                                                        id = null,
                                                        oppdragslinjekjedeId = null,
                                                        linjeId = 0,
                                                        delytelseId = oppdragsopplysninger.delytelseId.id,
                                                        sats = oppdragsopplysninger.sats.opplysning.verdi,
                                                        typeSats = TypeSats.valueOf(oppdragsopplysninger.sats.opplysning.satsType),
                                                        brukKjoreplan =
                                                            when (oppdragsopplysninger.brukKjøreplan.opplysning) {
                                                                BrukKjøreplan.Ja -> true
                                                                BrukKjøreplan.Nei -> false
                                                            },
                                                        datoVedtakFom = periode.fom,
                                                        datoVedtakTom = (periode as? Periode.LukketPeriode)?.tom,
                                                        vedtakId = oppdragsopplysninger.vedtakId?.opplysning?.id ?: "Javel",
                                                        henvisning = oppdragsopplysninger.henvisning?.opplysning?.henvisning,
                                                        utbetalesTilId = utbetalesTil,
                                                        kodeKlasse =
                                                            YtelseKlasseKode(
                                                                kode = YtelseKlasseKodeRef(oppdragsopplysninger.ytelseKlassekode.opplysning.kode),
                                                                beskrivelse = "ja",
                                                                grunnlagSkatt = true,
                                                                grunnlagTrekk = true,
                                                                fagomraader = listOf(FagomraadeKode.AAP), // TODO
                                                                tidspktOpprettet = LocalDateTime.now(),
                                                            ),
                                                        skyldnerId =
                                                            when (val skyldner = oppdragsopplysninger.skyldner?.opplysning) {
                                                                is SamhandlerId -> AktoerId(skyldner.tssId.toLong())
                                                                null -> null
                                                            },
                                                        kid = oppdragsopplysninger.kidnummer?.opplysning?.nummer,
                                                        typeSoknad = oppdragsopplysninger.søknadstype?.opplysning?.type,
                                                        maksDato = oppdragsopplysninger.maksdato?.opplysning,
                                                        refunderesId =
                                                            when (val refunderes = oppdragsopplysninger.refunderesTil?.opplysning) {
                                                                is Organisasjonsnummer -> refunderes.nummer
                                                                null -> null
                                                            },
                                                        bruker = bruker,
                                                        tidspktReg = LocalDateTime.now(),
                                                        vedtakssats = oppdragsopplysninger.vedtakssats?.opplysning?.verdi,
                                                        annullert = false,
                                                    ),
                                                ),
                                        ),
                                    ),
                                avventPerioder =
                                    oppdragsopplysninger.avventingsårsak?.let {
                                        listOf(
                                            Avvent(
                                                id = null,
                                                oppdragId = oppdragId,
                                                periode = datoIntervall,
                                                kodeArsak =
                                                    when (it.opplysning) {
                                                        is Avventingsårsak.Avregning -> "AVRE"
                                                        is Avventingsårsak.Refusjonskrav -> "AVRK"
                                                    },
                                                datoOverfores =
                                                    when (val avventing = it.opplysning) {
                                                        is Avventingsårsak.Avregning -> avventing.til
                                                        is Avventingsårsak.Refusjonskrav -> avventing.til
                                                    },
                                                kansellert = false,
                                                bruker = it.saksbehandlerId.id,
                                                tidspktReg = LocalDateTime.now(),
                                            ),
                                        )
                                    } ?: emptyList(),
                            ),
                        periode = datoIntervall,
                        konto = "",
                        gjelder = gjelderId,
                        beregnet = beregnet,
                        utbetalesTil = utbetalesTil,
                        status = BeregningsStatus.BEREGNET,
                        trekk = emptyList(),
                        ekstraInfo = mutableListOf(),
                        overfoeringsdato = null,
                        tidligereTrukneTrekkvedtakIMaaneden = emptySet(),
                    )
                }
            }

        val beregningssegment =
            delperioder
                .groupBy {
                    Segmentgruppering(
                        fagområde = it.oppdrag.fagomraade,
                        fagsystemId = it.oppdrag.fagsystemId,
                        utbetalesTil = it.utbetalesTil!!,
                        kid =
                            it.oppdrag.oppdragslinjekjeder
                                .single()
                                .linjer
                                .single()
                                .kid,
                    )
                }.map { (segmentgruppering, delperioder) ->
                    BeregningsSegment(
                        linjeGroupKey = LinjeGroupKey(utbetalesTilId = segmentgruppering.utbetalesTil, kid = segmentgruppering.kid),
                        delperioder = delperioder,
                    )
                }

        val beregningsmotorKontekst =
            BeregningsmotorKontekst(
                periode = DatoIntervall(fom, tom),
                restFrikortBeloep = restFrikortBeloep,
                segmenter = beregningssegment,
                skatteKontekst = skatteKontekst,
                satsBoetter =
                    SatsBoetter(
                        innkommendeSatser = emptyMap(),
                    ),
                saldoBoetter =
                    SaldoBoetter(
                        innkommendeSaldoer = emptyMap(),
                    ),
                overføringsdato = LocalDate.now(),
                beregnet = beregnet,
            )

        val beregningsmotor =
            Beregningsmotor(
                ytelser = Ytelsesmotor(),
                trekk = Trekkmotor(),
                skatt = Skattemotor(),
            )

        return beregningsmotorKontekst to beregningsmotor.calculate(beregningsmotorKontekst).beregninger()
    }
}
