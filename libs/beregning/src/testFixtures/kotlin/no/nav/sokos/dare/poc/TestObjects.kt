package no.nav.sokos.dare.poc

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicLong

import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.trekk.FagomraadeKode
import no.nav.sokos.dare.poc.trekk.SatsType
import no.nav.sokos.dare.poc.trekk.TrekkAlt
import no.nav.sokos.dare.poc.trekk.TrekkAlternativ
import no.nav.sokos.dare.poc.trekk.TrekkFagomraade
import no.nav.sokos.dare.poc.trekk.TrekkStatus
import no.nav.sokos.dare.poc.trekk.Trekkberegningstype.UTBDATO
import no.nav.sokos.dare.poc.trekk.Trekkberegningstype.YTELPER
import no.nav.sokos.dare.poc.trekk.Trekkperiode
import no.nav.sokos.dare.poc.trekk.TrekkperiodeId
import no.nav.sokos.dare.poc.trekk.Trekktype
import no.nav.sokos.dare.poc.trekk.Trekkvedtak
import no.nav.sokos.dare.poc.trekk.TrekkvedtakId

object TestObjects {
    fun trekkVedtak(
        trekkalt: TrekkAlternativ,
        type: Trekktype,
        perioder: List<DatoIntervall>,
        sats: BigDecimal,
        belopSaldo: BigDecimal? = null,
    ): Trekkvedtak {
        val tId = TrekkvedtakId(TestId.next())
        val trekkPerioder =
            perioder.map { periode ->
                Trekkperiode(
                    TrekkperiodeId(TestId.next()),
                    trekkvedtakId = tId.id,
                    datoFom = periode.fom,
                    datoTom = periode.tom,
                    sats = sats,
                    satstype = SatsType.valueOf(trekkalt.name.substring(3, 4)),
                    bruker = "",
                    tidspktReg = LocalDateTime.now(),
                )
            }

        return Trekkvedtak(
            id = TrekkvedtakId(TestId.next()),
            trekktypeKode = type.kodeTrekktype,
            kreditorIdTss = "",
            kreditorTrekkId = "",
            trekkalt = TrekkAlt.valueOf(trekkalt.name.substring(0, 3)),
            belopSaldo = belopSaldo,
            ansvarligEnhet = "",
            kilde = "",
            debitorId = 0L,
            trekkstatus = TrekkStatus.LOPE,
            trekktype = type,
            bruker = "bruker",
            kreditorRef = "",
            tidspktReg = LocalDateTime.now(),
            trekkperioder = trekkPerioder,
            fagomraader = listOf(TrekkFagomraade(trekkvedtakId = 0L, fagomraadeKode = FagomraadeKode.AAP, aktiv = true, tidspktReg = LocalDateTime.now())),
        )
    }

    fun trekkVedtak(
        trekkalt: TrekkAlternativ,
        type: Trekktype,
        perioder: List<Pair<BigDecimal, DatoIntervall>>,
        belopSaldo: BigDecimal? = null,
    ): Trekkvedtak {
        val tId = TrekkvedtakId(TestId.next())
        val trekkPerioder =
            perioder.map { periode ->
                Trekkperiode(
                    TrekkperiodeId(TestId.next()),
                    trekkvedtakId = tId.id,
                    datoFom = periode.second.fom,
                    datoTom = periode.second.tom,
                    sats = periode.first,
                    satstype = SatsType.valueOf(trekkalt.name.substring(3, 4)),
                    bruker = "",
                    tidspktReg = LocalDateTime.now(),
                )
            }

        return Trekkvedtak(
            id = TrekkvedtakId(TestId.next()),
            trekktypeKode = type.kodeTrekktype,
            kreditorIdTss = "",
            kreditorTrekkId = "",
            trekkalt = TrekkAlt.valueOf(trekkalt.name.substring(0, 3)),
            belopSaldo = belopSaldo,
            ansvarligEnhet = "",
            kilde = "",
            debitorId = 0L,
            trekkstatus = TrekkStatus.LOPE,
            trekktype = type,
            bruker = "bruker",
            kreditorRef = "",
            tidspktReg = LocalDateTime.now(),
            trekkperioder = trekkPerioder,
        )
    }

    object TrekkType {
        val YTELSESPERIODE_SKATTEREDUSERENDE =
            Trekktype(
                kodeTrekktype = "TESTYPSR",
                beskrivelse = "Test Ytelsesperiode Skattereduserende",
                prioritet = "10",
                kodeKlasseTrekk = "TBD",
                kodeTrekkategori = "TBD",
                reduserSkattegr = true,
                kodeOppgjoerstype = "TDB",
                kodeKlasseOppgj = "TDB",
                belopsgrense = 10000000L,
                bruker = "",
                tidspktEndret = LocalDateTime.now(),
                trekkberegningstype = YTELPER,
            )

        val YTELSESPERIODE_IKKE_SKATTEREDUSERENDE =
            Trekktype(
                kodeTrekktype = "TESTYPISR",
                beskrivelse = "Test Ytelsesperiode Ikke-Skattereduserende",
                prioritet = "10",
                kodeKlasseTrekk = "TBD",
                kodeTrekkategori = "TBD",
                reduserSkattegr = false,
                kodeOppgjoerstype = "TDB",
                kodeKlasseOppgj = "TDB",
                belopsgrense = 10000000L,
                bruker = "",
                tidspktEndret = LocalDateTime.now(),
                trekkberegningstype = YTELPER,
            )

        val UTBETALINGSDATO_IKKE_SKATTEREDUSERENDE =
            Trekktype(
                kodeTrekktype = "TESTUDISR",
                beskrivelse = "Test Utbetalingsdato Ikke-Skattereduserende",
                prioritet = "10",
                kodeKlasseTrekk = "TBD",
                kodeTrekkategori = "TBD",
                reduserSkattegr = false,
                kodeOppgjoerstype = "TDB",
                kodeKlasseOppgj = "TDB",
                belopsgrense = 10000000L,
                bruker = "",
                tidspktEndret = LocalDateTime.now(),
                trekkberegningstype = UTBDATO,
            )
    }
}

private object TestId {
    private val counter = AtomicLong(0)

    fun next(): Long = counter.incrementAndGet()
}
