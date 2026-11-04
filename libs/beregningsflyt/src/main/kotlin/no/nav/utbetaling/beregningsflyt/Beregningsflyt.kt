package no.nav.utbetaling.beregningsflyt

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
import no.nav.utbetaling.beregning.oppdrag.Part
import no.nav.utbetaling.beregning.oppdrag.UtbetalesTil
import no.nav.utbetaling.beregning.tid.Periode

class Beregningsflyt {

    private data class Segmentgruppering(
        val fagområde: FagomraadeKode,
        val fagsystemId: String,
        val utbetalesTil: AktoerId,
        val kid: String?
    )

    fun beregn(beregningsperiode: Periode.LukketPeriode, oppdragstidslinje: Oppdragstidslinje, skatteKontekst: SkatteKontekst, beregnet: LocalDateTime): Pair<BeregningsmotorKontekst, List<Beregning>> {
        val fom = beregningsperiode.fom
        val tom = beregningsperiode.tom
        //todo finne splittpunkter basert på skattekontekst se beregningsfordeler.


        val delperioder = oppdragstidslinje.tidslinjer.flatMap { (_, tidslinje) ->
            val beregningstidslinje = beregningstidslinje(beregningsperiode, tidslinje, beregnet)

            beregningstidslinje.map { (periode, beregningsopplysninger ) ->
                val oppdragsopplysninger = beregningsopplysninger.oppdragopplysninger
                val datoIntervall = DatoIntervall(periode.fom, (periode as? Periode.LukketPeriode)?.tom)
                val oppdragId = OppdragId(0)
                val utbetalesTil1 = when (val utbetalesTil = oppdragsopplysninger.utbetalesTil!!.opplysning) {
                    is UtbetalesTil.Organisasjon -> AktoerId(utbetalesTil.organisasjonsnummer.nummer.toLong())
                    is UtbetalesTil.Person -> AktoerId(utbetalesTil.personidentifikator.identifikator.toLong())
                    is UtbetalesTil.Samhandler -> AktoerId(utbetalesTil.samhandlerId.tssId.toLong())
                }
                val bruker = oppdragsopplysninger.sats.saksbehandlerId.id
                OppdragDelperiode(
                    oppdrag = Oppdrag(
                        id = oppdragId,
                        fagsystemId = oppdragsopplysninger.fagsystemId.id,
                        fagomraade = FagomraadeKode.fromValue(oppdragsopplysninger.fagområde.fagområde),
                        datoForfall = oppdragsopplysninger.forfall?.opplysning,
                        gjelderId = AktoerId(oppdragsopplysninger.gjelderId!!.opplysning.identifikator.toLong()),
                        datoTom = datoIntervall.tom,
                        bilagsType = oppdragsopplysninger.bilagstype?.opplysning?.type,
                        bruker = bruker,
                        tidspktReg = LocalDateTime.now(),
                        oppdragslinjekjeder = listOf(Oppdragslinjekjede(
                            id = null,
                            oppdragId = oppdragId,
                            tidspktReg = LocalDateTime.now(),
                            linjer = listOf(Oppdragslinje(
                                id = null,
                                oppdragslinjekjedeId = null,
                                linjeId = 0,
                                delytelseId = oppdragsopplysninger.delytelseId.id,
                                sats = oppdragsopplysninger.sats.opplysning.verdi,
                                typeSats = TypeSats.valueOf(oppdragsopplysninger.sats.opplysning.satsType),
                                brukKjoreplan = when (oppdragsopplysninger.brukKjøreplan.opplysning) {
                                    BrukKjøreplan.Ja -> true
                                    BrukKjøreplan.Nei -> false
                                },
                                datoVedtakFom = periode.fom,
                                datoVedtakTom = (periode as? Periode.LukketPeriode)?.tom,
                                vedtakId = oppdragsopplysninger.vedtakId?.opplysning?.id ?: "Javel",
                                henvisning = oppdragsopplysninger.henvisning?.opplysning?.henvisning,
                                utbetalesTilId = utbetalesTil1,
                                kodeKlasse = YtelseKlasseKode(
                                    kode = YtelseKlasseKodeRef(oppdragsopplysninger.ytelseKlassekode.opplysning.kode),
                                    beskrivelse = "ja",
                                    grunnlagSkatt = true,
                                    grunnlagTrekk = true,
                                    fagomraader = listOf(FagomraadeKode.AAP), // TODO
                                    tidspktOpprettet = LocalDateTime.now()
                                ),
                                skyldnerId = when (val skyldner = oppdragsopplysninger.skyldner?.opplysning) {
                                    is Part.Organisasjon -> AktoerId(skyldner.organisasjonsnummer.nummer.toLong())
                                    is Part.Person -> AktoerId(skyldner.personidentifikator.identifikator.toLong())
                                    is Part.Samhandler -> AktoerId(skyldner.samhandlerId.tssId.toLong())
                                    null -> null
                                },
                                kid = oppdragsopplysninger.kidnummer?.opplysning?.nummer,
                                typeSoknad = oppdragsopplysninger.typeSøknad?.opplysning?.type,
                                maksDato = oppdragsopplysninger.refusjonsinfo?.opplysning?.maksDato,
                                refunderesId = when (val refunderes = oppdragsopplysninger.refusjonsinfo?.opplysning?.refunderes) {
                                    is Part.Organisasjon -> refunderes.organisasjonsnummer.nummer
                                    is Part.Person -> refunderes.personidentifikator.identifikator
                                    is Part.Samhandler -> refunderes.samhandlerId.tssId
                                    null -> null
                                },
                                bruker = bruker,
                                tidspktReg = LocalDateTime.now(),
                                vedtakssats = oppdragsopplysninger.vedtaksSats?.opplysning?.verdi,
                                annullert = false
                            ))
                        )),
                        avventPerioder = oppdragsopplysninger.avventingsårsak?.let { listOf(Avvent(
                            id = null,
                            oppdragId = oppdragId,
                            periode = datoIntervall,
                            kodeArsak = when (it.opplysning) {
                                is Avventingsårsak.Avregning -> "AVRE"
                                is Avventingsårsak.Refusjonskrav -> "AVRK"
                            },
                            datoOverfores = when (val avventing = it.opplysning) {
                                is Avventingsårsak.Avregning -> avventing.til
                                is Avventingsårsak.Refusjonskrav -> avventing.til
                            },
                            kansellert = false,
                            bruker = it.saksbehandlerId.id,
                            tidspktReg = LocalDateTime.now()
                        )) } ?: emptyList(),
                    ),
                    periode = datoIntervall,
                    konto = "",
                    gjelder = AktoerId(oppdragsopplysninger.gjelderId!!.opplysning.identifikator.toLong()),
                    beregnet = beregnet,
                    utbetalesTil = utbetalesTil1,
                    status = BeregningsStatus.BEREGNET,
                    trekk = emptyList(),
                    ekstraInfo = mutableListOf(),
                    overfoeringsdato = null,
                    tidligereTrukneTrekkvedtakIMaaneden = emptySet(),
                )
            }
        }

        val beregningssegment = delperioder.groupBy { Segmentgruppering(
            fagområde = it.oppdrag.fagomraade,
            fagsystemId = it.oppdrag.fagsystemId,
            utbetalesTil = it.utbetalesTil!!,
            kid = it.oppdrag.oppdragslinjekjeder.single().linjer.single().kid
        )}.map { (segmentgruppering, delperioder) ->
            BeregningsSegment(
                linjeGroupKey = LinjeGroupKey(utbetalesTilId = segmentgruppering.utbetalesTil, kid = segmentgruppering.kid),
                delperioder = delperioder
            )
        }

        val beregningsmotorKontekst = BeregningsmotorKontekst(
            periode = DatoIntervall(fom, tom),
            restFrikortBeloep = null,
            segmenter = beregningssegment,
            skatteKontekst = skatteKontekst,
            satsBoetter = SatsBoetter(
                innkommendeSatser = emptyMap()
            ),
            saldoBoetter = SaldoBoetter(
                innkommendeSaldoer = emptyMap()
            ),
            overføringsdato = LocalDate.now(),
            beregnet = beregnet
        )

        val beregningsmotor = Beregningsmotor(
            ytelser = Ytelsesmotor(),
            trekk = Trekkmotor(),
            skatt = Skattemotor()
        )

        return beregningsmotorKontekst to beregningsmotor.calculate(beregningsmotorKontekst).beregninger()
    }
}