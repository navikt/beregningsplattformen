package no.nav.sokos.dare.poc.beregning.integrasjon

import java.io.File

import no.nav.sokos.dare.poc.beregning.Beregning
import no.nav.sokos.dare.poc.beregning.fordeler.SkatteKontekst
import no.nav.sokos.dare.poc.oppdrag.Oppdrag
import no.nav.sokos.dare.poc.oppdrag.YtelseKlasseKode
import no.nav.sokos.dare.poc.oppdrag25.Oppdrag110
import no.nav.sokos.dare.poc.trekk.Trekkvedtak

object OppsettDumper {
    fun dump(
        testKontekst: TestKontekst,
        oppdrag: List<Oppdrag>,
        oppdragInput: List<Oppdrag110>,
        trekk: List<Trekkvedtak>,
        skatteKontekst: SkatteKontekst,
        relaterteBeregninger: List<Beregning>,
        klassekoder: List<YtelseKlasseKode>,
        dir: File,
        filnavn: String,
    ) {
        dir.mkdirs()
        val text = render(testKontekst, oppdrag, oppdragInput, trekk, skatteKontekst, relaterteBeregninger, klassekoder)
        File(dir, filnavn).writeText(text)
    }

    fun render(
        testKontekst: TestKontekst,
        oppdrag: List<Oppdrag>,
        oppdragInput: List<Oppdrag110>,
        trekk: List<Trekkvedtak>,
        skatteKontekst: SkatteKontekst,
        relaterteBeregninger: List<Beregning>,
        klassekoder: List<YtelseKlasseKode> = emptyList(),
    ): String =
        buildString {
            appendLine("=".repeat(80))
            appendLine("OPPSETT (input til beregning)")
            appendLine("=".repeat(80))
            appendLine()

            renderTestKontekst(this, testKontekst)
            renderOppdrag(this, oppdrag, oppdragInput, klassekoder)
            renderTrekk(this, trekk)
            renderSkatteKontekst(this, skatteKontekst)
            renderRelaterteBeregninger(this, relaterteBeregninger)
        }

    private fun renderTestKontekst(
        sb: StringBuilder,
        testKontekst: TestKontekst,
    ) {
        sb.appendLine("-".repeat(80))
        sb.appendLine("KONTEKST")
        sb.appendLine("-".repeat(80))
        sb.appendLine("  Gjelder:          ${testKontekst.gjelder.id}")
        sb.appendLine("  UtbetalesTil:     ${testKontekst.utbetalesTil?.id ?: "(ingen)"}")
        sb.appendLine("  Periode:          ${testKontekst.periode.fom} -> ${testKontekst.periode.tom}")
        sb.appendLine("  Beregnet:         ${testKontekst.beregnet}")
        sb.appendLine("  Overfoeringsdato: ${testKontekst.overfoeringsdato}")
        sb.appendLine()
    }

