package no.nav.utbetaling.beregning.trekk.utils

import no.nav.utbetaling.beregning.trekk.domain.SatsType
import no.nav.utbetaling.beregning.trekk.domain.TrekkFagomraade
import no.nav.utbetaling.beregning.trekk.domain.Trekkperiode
import no.nav.utbetaling.beregning.trekk.domain.Trekkstatus
import no.nav.utbetaling.beregning.trekk.domain.Trekkvedtak
import no.nav.utbetaling.beregning.trekk.dto.Fagomraade
import no.nav.utbetaling.beregning.trekk.dto.Fagomraader
import no.nav.utbetaling.beregning.trekk.dto.Periode
import no.nav.utbetaling.beregning.trekk.dto.Perioder
import no.nav.utbetaling.beregning.trekk.dto.Trekk

object TrekkConverter {
    fun Trekk.toTrekkVedtak(): Trekkvedtak =
        Trekkvedtak(
            eksternTrekkId = trekkvedtakId,
            trekktypeKode = kodeTrekktype,
            kreditorIdTss = kreditorIdTss,
            kreditorTrekkId = kreditorTrekkId,
            kreditorRef = kreditorRef,
            kreditorKid = kreditorKid,
            debitorId = debitorId,
            belopSaldo = belopSaldo,
            datoPriFom = datoPriFom,
            gyldigTom = datoGyldigTom,
            ansvarligEnhet = enhet.orEmpty(),
            kilde = kilde,
            trekkstatus = trekkstatus?.let { Trekkstatus.fromString(it) } ?: Trekkstatus.LOPE,
            bruker = brukerId,
            tidspktReg = tidsPktReg,
        )

    fun Fagomraade.toTrekkFagomraade(trekkvedtakId: Long): TrekkFagomraade =
        TrekkFagomraade(
            trekkvedtakId = trekkvedtakId,
            fagomraadeKode = kodeFagomraade,
            aktiv = "N".equals(feilRegFagomraade, ignoreCase = true) && lopenrFagomraade == 9999,
            tidspktReg = tidsPktRegFagomraade,
        )

    fun Periode.toTrekkperiode(
        trekkvedtakId: Long,
        satstype: SatsType,
    ): Trekkperiode =
        Trekkperiode(
            trekkvedtakId = trekkvedtakId,
            datoFom = periodeFomDato,
            datoTom = periodeTomDato,
            sats = sats!!,
            satstype = satstype,
            feilReg = !("N".equals(feilRegPeriode, ignoreCase = true) && lopenrPeriode == 9999),
            bruker = brukerIdPeriode,
            tidspktReg = tidsPktRegPeriode,
        )

    fun Trekkvedtak.toTrekk(): Trekk =
        Trekk(
            id = id?.id,
            trekkvedtakId = eksternTrekkId,
            kodeTrekktype = trekktypeKode,
            kreditorIdTss = kreditorIdTss,
            kreditorTrekkId = kreditorTrekkId,
            kreditorRef = kreditorRef,
            kreditorKid = kreditorKid,
            debitorId = debitorId,
            kodeTrekkAlternativ = belopSaldo?.let { "SAL" } ?: "LOP",
            belopSaldo = belopSaldo,
            datoPriFom = datoPriFom,
            datoGyldigTom = gyldigTom,
            trekkstatus = trekkstatus.name,
            enhet = ansvarligEnhet,
            kilde = kilde,
            brukerId = bruker,
            tidsPktReg = tidspktReg,
            fagomraader =
                Fagomraader(
                    fagomraade =
                        fagomraader
                            .map {
                                Fagomraade(
                                    kodeFagomraade = it.fagomraadeKode,
                                    lopenrFagomraade = 9999,
                                    feilRegFagomraade = if (it.aktiv) "N" else "J",
                                    tidsPktRegFagomraade = it.tidspktReg,
                                )
                            }.toMutableList(),
                ),
            perioder =
                Perioder(
                    periode =
                        trekkperioder
                            .map {
                                Periode(
                                    periodeFomDato = it.datoFom,
                                    periodeTomDato = it.datoTom,
                                    sats = it.sats,
                                    lopenrPeriode = 9999,
                                    feilRegPeriode = if (it.feilReg) "N" else "J",
                                    brukerIdPeriode = it.bruker,
                                    tidsPktRegPeriode = it.tidspktReg,
                                )
                            }.toMutableList(),
                ),
        )
}
