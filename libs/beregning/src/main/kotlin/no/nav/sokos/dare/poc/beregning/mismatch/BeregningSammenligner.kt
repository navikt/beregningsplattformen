package no.nav.sokos.dare.poc.beregning.mismatch

import java.math.BigDecimal

import no.nav.sokos.dare.poc.beregning.Beregning
import no.nav.sokos.dare.poc.beregning.Delberegning
import no.nav.sokos.dare.poc.beregning.Delberegningslinje

enum class BeregningAvvikType {
    LAGT_TIL,
    FJERNET,
    ENDRET,
}

data class BeregningAvvik(
    val sti: String,
    val type: BeregningAvvikType,
    val forventet: String? = null,
    val faktisk: String? = null,
)

data class BeregningTreff(
    val forventet: Beregning,
    val faktisk: Beregning,
    val avvik: List<BeregningAvvik>,
)

data class BeregningTvetydighet(
    val forventetIndeks: Int,
    val forventet: Beregning,
    val kandidater: List<Beregning>,
    val matchendeNoekkel: String,
)

data class BeregningSammenligningsResultat(
    val treff: List<BeregningTreff>,
    val avvik: List<BeregningAvvik>,
    val lagtTilBeregninger: List<Beregning>,
    val fjernedeBeregninger: List<Beregning>,
    val fjernedeBeregningIndekser: List<Int>,
    val uloesteTvetydigheter: List<BeregningTvetydighet>,
    val erLik: Boolean,
)

class BeregningSammenligner {
    fun sammenlign(
        forventet: List<Beregning>,
        faktisk: List<Beregning>,
    ): BeregningSammenligningsResultat {
        val ubrukteFaktiskeIndekser = faktisk.indices.toMutableSet()
        val treff = mutableListOf<BeregningTreff>()
        val alleAvvik = mutableListOf<BeregningAvvik>()
        val fjernet = mutableListOf<Beregning>()
        val fjernetIndekser = mutableListOf<Int>()
        val uloest = mutableListOf<BeregningTvetydighet>()

        forventet.forEachIndexed { forventetIndeks, forventetBeregning ->
            val tilgjengeligeIndekser = ubrukteFaktiskeIndekser.toList()
            val treffResultat = finnTreff(forventetBeregning, faktisk, tilgjengeligeIndekser)

            when (treffResultat) {
                is TreffResultat.Funnet -> {
                    ubrukteFaktiskeIndekser.remove(treffResultat.indeks)
                    val avvik = sammenlignBeregning(forventetIndeks, forventetBeregning, faktisk[treffResultat.indeks])
                    treff += BeregningTreff(forventetBeregning, faktisk[treffResultat.indeks], avvik)
                    alleAvvik += avvik
                }

                is TreffResultat.Tvetydig -> {
                    uloest +=
                        BeregningTvetydighet(
                            forventetIndeks = forventetIndeks,
                            forventet = forventetBeregning,
                            kandidater = treffResultat.indekser.map(faktisk::get),
                            matchendeNoekkel = treffResultat.noekkel,
                        )
                }

                is TreffResultat.Uloest -> {
                    ubrukteFaktiskeIndekser.remove(treffResultat.indeks)
                    val faktiskBeregning = faktisk[treffResultat.indeks]
                    val avvik = sammenlignBeregning(forventetIndeks, forventetBeregning, faktiskBeregning)
                    treff += BeregningTreff(forventetBeregning, faktiskBeregning, avvik)
                    alleAvvik += avvik
                    uloest +=
                        BeregningTvetydighet(
                            forventetIndeks = forventetIndeks,
                            forventet = forventetBeregning,
                            kandidater = listOf(faktiskBeregning),
                            matchendeNoekkel = treffResultat.noekkel,
                        )
                }

                TreffResultat.IkkeFunnet -> {
                    fjernet += forventetBeregning
                    fjernetIndekser += forventetIndeks
                }
            }
        }

        val lagtTil = ubrukteFaktiskeIndekser.map(faktisk::get)

        return BeregningSammenligningsResultat(
            treff = treff,
            avvik = alleAvvik,
            lagtTilBeregninger = lagtTil,
            fjernedeBeregninger = fjernet,
            fjernedeBeregningIndekser = fjernetIndekser,
            uloesteTvetydigheter = uloest,
            erLik = alleAvvik.isEmpty() && lagtTil.isEmpty() && fjernet.isEmpty() && uloest.isEmpty(),
        )
    }

