package no.nav.utbetaling.beregning.lab.beregning

import java.time.LocalDateTime

import io.ktor.server.plugins.BadRequestException

import no.nav.sokos.dare.poc.trekk.FagomraadeKode
import no.nav.sokos.dare.poc.trekk.SatsType
import no.nav.sokos.dare.poc.trekk.TrekkFagomraade
import no.nav.sokos.dare.poc.trekk.TrekkStatus
import no.nav.sokos.dare.poc.trekk.Trekkberegningstype
import no.nav.sokos.dare.poc.trekk.Trekkperiode
import no.nav.sokos.dare.poc.trekk.TrekkperiodeId
import no.nav.sokos.dare.poc.trekk.Trekktype
import no.nav.sokos.dare.poc.trekk.Trekkvedtak
import no.nav.sokos.dare.poc.trekk.TrekkvedtakId

internal fun List<TrekkDTO>.tilTrekkvedtak(): List<Trekkvedtak> {
    forEachIndexed { index, trekk -> trekk.valider(index + 1) }
    val tidspunkt = LocalDateTime.now()
    var nestePeriodeId = 1L

    return mapIndexed { index, trekk ->
        val vedtakId = (index + 1).toLong()
        Trekkvedtak(
            id = TrekkvedtakId(vedtakId),
            trekktypeKode = trekk.trekkType,
            belopSaldo = trekk.saldo,
            gyldigTom = trekk.gyldigTom,
            ansvarligEnhet = "SYNTETISK",
            debitorId = 0L,
            kilde = "SYNTETISK",
            trekkstatus = TrekkStatus.LOPE,
            bruker = "SYNTETISK",
            tidspktReg = tidspunkt,
            trekkperioder =
                trekk.trekkPerioder.map { periode ->
                    Trekkperiode(
                        id = TrekkperiodeId(nestePeriodeId++),
                        trekkvedtakId = vedtakId,
                        datoFom = periode.fom,
                        datoTom = periode.tom,
                        sats = periode.sats,
                        satstype = SatsType.valueOf(periode.satsType),
                        bruker = "SYNTETISK",
                        tidspktReg = tidspunkt,
                    )
                },
            // Beregningsflyten filtrerer foreløpig trekk på AAP.
            fagomraader =
                listOf(
                    TrekkFagomraade(
                        trekkvedtakId = vedtakId,
                        fagomraadeKode = FagomraadeKode.AAP,
                        aktiv = true,
                        tidspktReg = tidspunkt,
                    ),
                ),
            trekktype =
                Trekktype(
                    kodeTrekktype = trekk.trekkType,
                    beskrivelse = "Syntetisk trekk",
                    prioritet = "10",
                    kodeKlasseTrekk = "SYNTETISK",
                    kodeTrekkategori = "SYNTETISK",
                    reduserSkattegr = trekk.skatteReduserende,
                    kodeOppgjoerstype = "SYNTETISK",
                    kodeKlasseOppgj = "SYNTETISK",
                    belopsgrense = 0L,
                    bruker = "SYNTETISK",
                    tidspktEndret = tidspunkt,
                    trekkberegningstype = Trekkberegningstype.valueOf(trekk.beregningsType),
                ),
        )
    }
}

private fun TrekkDTO.valider(trekknummer: Int) {
    fun krev(
        gyldig: Boolean,
        melding: String,
    ) {
        if (!gyldig) throw BadRequestException("Trekk $trekknummer: $melding")
    }

    krev(trekkType.isNotBlank(), "Fyll ut trekktype.")
    krev(beregningsType in Trekkberegningstype.entries.map { it.name }, "Velg beregningstype YTELPER eller UTBDATO.")
    krev(saldo == null || saldo.signum() >= 0, "Saldo må være null eller større, eller utelates.")
    krev(trekkPerioder.isNotEmpty(), "Legg til minst én trekkperiode.")

    trekkPerioder.forEachIndexed { index, periode ->
        val nummer = index + 1
        krev(periode.sats.signum() >= 0, "Periode $nummer: Satsen må være null eller større.")
        krev(periode.satsType in setOf("D", "M"), "Periode $nummer: Velg satstype D eller M.")
        krev(periode.fom <= periode.tom, "Periode $nummer: Fra og med må være før eller lik til og med.")
        krev(gyldigTom == null || periode.tom <= gyldigTom, "Periode $nummer: Til og med kan ikke være etter gyldig til og med.")
    }

    trekkPerioder
        .withIndex()
        .sortedBy { it.value.fom }
        .zipWithNext()
        .forEach { (forrige, neste) ->
            krev(
                forrige.value.tom < neste.value.fom,
                "Periode ${forrige.index + 1} og ${neste.index + 1} overlapper. Periodene kan ikke dele samme dato.",
            )
        }
}
