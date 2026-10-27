package no.nav.sokos.dare.poc.beregning.integrasjon

import java.io.File

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

import no.nav.sokos.dare.poc.beregning.DelberegningType
import no.nav.sokos.dare.poc.beregning.dto.BeregningsPresentasjonV2
import no.nav.sokos.dare.poc.beregning.dto.BeregningsmotorKontekstPresentasjon
import no.nav.sokos.dare.poc.beregning.dto.DelBeregningsPresentasjonV2
import no.nav.sokos.dare.poc.beregning.dto.Linje
import no.nav.sokos.dare.poc.beregning.dto.SaldoBoettePresentasjon
import no.nav.sokos.dare.poc.beregning.dto.SatsBoettePresentasjon

object BeregningsmotorKontekstKomparator {
    fun sammenlignMedFil(
        faktisk: BeregningsmotorKontekstPresentasjon,
        fil: File,
    ) {
        val forventet = BeregningsmotorKontekstFileRepository.les(fil)
        sammenlign(faktisk, forventet)
    }

    fun sammenlign(
        faktisk: BeregningsmotorKontekstPresentasjon,
        forventet: BeregningsmotorKontekstPresentasjon,
    ) {
        assertSoftly {
            withClue("periode") { faktisk.periode shouldBe forventet.periode }

            sammenlignBeregninger(faktisk.beregninger, forventet.beregninger)

            sammenlignSaldoBoetter(faktisk.saldoBoetter, forventet.saldoBoetter)
            sammenlignSatsBoetter(faktisk.satsBoetter, forventet.satsBoetter)

            withClue("restFrikortBeloep") {
                if (forventet.restFrikortBeloep != null) {
                    faktisk.restFrikortBeloep?.compareTo(forventet.restFrikortBeloep) shouldBe 0
                } else {
                    faktisk.restFrikortBeloep shouldBe null
                }
            }
        }
    }

    private fun sammenlignBeregninger(
        faktisk: List<BeregningsPresentasjonV2>,
        forventet: List<BeregningsPresentasjonV2>,
    ) {
        withClue("Antall beregninger") {
            faktisk shouldHaveSize forventet.size
        }
        forventet.zip(faktisk).forEachIndexed { idx, (forv, fakt) ->
            val prefix = "Beregning #${idx + 1}"
            withClue("$prefix: periode") { fakt.periode shouldBe forv.periode }
            withClue("$prefix: status") { fakt.status shouldBe forv.status }
            withClue("$prefix: statusflagg") { fakt.statusflagg shouldBe forv.statusflagg }
            withClue("$prefix: overfoeres") { fakt.overfoeres shouldBe forv.overfoeres }
            withClue("$prefix: fagomraade") { fakt.fagomraade shouldBe forv.fagomraade }
            withClue("$prefix: gjelder") { fakt.gjelder shouldBe forv.gjelder }
            withClue("$prefix: utbetalesTil") { fakt.utbetalesTil shouldBe forv.utbetalesTil }
            withClue("$prefix: beloepTilUtbetaling (faktisk=${fakt.beloepTilUtbetaling}, forventet=${forv.beloepTilUtbetaling})") {
                fakt.beloepTilUtbetaling.compareTo(forv.beloepTilUtbetaling) shouldBe 0
            }
            withClue("$prefix: antall delberegninger") {
                fakt.delberegninger shouldHaveSize forv.delberegninger.size
            }
            forv.delberegninger.zip(fakt.delberegninger).forEachIndexed { dIdx, (forvDel, faktDel) ->
                sammenlignDelberegning(faktDel, forvDel, "$prefix > Delberegning #${dIdx + 1}")
            }
        }
    }

    private fun sammenlignDelberegning(
        faktisk: DelBeregningsPresentasjonV2,
        forventet: DelBeregningsPresentasjonV2,
        prefix: String,
    ) {
        withClue("$prefix: oppdragId") { faktisk.oppdragId shouldBe forventet.oppdragId }
        withClue("$prefix: datoIntervall") { faktisk.datoIntervall shouldBe forventet.datoIntervall }
        withClue("$prefix: columns") { faktisk.columns shouldBe forventet.columns }
        withClue("$prefix: ekstraInfo") { faktisk.ekstraInfo shouldBe forventet.ekstraInfo }
        withClue("$prefix: antall linjer") {
            faktisk.rows shouldHaveSize forventet.rows.size
        }
        forventet.rows.zip(faktisk.rows).forEachIndexed { lIdx, (forv, fakt) ->
            sammenlignLinje(fakt, forv, "$prefix > Linje #${lIdx + 1}")
        }
    }

