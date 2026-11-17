package no.nav.utbetaling.beregningsflyt

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

import no.nav.sokos.dare.poc.aktoer.AktoerId
import no.nav.sokos.dare.poc.beregning.Beregning
import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.beregning.DelberegningType
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
import no.nav.sokos.dare.poc.beregning.fordeler.TrekkvedtakForBeregning
import no.nav.sokos.dare.poc.beregning.fordeler.Ytelselinje
import no.nav.sokos.dare.poc.beregning.fordeler.beregnInitielleSaldoer
import no.nav.sokos.dare.poc.beregning.fordeler.beregnInitielleSatser
import no.nav.sokos.dare.poc.beregning.fordeler.makeTrekkPeriodeForBeregning
import no.nav.sokos.dare.poc.oppdrag.OppdragId
import no.nav.sokos.dare.poc.oppdrag.YtelseKlasseKode
import no.nav.sokos.dare.poc.oppdrag.YtelseKlasseKodeRef
import no.nav.sokos.dare.poc.trekk.FagomraadeKode
import no.nav.sokos.dare.poc.trekk.SatsType
import no.nav.sokos.dare.poc.trekk.Trekkberegningstype
import no.nav.sokos.dare.poc.trekk.Trekkperiode
import no.nav.sokos.dare.poc.trekk.Trekkvedtak
import no.nav.sokos.dare.poc.trekk.TrekkvedtakId
import no.nav.utbetaling.beregning.oppdrag.Opplysninger
import no.nav.utbetaling.beregning.oppdrag.Organisasjonsnummer
import no.nav.utbetaling.beregning.oppdrag.Personidentifikator
import no.nav.utbetaling.beregning.tid.Periode
import no.nav.utbetaling.beregningsflyt.Avventinger.medAvventing