    /**
     * Finner matchende kandidat blant [tilgjengeligeIndekser]. Kun den sterkeste nøkkelen (periode + første
     * delberegning oppdragId) rapporteres som uløst tvetydig dersom flere kandidater deler den. Svakere
     * nøkler (bare periode, eller bare oppdragId) velger alltid første kandidat deterministisk. Dersom
     * enten forventet beregning eller den valgte kandidaten mangler oppdragId, sammenlignes paret, men
     * markeres som uløst. Fullt nøklede beregninger uten treff rapporteres fortsatt som fjernet og lagt til.
     */
    private fun finnTreff(
        forventet: Beregning,
        faktisk: List<Beregning>,
        tilgjengeligeIndekser: List<Int>,
    ): TreffResultat {
        if (tilgjengeligeIndekser.isEmpty()) return TreffResultat.IkkeFunnet

        val forventetOppdragId = forventet.delberegning.firstOrNull()?.oppdragId
        val eksakt =
            if (forventetOppdragId == null) {
                emptyList()
            } else {
                tilgjengeligeIndekser.filter { indeks ->
                    val kandidatOppdragId = faktisk[indeks].delberegning.firstOrNull()?.oppdragId
                    kandidatOppdragId != null &&
                        faktisk[indeks].periode == forventet.periode &&
                        kandidatOppdragId == forventetOppdragId
                }
            }
        if (eksakt.isNotEmpty()) return eksakt.tilTreffResultat("periode + første delberegning oppdragId")

        val sammePeriode = tilgjengeligeIndekser.filter { faktisk[it].periode == forventet.periode }
        if (sammePeriode.isNotEmpty()) {
            val indeks = sammePeriode.first()
            return if (forventetOppdragId == null || faktisk[indeks].oppdragId() == null) {
                TreffResultat.Uloest(indeks, UNOEKLET_FALLBACK_NOEKKEL)
            } else {
                TreffResultat.Funnet(indeks)
            }
        }

        val sammeOppdrag =
            if (forventetOppdragId == null) {
                emptyList()
            } else {
                tilgjengeligeIndekser.filter { indeks ->
                    val kandidatOppdragId = faktisk[indeks].oppdragId()
                    kandidatOppdragId != null && kandidatOppdragId == forventetOppdragId
                }
            }
        if (sammeOppdrag.isNotEmpty()) return TreffResultat.Funnet(sammeOppdrag.first())

        if (forventetOppdragId == null) return TreffResultat.Uloest(tilgjengeligeIndekser.first(), UNOEKLET_FALLBACK_NOEKKEL)

        val unoeklaKandidat = tilgjengeligeIndekser.firstOrNull { faktisk[it].oppdragId() == null }
        if (unoeklaKandidat != null) return TreffResultat.Uloest(unoeklaKandidat, UNOEKLET_FALLBACK_NOEKKEL)

        return TreffResultat.IkkeFunnet
    }

    private fun Beregning.oppdragId() = delberegning.firstOrNull()?.oppdragId

    private fun List<Int>.tilTreffResultat(noekkel: String): TreffResultat = if (size == 1) TreffResultat.Funnet(first()) else TreffResultat.Tvetydig(this, noekkel)

    private fun sammenlignBeregning(
        forventetIndeks: Int,
        forventet: Beregning,
        faktisk: Beregning,
    ): List<BeregningAvvik> {
        val avvik = mutableListOf<BeregningAvvik>()
        val sti = "beregning[$forventetIndeks]"
        leggTilEndret("$sti.periode", forventet.periode, faktisk.periode, avvik)
        leggTilEndret("$sti.konto", forventet.konto, faktisk.konto, avvik)
        leggTilEndret("$sti.status", forventet.status, faktisk.status, avvik)
        leggTilEndret(
            "$sti.statusflagg",
            forventet.statusflagg.joinToString(",") { it.name },
            faktisk.statusflagg.joinToString(",") { it.name },
            avvik,
        )
        leggTilEndret("$sti.fagomraade", forventet.fagomraade, faktisk.fagomraade, avvik)
        leggTilEndret("$sti.gjelder", forventet.gjelder.id, faktisk.gjelder.id, avvik)
        leggTilEndret("$sti.overfoeres", forventet.overfoeres, faktisk.overfoeres, avvik)
        leggTilEndret("$sti.utbetalesTil", forventet.utbetalesTil?.id, faktisk.utbetalesTil?.id, avvik)
        leggTilDesimalEndret("$sti.beloepTilUtbetaling", forventet.delberegning.sumOf { it.sum }, faktisk.delberegning.sumOf { it.sum }, avvik)

        sammenlignLister(
            sti = "$sti.delberegninger",
            forventet = forventet.delberegning,
            faktisk = faktisk.delberegning,
            avvik = avvik,
            fjernetVerdi = { "missing in generated" },
            lagtTilVerdi = { "missing in referenced" },
        ) { delberegningSti, venstre, hoyre ->
            sammenlignDelberegning(delberegningSti, venstre, hoyre, avvik)
        }
        return avvik
    }

