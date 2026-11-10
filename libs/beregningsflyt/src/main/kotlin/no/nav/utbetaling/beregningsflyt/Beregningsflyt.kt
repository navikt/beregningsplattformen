package no.nav.utbetaling.beregningsflyt

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

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
import no.nav.sokos.dare.poc.beregning.fordeler.OppdragDelperiode
import no.nav.sokos.dare.poc.beregning.fordeler.SaldoBoetter
import no.nav.sokos.dare.poc.beregning.fordeler.SatsBoetter
import no.nav.sokos.dare.poc.beregning.fordeler.TrekkvedtakForBeregning
import no.nav.sokos.dare.poc.beregning.fordeler.beregnInitielleSaldoer
import no.nav.sokos.dare.poc.beregning.fordeler.beregnInitielleSatser
import no.nav.sokos.dare.poc.beregning.fordeler.makeTrekkPeriodeForBeregning
import no.nav.sokos.dare.poc.oppdrag.Oppdrag
import no.nav.sokos.dare.poc.oppdrag.OppdragId
import no.nav.sokos.dare.poc.oppdrag.Oppdragslinje
import no.nav.sokos.dare.poc.oppdrag.Oppdragslinjekjede
import no.nav.sokos.dare.poc.oppdrag.TypeSats
import no.nav.sokos.dare.poc.oppdrag.YtelseKlasseKode
import no.nav.sokos.dare.poc.oppdrag.YtelseKlasseKodeRef
import no.nav.sokos.dare.poc.trekk.FagomraadeKode
import no.nav.sokos.dare.poc.trekk.SatsType
import no.nav.sokos.dare.poc.trekk.Trekkberegningstype
import no.nav.sokos.dare.poc.trekk.Trekkperiode
import no.nav.sokos.dare.poc.trekk.Trekkvedtak
import no.nav.utbetaling.beregning.oppdrag.Avventingsårsak
import no.nav.utbetaling.beregning.oppdrag.BrukKjøreplan
import no.nav.utbetaling.beregning.oppdrag.Organisasjonsnummer
import no.nav.utbetaling.beregning.oppdrag.Personidentifikator
import no.nav.utbetaling.beregning.oppdrag.SamhandlerId
import no.nav.utbetaling.beregning.tid.Periode

class Beregningsflyt {
    internal data class Segmentgruppering(
        val fagområde: FagomraadeKode,
        val fagsystemId: String,
        val utbetalesTil: AktoerId,
        val kid: String?,
        val status: BeregningsStatus,
    )