class Beregningsflyt {
    fun beregn(beregningsgrunnlag: Beregningsgrunnlag): Pair<BeregningsmotorKontekst, List<Beregning>> {
        val beregningstidspunkt = beregningsgrunnlag.beregningstidspunkt()

        val beregningsegmenter =
            Segmentering.grunnlag(beregningsgrunnlag.oppdragstidslinje).map { segmentgrunnlag ->

                val oppdragDelperioder =
                    beregningstidslinje(
                        beregningsperiode = beregningsgrunnlag.beregningsperiode,
                        trekk = beregningsgrunnlag.trekkVedtak,
                        relaterteBeregninger = beregningsgrunnlag.relaterteBeregninger,
                        tidslinje = segmentgrunnlag.delperiodegrunnlag,
                        beregningstidspunkt = beregningstidspunkt,
                    ).flatMap { (periode, beregningsopplysninger) ->
                        beregningsopplysninger.oppdragopplysninger.entries.map { (oppdragsidentifikator, opplysninger) ->
                            val tidligereTrukneTrekkvedtakIMaaneden =
                                beregningsgrunnlag.relaterteBeregninger
                                    .filter { beregning -> beregning.overfoeres?.let { YearMonth.from(it) == YearMonth.from(beregningsgrunnlag.overføres) } == true }
                                    .flatMap { it.delberegning }
                                    .filter { it.oppdragId?.id == 0L } // husk bruke en id som er unik når vi kommer dit.
                                    .flatMap { it.linjer }
                                    .filter { it.type == DelberegningType.TREKK }
                                    .mapNotNull { it.trekkvedtakId }
                                    .toSet()

                            opplysninger.tilOppdragDelperiode(
                                oppdragId = OppdragId(0), // husk bruke en id som er unik når vi kommer dit.
                                periode = periode,
                                beregningstidspunkt = beregningstidspunkt,
                                overføres = beregningsgrunnlag.overføres,
                                fagomraade = FagomraadeKode.fromValue(oppdragsidentifikator.fagområde.fagområde),
                                tidligereYtelsesberegninger = beregningsopplysninger.tidligereYtelsesberegninger,
                                trekkvedtak = beregningsgrunnlag.trekkVedtak,
                                tidligereTrukneTrekkvedtakIMaaneden = tidligereTrukneTrekkvedtakIMaaneden,
                            )
                        }
                    }

                val beregningsegment =
                    BeregningsSegment(
                        linjeGroupKey = LinjeGroupKey(utbetalesTilId = oppdragDelperioder.first().utbetalesTil!!, kid = segmentgrunnlag.kidnummer?.nummer),
                        delperioder = oppdragDelperioder,
                    )

                when (val avventingsårsak = segmentgrunnlag.avventingsårsak) {
                    null -> beregningsegment
                    else -> beregningsegment.medAvventing(avventingsårsak)
                }
            }

        val saldoBoetter = beregnInitielleSaldoer(beregningsgrunnlag.trekkVedtak, beregningsgrunnlag.relaterteBeregninger)
        val satsBoetter = beregnInitielleSatser(beregningsegmenter, beregningsgrunnlag.relaterteBeregninger)

        val beregningsmotorKontekst =
            BeregningsmotorKontekst(
                periode = DatoIntervall(beregningsgrunnlag.beregningsperiode.fom, beregningsgrunnlag.beregningsperiode.tom),
                restFrikortBeloep = beregningsgrunnlag.tilgjengeligFrikortbeløp,
                segmenter = beregningsegmenter,
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
        private fun List<Opplysninger>.tilOppdragDelperiode(
            oppdragId: OppdragId,
            periode: Periode,
            beregningstidspunkt: LocalDateTime,
            overføres: LocalDate,
            fagomraade: FagomraadeKode,
            tidligereYtelsesberegninger: List<Beregning>,
            trekkvedtak: List<Trekkvedtak>,
            tidligereTrukneTrekkvedtakIMaaneden: Set<TrekkvedtakId>,
        ): OppdragDelperiode {
            val datoIntervall = DatoIntervall(periode.fom, (periode as? Periode.LukketPeriode)?.tom)
            val gjelderId =
                when (val gjelder = first().gjelder!!.opplysning) {
                    is Personidentifikator -> AktoerId(gjelder.identifikator.toLong())
                }
            val utbetalesTil =
                when (val utbetalesTil = first().utbetalesTil?.opplysning) {
                    is Organisasjonsnummer -> AktoerId(utbetalesTil.nummer.toLong())
                    is Personidentifikator -> AktoerId(utbetalesTil.identifikator.toLong())
                    null -> gjelderId
                }
            val bruker = first().sats.saksbehandlerId.id

            return OppdragDelperiode(
                periode = datoIntervall,
                konto = "TODO-fjern-meg-jeg-skal-ikke-være-her",
                gjelder = gjelderId,
                beregnet = beregningstidspunkt,
                utbetalesTil = utbetalesTil,
                status = BeregningsStatus.BEREGNET,
                ytelselinjer = this.map {
                    Ytelselinje(
                        sats = it.sats.opplysning.verdi,
                        vedtakssats = it.vedtakssats?.opplysning?.verdi,
                        ytelseKlassekode = YtelseKlasseKode(
                            kode = YtelseKlasseKodeRef(it.ytelseKlassekode.opplysning.kode),
                            beskrivelse = "ja",
                            grunnlagSkatt = true,
                            grunnlagTrekk = true,
                            fagomraader = listOf(FagomraadeKode.AAP), // TODO
                            tidspktOpprettet = LocalDateTime.now(),
                        ),
                    )
                },
                fagomraade = fagomraade,
                oppdragId = oppdragId,
                trekk = buildTrekkForDelperiode(datoIntervall, overføres, trekkvedtak),
                ekstraInfo = mutableListOf(),
                overfoeringsdato = overføres,
                tidligereTrukneTrekkvedtakIMaaneden = tidligereTrukneTrekkvedtakIMaaneden,
                relaterteBeregninger = tidligereYtelsesberegninger,
            )
        }
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
