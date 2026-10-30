package no.nav.utbetaling.beregningsflyt

import java.math.BigDecimal

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

import no.nav.sokos.dare.poc.beregning.dto.BeregningsmotorKontekstPresentasjon
import no.nav.sokos.dare.poc.beregning.dto.Linje
import no.nav.sokos.dare.poc.beregning.dto.SaldoBoettePresentasjon
import no.nav.sokos.dare.poc.beregning.dto.SatsBoettePresentasjon
import no.nav.sokos.dare.poc.beregning.integrasjon.BeregningsmotorKontekstKomparator
import no.nav.sokos.dare.poc.oppdrag.OppdragslinjekjedeId

object ResultatSammenligning {
    fun sammenlign(
        faktisk: BeregningsmotorKontekstPresentasjon,
        forventet: BeregningsmotorKontekstPresentasjon,
    ) {
        BeregningsmotorKontekstKomparator.sammenlign(faktisk, forventet)
        val kjeder = Kjedematching()
        assertSoftly {
            sammenlignSaldoer(faktisk.saldoBoetter, forventet.saldoBoetter, "saldoBoetter")
            sammenlignSatser(faktisk.satsBoetter, forventet.satsBoetter, "satsBoetter")
            sammenlignSaldoer(faktisk.initielleSaldoBoetter, forventet.initielleSaldoBoetter, "initielleSaldoBoetter")
            sammenlignSatser(faktisk.initielleSatsBoetter, forventet.initielleSatsBoetter, "initielleSatsBoetter")
            faktisk.beregninger.zip(forventet.beregninger).forEachIndexed { b, (fakt, forv) ->
                withClue("beregninger[$b]") {
                    fakt.id shouldBe forv.id
                    fakt.beregnet shouldBe forv.beregnet
                    fakt.ekstraInfo shouldBe forv.ekstraInfo
                    fakt.delberegninger.zip(forv.delberegninger).forEachIndexed { d, (faktDel, forvDel) ->
                        withClue("delberegninger[$d]") {
                            faktDel.rows.zip(forvDel.rows).forEachIndexed { r, (faktRad, forvRad) ->
                                withClue("rows[$r]") { sammenlignRad(faktRad, forvRad, kjeder) }
                            }
                            withClue("sums") { sammenlignRad(faktDel.sums, forvDel.sums, kjeder) }
                            withClue("sumColumn") { sammenlignBeloep(faktDel.sumColumn, forvDel.sumColumn) }
                        }
                    }
                }
            }
        }
    }

    private fun sammenlignRad(
        faktisk: Linje,
        forventet: Linje,
        kjeder: Kjedematching,
    ) {
        faktisk.type shouldBe forventet.type
        faktisk.rowName shouldBe forventet.rowName
        sammenlignBeloep(faktisk.columnValues, forventet.columnValues)
        sammenlignBeloep(faktisk.rowValue, forventet.rowValue)
        faktisk.ekstra shouldBe forventet.ekstra
        faktisk.satstype shouldBe forventet.satstype
        sammenlignBeloep(faktisk.sats, forventet.sats)
        faktisk.trekkVedtakId shouldBe forventet.trekkVedtakId
        faktisk.skattType shouldBe forventet.skattType
        faktisk.reskontroKonto shouldBe forventet.reskontroKonto
        faktisk.kodeKlasse shouldBe forventet.kodeKlasse
        withClue("capacity") {
            val kapasitet = forventet.capacity
            if (kapasitet == null) {
                faktisk.capacity shouldBe null
            } else {
                (faktisk.capacity != null) shouldBe true
                withClue("dayCapacity") { sammenlignBeloep(faktisk.capacity?.dayCapacity, kapasitet.dayCapacity) }
                withClue("lumpCapacity") { sammenlignBeloep(faktisk.capacity?.lumpCapacity, kapasitet.lumpCapacity) }
            }
        }
        val forventetKjede = forventet.oppdragslinjekjedeId
        if (forventetKjede == null) {
            withClue("oppdragslinjekjedeId") { faktisk.oppdragslinjekjedeId shouldBe null }
        } else {
            kjeder.sammenlign(faktisk.oppdragslinjekjedeId, forventetKjede)
        }
    }

    private fun sammenlignSaldoer(
        faktisk: List<SaldoBoettePresentasjon>,
        forventet: List<SaldoBoettePresentasjon>,
        felt: String,
    ) {
        withClue(felt) {
            faktisk shouldHaveSize forventet.size
            faktisk.zip(forventet).forEach { (fakt, forv) ->
                fakt.trekkvedtakId shouldBe forv.trekkvedtakId
                sammenlignBeloep(fakt.restSaldo, forv.restSaldo)
                sammenlignBeloep(fakt.opprinneligSaldo, forv.opprinneligSaldo)
            }
        }
    }

    private fun sammenlignSatser(
        faktisk: List<SatsBoettePresentasjon>,
        forventet: List<SatsBoettePresentasjon>,
        felt: String,
    ) {
        withClue(felt) {
            faktisk shouldHaveSize forventet.size
            faktisk.zip(forventet).forEach { (fakt, forv) ->
                fakt.trekkperiodeId shouldBe forv.trekkperiodeId
                fakt.boette shouldBe forv.boette
                sammenlignBeloep(fakt.restSats, forv.restSats)
                sammenlignBeloep(fakt.opprinneligSats, forv.opprinneligSats)
            }
        }
    }

    private fun sammenlignBeloep(
        faktisk: BigDecimal?,
        forventet: BigDecimal?,
    ) {
        if (forventet == null) {
            faktisk shouldBe null
        } else {
            faktisk?.compareTo(forventet) shouldBe 0
        }
    }

    private fun sammenlignBeloep(
        faktisk: List<BigDecimal>?,
        forventet: List<BigDecimal>?,
    ) {
        if (forventet == null) {
            faktisk shouldBe null
        } else {
            faktisk?.size shouldBe forventet.size
            faktisk.orEmpty().zip(forventet).forEach { (fakt, forv) -> sammenlignBeloep(fakt, forv) }
        }
    }

    private class Kjedematching {
        private val forover = linkedMapOf<OppdragslinjekjedeId, OppdragslinjekjedeId>()
        private val bakover = linkedMapOf<OppdragslinjekjedeId, OppdragslinjekjedeId>()

        fun sammenlign(
            faktisk: OppdragslinjekjedeId?,
            forventet: OppdragslinjekjedeId,
        ) {
            withClue("oppdragslinjekjedeId (bijective encounter-order identity)") {
                (faktisk != null) shouldBe true
                if (faktisk != null) {
                    forover.getOrPut(forventet) { faktisk } shouldBe faktisk
                    bakover.getOrPut(faktisk) { forventet } shouldBe forventet
                    // Matched first encounters assign the same canonical label on both sides.
                    forover.keys.indexOf(forventet) shouldBe bakover.keys.indexOf(faktisk)
                }
            }
        }
    }
}