    private fun sammenlignLinje(
        faktisk: Linje,
        forventet: Linje,
        prefix: String,
    ) {
        withClue("$prefix: type") { faktisk.type shouldBe forventet.type }
        withClue("$prefix: rowValue (faktisk=${faktisk.rowValue}, forventet=${forventet.rowValue})") {
            faktisk.rowValue.compareTo(forventet.rowValue) shouldBe 0
        }
        withClue("$prefix: satstype") { faktisk.satstype shouldBe forventet.satstype }
        withClue("$prefix: sats") {
            if (forventet.sats != null) {
                withClue("(faktisk=${faktisk.sats}, forventet=${forventet.sats})") {
                    faktisk.sats?.compareTo(forventet.sats) shouldBe 0
                }
            } else {
                faktisk.sats shouldBe null
            }
        }
        withClue("$prefix: ekstra") { faktisk.ekstra shouldBe forventet.ekstra }
        withClue("$prefix: trekkVedtakId") { faktisk.trekkVedtakId shouldBe forventet.trekkVedtakId }
        withClue("$prefix: skattType") { faktisk.skattType shouldBe forventet.skattType }
        if (forventet.type == DelberegningType.SKATT) {
            withClue("$prefix: forventet kodeKlasse må være satt") { forventet.kodeKlasse shouldNotBe null }
            withClue("$prefix: kodeKlasse") { faktisk.kodeKlasse shouldBe forventet.kodeKlasse }
            withClue("$prefix: forventet reskontroKonto må være satt") { forventet.reskontroKonto shouldNotBe null }
            withClue("$prefix: reskontroKonto") { faktisk.reskontroKonto shouldBe forventet.reskontroKonto }
        }
        withClue("$prefix: antall columnValues") {
            faktisk.columnValues.size shouldBe forventet.columnValues.size
        }
        forventet.columnValues.zip(faktisk.columnValues).forEachIndexed { cIdx, (forv, fakt) ->
            withClue("$prefix > Kolonne #${cIdx + 1}: beloep (faktisk=$fakt, forventet=$forv)") {
                fakt.compareTo(forv) shouldBe 0
            }
        }
    }

    private fun sammenlignSaldoBoetter(
        faktisk: List<SaldoBoettePresentasjon>,
        forventet: List<SaldoBoettePresentasjon>,
    ) {
        withClue("Antall saldobøtter") {
            faktisk shouldHaveSize forventet.size
        }
        forventet.zip(faktisk).forEachIndexed { idx, (forv, fakt) ->
            val prefix = "Saldobøtte #${idx + 1} (trekkvedtakId=${forv.trekkvedtakId})"
            withClue("$prefix: trekkvedtakId") { fakt.trekkvedtakId shouldBe forv.trekkvedtakId }
            withClue("$prefix: restSaldo (faktisk=${fakt.restSaldo}, forventet=${forv.restSaldo})") {
                fakt.restSaldo.compareTo(forv.restSaldo) shouldBe 0
            }
        }
    }

    private fun sammenlignSatsBoetter(
        faktisk: List<SatsBoettePresentasjon>,
        forventet: List<SatsBoettePresentasjon>,
    ) {
        withClue("Antall satsbøtter") {
            faktisk shouldHaveSize forventet.size
        }
        forventet.zip(faktisk).forEachIndexed { idx, (forv, fakt) ->
            val prefix = "Satsbøtte #${idx + 1} (trekkperiodeId=${forv.trekkperiodeId}, boette=${forv.boette})"
            withClue("$prefix: trekkperiodeId") { fakt.trekkperiodeId shouldBe forv.trekkperiodeId }
            withClue("$prefix: boette") { fakt.boette shouldBe forv.boette }
            withClue("$prefix: restSats (faktisk=${fakt.restSats}, forventet=${forv.restSats})") {
                fakt.restSats.compareTo(forv.restSats) shouldBe 0
            }
        }
    }
}
