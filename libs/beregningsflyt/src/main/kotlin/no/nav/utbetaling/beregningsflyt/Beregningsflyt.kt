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

    fun beregn(beregningsperiode: Periode.LukketPeriode, oppdragstidslinje: Oppdragstidslinje, skatteKontekst: SkatteKontekst): List<Beregning> {
        val fom = beregningsperiode.fom
        val tom = beregningsperiode.tom
        //todo finne splittpunkter basert på skattekontekst se beregningsfordeler.

        val delperioder = oppdragstidslinje.tidslinjer.flatMap { (_, tidslinje) ->
            tidslinje.map { (periode, opplysninger ) ->
                val datoIntervall = DatoIntervall(periode.fom, (periode as? Periode.LukketPeriode)?.tom)
                val oppdragId = OppdragId(0)
                val utbetalesTil1 = when (val utbetalesTil = opplysninger.utbetalesTil!!.opplysning) {
                    is UtbetalesTil.Organisasjon -> AktoerId(utbetalesTil.organisasjonsnummer.nummer.toLong())
                    is UtbetalesTil.Person -> AktoerId(utbetalesTil.personidentifikator.identifikator.toLong())
                    is UtbetalesTil.Samhandler -> AktoerId(utbetalesTil.samhandlerId.tssId.toLong())
                }
                val bruker = opplysninger.sats.saksbehandlerId.id
                OppdragDelperiode(
                    oppdrag = Oppdrag(
                        id = oppdragId,
                        fagsystemId = opplysninger.fagsystemId.id,
                        fagomraade = FagomraadeKode.fromValue(opplysninger.fagområde.fagområde),
                        datoForfall = opplysninger.forfall?.opplysning,
                        gjelderId = AktoerId(opplysninger.gjelderId!!.opplysning.identifikator.toLong()),
                        datoTom = datoIntervall.tom,
                        bilagsType = opplysninger.bilagstype?.opplysning?.type,
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
                                delytelseId = opplysninger.delytelseId.id,
                                sats = opplysninger.sats.opplysning.verdi,
                                typeSats = TypeSats.valueOf(opplysninger.sats.opplysning.satsType),
                                brukKjoreplan = when (opplysninger.brukKjøreplan.opplysning) {
                                    BrukKjøreplan.Ja -> true
                                    BrukKjøreplan.Nei -> false
                                },
                                datoVedtakFom = periode.fom,
                                datoVedtakTom = (periode as? Periode.LukketPeriode)?.tom,
                                vedtakId = opplysninger.vedtakId?.opplysning?.id ?: "Javel",
                                henvisning = opplysninger.henvisning?.opplysning?.henvisning,
                                utbetalesTilId = utbetalesTil1,
                                kodeKlasse = YtelseKlasseKode(
                                    kode = YtelseKlasseKodeRef(opplysninger.ytelseKlassekode.opplysning.kode),
                                    beskrivelse = "ja",
                                    grunnlagSkatt = true,
                                    grunnlagTrekk = true,
                                    fagomraader = emptyList(),
                                    tidspktOpprettet = LocalDateTime.now()
                                ),
                                skyldnerId = when (val skyldner = opplysninger.skyldner?.opplysning) {
                                    is Part.Organisasjon -> AktoerId(skyldner.organisasjonsnummer.nummer.toLong())
                                    is Part.Person -> AktoerId(skyldner.personidentifikator.identifikator.toLong())
                                    is Part.Samhandler -> AktoerId(skyldner.samhandlerId.tssId.toLong())
                                    null -> null
                                },
                                kid = opplysninger.kidnummer?.opplysning?.nummer,
                                typeSoknad = opplysninger.typeSøknad?.opplysning?.type,
                                maksDato = opplysninger.refusjonsinfo?.opplysning?.maksDato,
                                refunderesId = when (val refunderes = opplysninger.refusjonsinfo?.opplysning?.refunderes) {
                                    is Part.Organisasjon -> refunderes.organisasjonsnummer.nummer
                                    is Part.Person -> refunderes.personidentifikator.identifikator
                                    is Part.Samhandler -> refunderes.samhandlerId.tssId
                                    null -> null
                                },
                                bruker = bruker,
                                tidspktReg = LocalDateTime.now(),
                                vedtakssats = opplysninger.vedtaksSats?.opplysning?.verdi,
                                annullert = false
                            ))
                        )),
                        avventPerioder = opplysninger.avventingsårsak?.let { listOf(Avvent(
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
                    gjelder = AktoerId(opplysninger.gjelderId!!.opplysning.identifikator.toLong()),
                    beregnet = LocalDateTime.now(),
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
            segmenter = beregningssegment,
            skatteKontekst = skatteKontekst,
            satsBoetter = SatsBoetter(
                innkommendeSatser = emptyMap()
            ),
            saldoBoetter = SaldoBoetter(
                innkommendeSaldoer = emptyMap()
            ),
            overføringsdato = LocalDate.now(),
            beregnet = LocalDateTime.now()
        )

        val beregningsmotor = Beregningsmotor(
            ytelser = Ytelsesmotor(),
            trekk = Trekkmotor(),
            skatt = Skattemotor()
        )

        return beregningsmotor.calculate(beregningsmotorKontekst).beregninger()
    }
}