    private fun renderOppdrag(
        sb: StringBuilder,
        oppdrag: List<Oppdrag>,
        oppdragInput: List<Oppdrag110>,
        klassekoder: List<YtelseKlasseKode>,
    ) {
        val klassekodeMap = klassekoder.associateBy { it.kode }
        sb.appendLine("-".repeat(80))
        sb.appendLine("OPPDRAG (${oppdrag.size} stk)")
        sb.appendLine("-".repeat(80))
        if (oppdrag.isEmpty()) {
            sb.appendLine("  (ingen)")
        } else {
            oppdrag.forEachIndexed { idx, oppdragPeriode ->
                val oppdragRawInput = oppdragInput.getOrNull(idx)
                sb.appendLine("  Oppdrag #${idx + 1}")
                sb.appendLine("    OppdragId:   ${oppdragPeriode.id?.id ?: "-"}")
                sb.appendLine("    FagsystemId: ${oppdragPeriode.fagsystemId}")
                sb.appendLine("    Fagområde:   ${oppdragPeriode.fagomraade}")
                sb.appendLine("    Linjer (${oppdragPeriode.alleOppdragslinjer.size} stk):")
                oppdragPeriode.alleOppdragslinjer.forEach { linje ->
                    val beskrivelse = klassekodeMap[linje.kodeKlasse.kode]?.beskrivelse?.let { " ($it)" } ?: ""
                    sb.appendLine("      - LinjeId=${linje.linjeId}  kodeKlasse=${linje.kodeKlasse.kode}$beskrivelse  sats=${linje.sats}  fom=${linje.datoVedtakFom}  tom=${linje.datoVedtakTom}")
                }
                if (!oppdragRawInput?.avvent118.isNullOrEmpty()) {
                    sb.appendLine("    Avvent118 (rå input) (${oppdragRawInput!!.avvent118.size} stk):")
                    oppdragRawInput.avvent118.forEach { avvent ->
                        sb.appendLine(
                            "      - årsak=${avvent.kodeArsak ?: "-"}  " +
                                "fom=${avvent.datoAvventFom ?: "-"}  " +
                                "tom=${avvent.datoAvventTom ?: "-"}  " +
                                "overføres=${avvent.datoOverfores ?: "-"}  " +
                                "feilreg=${avvent.feilreg ?: "-"}",
                        )
                    }
                }
                sb.appendLine()
            }
        }
        sb.appendLine()
    }

    private fun renderTrekk(
        sb: StringBuilder,
        trekk: List<Trekkvedtak>,
    ) {
        sb.appendLine("-".repeat(80))
        sb.appendLine("TREKK (${trekk.size} stk)")
        sb.appendLine("-".repeat(80))
        if (trekk.isEmpty()) {
            sb.appendLine("  (ingen)")
        } else {
            trekk.forEachIndexed { idx, t ->
                sb.appendLine("  Trekk #${idx + 1}")
                sb.appendLine("    TrekkvedtakId:  ${t.id?.id ?: "-"}")
                sb.appendLine("    Trekktype:      ${t.trekktypeKode}")
                sb.appendLine("    Beregningstype: ${t.trekktype?.trekkberegningstype ?: "-"}")
                sb.appendLine("    ReduserSkatt:   ${t.trekktype?.reduserSkattegr ?: "-"}")
                sb.appendLine("    TrekkAlt:       ${t.trekkalt ?: "-"}")
                sb.appendLine("    BelopSaldo:     ${t.belopSaldo ?: "-"}")
                sb.appendLine("    GyldigTom:      ${t.gyldigTom ?: "-"}")
                sb.appendLine("    Trekkperioder:  ${t.trekkperioder.size} stk")
                t.trekkperioder.forEach { tp ->
                    sb.appendLine("      - Id=${tp.id?.id ?: "-"}  sats=${tp.sats}  satstype=${tp.satstype}  fom=${tp.datoFom}  tom=${tp.datoTom ?: "-"}")
                }
                sb.appendLine()
            }
        }
        sb.appendLine()
    }

    private fun renderSkatteKontekst(
        sb: StringBuilder,
        skatteKontekst: SkatteKontekst,
    ) {
        sb.appendLine("-".repeat(80))
        sb.appendLine("SKATTEKONTEKST")
        sb.appendLine("-".repeat(80))
        sb.appendLine("  Skattekort:   ${skatteKontekst.skattekort ?: "(ingen)"}")
        if (skatteKontekst.allokeringer.isEmpty()) {
            sb.appendLine("  Allokeringer: (ingen)")
        } else {
            sb.appendLine("  Allokeringer:")
            skatteKontekst.allokeringer.forEach { (trekkode, faggruppe) ->
                sb.appendLine("    $trekkode -> $faggruppe")
            }
        }
        sb.appendLine()
    }

    private fun renderRelaterteBeregninger(
        sb: StringBuilder,
        relaterteBeregninger: List<Beregning>,
    ) {
        BeregningsmotorKontekstDumper.renderBeregninger(sb, relaterteBeregninger, "RELATERTE BEREGNINGER")
    }
}