    fun beregn(beregningsgrunnlag: Beregningsgrunnlag): Pair<BeregningsmotorKontekst, List<Beregning>> {
        val beregningstidspunkt = beregningsgrunnlag.beregningstidspunkt()

        val delperioder =
            beregningsgrunnlag.oppdragstidslinje.tidslinjer.flatMap { (_, tidslinje) ->
                val beregningstidslinje =
                    beregningstidslinje(
                        beregningsperiode = beregningsgrunnlag.beregningsperiode,
                        trekk = beregningsgrunnlag.trekkVedtak,
                        relaterteBeregninger = beregningsgrunnlag.relaterteBeregninger,
                        tidslinje = tidslinje,
                        beregningstidspunkt = beregningstidspunkt,
                    )
                beregningstidslinje.map { (periode, beregningsopplysninger) ->
                    val oppdragsopplysninger = beregningsopplysninger.oppdragopplysninger
                    val datoIntervall = DatoIntervall(periode.fom, (periode as? Periode.LukketPeriode)?.tom)
                    val oppdragId = OppdragId(0)
                    val gjelderId =
                        when (val gjelder = oppdragsopplysninger.gjelder!!.opplysning) {
                            is Personidentifikator -> AktoerId(gjelder.identifikator.toLong())
                        }
                    val utbetalesTil =
                        when (val utbetalesTil = oppdragsopplysninger.utbetalesTil?.opplysning) {
                            is Organisasjonsnummer -> AktoerId(utbetalesTil.nummer.toLong())
                            is Personidentifikator -> AktoerId(utbetalesTil.identifikator.toLong())
                            null -> gjelderId
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
                        beregnet = beregningstidspunkt,
                        utbetalesTil = utbetalesTil,
                        status = BeregningsStatus.BEREGNET,
                        trekk = buildTrekkForDelperiode(datoIntervall, beregningsgrunnlag.overføres, beregningsgrunnlag.trekkVedtak),
                        ekstraInfo = mutableListOf(),
                        overfoeringsdato = beregningsgrunnlag.overføres,
                        tidligereTrukneTrekkvedtakIMaaneden = emptySet(),
                        relaterteBeregninger = beregningsopplysninger.tidligereYtelsesberegninger,
                    ) to beregningsopplysninger
                }
            }

        val ferdigeSegmenter = mutableListOf<BeregningsSegment>()
        var nåværendeSegment: Pair<Segmentgruppering, BeregningsSegment>? = null

        delperioder.forEach { (delperiode, beregningsopplysninger) ->
            val (gruppering, segment) =
                Avventinger.lagBeregningsSegment(
                    delperiode = delperiode,
                    oppdragsopplysninger = beregningsopplysninger.oppdragopplysninger,
                )
            if (nåværendeSegment == null) {
                nåværendeSegment = gruppering to segment
            } else if (gruppering == nåværendeSegment.first) {
                nåværendeSegment = nåværendeSegment.first to nåværendeSegment.second.utvidMed(segment.delperioder)
            } else {
                ferdigeSegmenter.add(nåværendeSegment.second)
                nåværendeSegment = gruppering to segment
            }
        }

        if (nåværendeSegment != null) ferdigeSegmenter.add(nåværendeSegment.second)

        val saldoBoetter = beregnInitielleSaldoer(beregningsgrunnlag.trekkVedtak, beregningsgrunnlag.relaterteBeregninger)
        val satsBoetter = beregnInitielleSatser(ferdigeSegmenter, beregningsgrunnlag.relaterteBeregninger)

        val beregningsmotorKontekst =
            BeregningsmotorKontekst(
                periode = DatoIntervall(beregningsgrunnlag.beregningsperiode.fom, beregningsgrunnlag.beregningsperiode.tom),
                restFrikortBeloep = beregningsgrunnlag.tilgjengeligFrikortbeløp,
                segmenter = ferdigeSegmenter,
                skatteKontekst = beregningsgrunnlag.skatteKontekst,
                satsBoetter = SatsBoetter(satsBoetter),
                saldoBoetter = SaldoBoetter(saldoBoetter),
                overføringsdato = beregningsgrunnlag.overføres,
                beregnet = beregningstidspunkt,
            )

        val beregningsmotor =
            Beregningsmotor(
                ytelser = Ytelsesmotor(),
                trekk = Trekkmotor(),
                skatt = Skattemotor(),
            )

        return beregningsmotorKontekst to beregningsmotor.calculate(beregningsmotorKontekst).beregninger()
    }

    private companion object {
        fun BeregningsSegment.utvidMed(delperioder: List<OppdragDelperiode>) = this.copy(delperioder = this.delperioder + delperioder)
    }
}

// --- Grunnlagsbygging ---
private fun buildTrekkForDelperiode(
    delperiode: DatoIntervall,
    overfoeringsdatoEllerBeregnetDato: LocalDate,
    trekk: List<Trekkvedtak>,
): List<TrekkvedtakForBeregning> =
    trekk.filter { it.isTrekkGyldigIFagomraade(FagomraadeKode.AAP) }.mapNotNull { vedtak ->
        val relevantPeriode =
            vedtak.trekkperioder.firstOrNull { tp ->
                when (vedtak.trekktype?.trekkberegningstype) {
                    Trekkberegningstype.UTBDATO -> tp.periode.contains(overfoeringsdatoEllerBeregnetDato)
                    Trekkberegningstype.YTELPER, null -> tp.periode.overlaps(delperiode)
                }
            }
        relevantPeriode?.let {
            val vedtakBoetter = boetterForTrekkvedtak(vedtak, it, delperiode, overfoeringsdatoEllerBeregnetDato)
            TrekkvedtakForBeregning(
                trekkvedtak = vedtak,
                trekkperiode = makeTrekkPeriodeForBeregning(it, vedtakBoetter),
            )
        }
    }

/** For UTBDATO-trekk brukes overfoeringsdato som bøtte-nøkkel i stedet for ytelsesperioden */
private fun boetterForTrekkvedtak(
    vedtak: Trekkvedtak,
    trekkperiode: Trekkperiode,
    delperiode: DatoIntervall,
    overfoeringsdato: LocalDate,
): List<String> {
    val defaultBoetter =
        when (trekkperiode.satstype) {
            SatsType.M -> listOf(YearMonth.from(delperiode.fom).toString())
            SatsType.D -> delperiode.hverdager.map { it.toString() }
            else -> emptyList()
        }
    return when (vedtak.trekktype?.trekkberegningstype) {
        Trekkberegningstype.YTELPER, null -> {
            defaultBoetter
        }

        Trekkberegningstype.UTBDATO -> {
            when (trekkperiode.satstype) {
                SatsType.D -> listOf(overfoeringsdato.toString())
                SatsType.M -> listOf(YearMonth.from(overfoeringsdato).toString())
                SatsType.P -> defaultBoetter
            }
        }
    }
}