    private fun sammenlignDelberegning(
        sti: String,
        forventet: Delberegning,
        faktisk: Delberegning,
        avvik: MutableList<BeregningAvvik>,
    ) {
        leggTilEndret("$sti.datoIntervall", forventet.datoIntervall, faktisk.datoIntervall, avvik)
        leggTilEndret("$sti.kolonner", forventet.datoIntervall.dager.size, faktisk.datoIntervall.dager.size, avvik)
        sammenlignLister(
            sti = "$sti.rows",
            forventet = forventet.linjer,
            faktisk = faktisk.linjer,
            avvik = avvik,
            fjernetVerdi = ::radNavn,
            lagtTilVerdi = ::radNavn,
        ) { radSti, venstre, hoyre ->
            sammenlignLinje(radSti, venstre, hoyre, avvik)
        }
        sammenlignDesimaler("$sti.sumColumn", forventet.sumColumn, faktisk.sumColumn, avvik)
        sammenlignLinje("$sti.sums", forventet.sums, faktisk.sums, avvik)
    }

    private fun sammenlignLinje(
        sti: String,
        forventet: Delberegningslinje,
        faktisk: Delberegningslinje,
        avvik: MutableList<BeregningAvvik>,
    ) {
        leggTilEndret("$sti.rowName", radNavn(forventet), radNavn(faktisk), avvik)
        leggTilDesimalEndret("$sti.rowValue", forventet.beloep, faktisk.beloep, avvik)
        sammenlignDesimaler("$sti.columnValues", forventet.dagsbeloep.map { it.beloep }, faktisk.dagsbeloep.map { it.beloep }, avvik)
    }

    private fun radNavn(linje: Delberegningslinje): String =
        if (linje.kodeKlasse != null) {
            "${linje.type.name.lowercase().replaceFirstChar(Char::uppercase)} ${linje.kodeKlasse.visningskode}"
        } else {
            linje.type.name
                .lowercase()
                .replaceFirstChar(Char::uppercase)
        }

    private fun sammenlignDesimaler(
        sti: String,
        forventet: List<BigDecimal>,
        faktisk: List<BigDecimal>,
        avvik: MutableList<BeregningAvvik>,
    ) {
        val felles = minOf(forventet.size, faktisk.size)
        for (indeks in 0 until felles) {
            leggTilDesimalEndret("$sti[$indeks]", forventet[indeks], faktisk[indeks], avvik)
        }
        for (indeks in felles until forventet.size) {
            avvik += BeregningAvvik("$sti[$indeks]", BeregningAvvikType.FJERNET, forventet[indeks].toPlainString(), null)
        }
        for (indeks in felles until faktisk.size) {
            avvik += BeregningAvvik("$sti[$indeks]", BeregningAvvikType.LAGT_TIL, null, faktisk[indeks].toPlainString())
        }
    }

    private fun <T> sammenlignLister(
        sti: String,
        forventet: List<T>,
        faktisk: List<T>,
        avvik: MutableList<BeregningAvvik>,
        fjernetVerdi: (T) -> String,
        lagtTilVerdi: (T) -> String,
        sammenlign: (String, T, T) -> Unit,
    ) {
        val felles = minOf(forventet.size, faktisk.size)
        for (indeks in 0 until felles) {
            sammenlign("$sti[$indeks]", forventet[indeks], faktisk[indeks])
        }
        for (indeks in felles until forventet.size) {
            avvik += BeregningAvvik("$sti[$indeks]", BeregningAvvikType.FJERNET, fjernetVerdi(forventet[indeks]), null)
        }
        for (indeks in felles until faktisk.size) {
            avvik += BeregningAvvik("$sti[$indeks]", BeregningAvvikType.LAGT_TIL, null, lagtTilVerdi(faktisk[indeks]))
        }
    }

    private fun leggTilDesimalEndret(
        sti: String,
        forventet: BigDecimal,
        faktisk: BigDecimal,
        avvik: MutableList<BeregningAvvik>,
    ) {
        if (forventet.compareTo(faktisk) != 0) {
            avvik += BeregningAvvik(sti, BeregningAvvikType.ENDRET, forventet.toPlainString(), faktisk.toPlainString())
        }
    }

    private fun leggTilEndret(
        sti: String,
        forventet: Any?,
        faktisk: Any?,
        avvik: MutableList<BeregningAvvik>,
    ) {
        if (forventet != faktisk) {
            avvik += BeregningAvvik(sti, BeregningAvvikType.ENDRET, forventet?.toString(), faktisk?.toString())
        }
    }

    private sealed interface TreffResultat {
        data class Funnet(
            val indeks: Int,
        ) : TreffResultat

        data class Tvetydig(
            val indekser: List<Int>,
            val noekkel: String,
        ) : TreffResultat

        data class Uloest(
            val indeks: Int,
            val noekkel: String,
        ) : TreffResultat

        data object IkkeFunnet : TreffResultat
    }

    companion object {
        private const val UNOEKLET_FALLBACK_NOEKKEL = "deterministic fallback for structurally unkeyed calculation"

        fun sammenlign(
            forventet: List<Beregning>,
            faktisk: List<Beregning>,
        ): BeregningSammenligningsResultat = BeregningSammenligner().sammenlign(forventet, faktisk)
    }
}
