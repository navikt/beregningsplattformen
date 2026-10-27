package no.nav.sokos.dare.poc.beregning.integrasjon

import java.io.File

import no.nav.sokos.dare.poc.beregning.Beregning
import no.nav.sokos.dare.poc.beregning.Delberegning
import no.nav.sokos.dare.poc.beregning.DelberegningType
import no.nav.sokos.dare.poc.beregning.Delberegningslinje
import no.nav.sokos.dare.poc.beregning.EkstraInfo
import no.nav.sokos.dare.poc.beregning.MASK
import no.nav.sokos.dare.poc.beregning.TekstEkstraInfo
import no.nav.sokos.dare.poc.beregning.fordeler.BeregningsmotorKontekst

object BeregningsmotorKontekstDumper {
    fun dump(
        kontekst: BeregningsmotorKontekst,
        filnavn: String,
    ) {
        val outputDir = IntegrationTestConstants.outputDir
        outputDir.mkdirs()
        val text = render(kontekst)
        File(outputDir, filnavn).writeText(text)
    }

    fun dump(
        kontekst: BeregningsmotorKontekst,
        dir: File,
        filnavn: String,
    ) {
        dir.mkdirs()
        val text = render(kontekst)
        File(dir, filnavn).writeText(text)
    }

    fun render(kontekst: BeregningsmotorKontekst): String =
        buildString {
            appendLine("=".repeat(80))
            appendLine("BEREGNINGSMOTORKONTEKST ETTER BEREGNING")
            appendLine("=".repeat(80))
            appendLine()

            appendLine("Periode: ${kontekst.periode.fom} -> ${kontekst.periode.tom}")
            appendLine()

            renderBeregninger(this, kontekst.beregninger())
            renderSaldoBoetter(this, kontekst)
            renderSatsBoetter(this, kontekst)
            appendLine("Rest frikortbeløp: ${kontekst.restFrikortBeloep ?: "ikke satt"}")
            appendLine()
        }

    private fun renderSaldoBoetter(
        sb: StringBuilder,
        kontekst: BeregningsmotorKontekst,
    ) {
        sb.appendLine("-".repeat(80))
        sb.appendLine("SALDOBØTTER")
        sb.appendLine("-".repeat(80))
        val initielle = kontekst.saldoBoetter.innkommendeSaldoer
        val resulterende = kontekst.saldoBoetter.saldoer
        val alleIder = (initielle.keys + resulterende.keys).distinct()
        if (alleIder.isEmpty()) {
            sb.appendLine("  (ingen)")
        } else {
            alleIder.forEach { trekkvedtakId ->
                val initial = initielle[trekkvedtakId]
                val rest = resulterende[trekkvedtakId]
                val detaljer =
                    when {
                        initial != null && rest != null && initial.compareTo(rest) != 0 ->
                            "restSaldo=${rest.toPlainString()} (initiell=${initial.toPlainString()}, trukket=${(initial - rest).toPlainString()})"
                        initial != null && rest != null ->
                            "restSaldo=${rest.toPlainString()}"
                        initial != null ->
                            "FJERNET (initiell=${initial.toPlainString()})"
                        else ->
                            "restSaldo=${rest!!.toPlainString()} (ny under beregning)"
                    }
                sb.appendLine("  TrekkvedtakId=${trekkvedtakId.id}  $detaljer")
            }
        }
        sb.appendLine()
    }

    private fun renderSatsBoetter(
        sb: StringBuilder,
        kontekst: BeregningsmotorKontekst,
    ) {
        sb.appendLine("-".repeat(80))
        sb.appendLine("SATSBØTTER")
        sb.appendLine("-".repeat(80))
        val initielle = kontekst.satsBoetter.innkommendeSatser
        val resulterende = kontekst.satsBoetter.satser
        val alleIder = (initielle.keys + resulterende.keys).distinct()
        if (alleIder.isEmpty()) {
            sb.appendLine("  (ingen)")
        } else {
            alleIder.forEach { id ->
                val initial = initielle[id]
                val rest = resulterende[id]
                val detaljer =
                    when {
                        initial != null && rest != null && initial.compareTo(rest) != 0 ->
                            "restSats=${rest.toPlainString()} (initiell=${initial.toPlainString()}, trukket=${(initial - rest).toPlainString()})"
                        initial != null && rest != null ->
                            "restSats=${rest.toPlainString()}"
                        initial != null ->
                            "FJERNET (initiell=${initial.toPlainString()})"
                        else ->
                            "restSats=${rest!!.toPlainString()} (ny under beregning)"
                    }
                sb.appendLine("  TrekkperiodeId=${id.id.id}  bøtte=${id.boette}  $detaljer")
            }
        }
        sb.appendLine()
    }

    fun renderBeregninger(
        sb: StringBuilder,
        beregninger: List<Beregning>,
        header: String = "BEREGNINGER",
    ) {
        sb.appendLine("-".repeat(80))
        sb.appendLine("$header (${beregninger.size} stk)")
        sb.appendLine("-".repeat(80))
        if (beregninger.isEmpty()) {
            sb.appendLine("  (ingen)")
        } else {
            sb.appendLine()
            beregninger.forEachIndexed { idx, beregning ->
                sb.appendLine("  Beregning #${idx + 1}")
                sb.appendLine("    BeregningId: ${beregning.id?.id ?: "-"}")
                sb.appendLine("    Periode:     ${beregning.periode.fom} -> ${beregning.periode.tom}")
                sb.appendLine("    Status:      ${beregning.status}")
                if (beregning.statusflagg.isNotEmpty()) sb.appendLine("    Statusflagg:       ${beregning.statusflagg.joinToString(", ")}")
                sb.appendLine("    Konto:       ${beregning.konto}")
                sb.appendLine("    Fagområde:   ${beregning.fagomraade}")
                sb.appendLine("    Gjelder:     ${beregning.gjelder.id}")
                sb.appendLine("    Overføres:   ${beregning.overfoeres}")
                sb.appendLine("    Beregnet:    ${beregning.beregnet}")
                beregning.utbetalesTil?.let { sb.appendLine("    UtbetTil:    ${it.id}") }
                sb.appendLine()

                beregning.delberegning.forEachIndexed { dIdx, del ->
                    renderDelberegning(sb, dIdx, del)
                }
            }
        }
        sb.appendLine()
    }

    private fun renderDelberegning(
        sb: StringBuilder,
        idx: Int,
        del: Delberegning,
    ) {
        val columns = del.datoIntervall.dager
        sb.appendLine("    Delberegning #${idx + 1}")
        sb.appendLine("      OppdragId:  ${del.oppdragId?.id ?: "-"}")
        sb.appendLine("      Periode:    ${del.datoIntervall.fom} -> ${del.datoIntervall.tom}")
        sb.appendLine("      Kolonner:   ${columns.size} dager")
        if (del.ekstraInfo.isNotEmpty()) {
            sb.appendLine("      EkstraInfo:")
            del.ekstraInfo.forEach { renderEkstraInfo(sb, it, "        ") }
        }
        sb.appendLine()
        sb.appendLine("      Linjer (${del.linjer.size} stk):")
        sb.append("      ")
        sb.append(" ".repeat(Delberegning.COLUMN_HEADER_WIDTH))
        sb.append(columns.joinToString("") { "%02d-%02d".format(it.monthValue, it.dayOfMonth).padStart(Delberegning.COLUMN_WIDTH) })
        sb.append("beløp".padStart(Delberegning.COLUMN_WIDTH))
        sb.appendLine("sum".padStart(Delberegning.COLUMN_WIDTH))
        del.linjer.forEach { linje -> renderLinje(sb, linje, columns.size) }
        sb.appendLine("      ${"─".repeat(40)}")
        sb.appendLine("      ${del.sums.format(columns.size).trimEnd()}")
        sb.appendLine()
    }

    private fun renderLinje(
        sb: StringBuilder,
        linje: Delberegningslinje,
        kolonner: Int,
    ) {
        sb.append("      ")
        sb.appendLine(linje.format(kolonner).trimEnd())
        if (linje.type == DelberegningType.TREKK && linje.trekkvedtakId != null) {
            sb.appendLine("        trekkvedtakId=${linje.trekkvedtakId.id}")
        }
        if (linje.ekstra.isNotEmpty()) {
            linje.ekstra.forEach { renderEkstraInfo(sb, it, "        ") }
        }
    }

    private fun renderEkstraInfo(
        sb: StringBuilder,
        info: EkstraInfo,
        indent: String,
    ) {
        if (info is TekstEkstraInfo) {
            val verdierStr =
                if (info.verdier.isNotEmpty()) {
                    info.verdier.entries.joinToString(", ") { (k, v) ->
                        "$k=${compactValue(if (v.skjult) MASK else v.verdi)}"
                    }
                } else {
                    null
                }
            val parts = listOfNotNull(verdierStr, info.tekst?.takeIf { info.verdier.isEmpty() })
            val line = "$indent- [${info.grunn}]${if (parts.isNotEmpty()) " ${parts.joinToString(", ")}" else ""}"
            sb.appendLine(if (line.length > 250) line.take(247) + "..." else line)
            if (info.verdier.isNotEmpty() && info.grunn.template != null) {
                val resolved = info.toString()
                if (!resolved.contains("{")) {
                    sb.appendLine("$indent  $resolved")
                }
            }
        } else {
            sb.appendLine("$indent- $info")
        }
    }

    private fun compactValue(value: String): String {
        // Compact Trekktype(...) → "KRED/Kreditor"
        val trekktypeMatch = Regex("""Trekktype\(kodeTrekktype=(\w+), beskrivelse=([^,]+),.*""").find(value)
        if (trekktypeMatch != null) {
            val (kode, beskr) = trekktypeMatch.destructured
            return "$kode/$beskr"
        }
        // Compact Trekkperiode(...) → "sats=200/D, 2026-03-02→2026-03-06"
        val periodeMatch =
            Regex("""Trekkperiode\(.*datoFom=([^,]+), datoTom=([^,]+), sats=([^,]+), satstype=(\w+).*\)""").find(value)
        if (periodeMatch != null) {
            val (fom, tom, sats, satstype) = periodeMatch.destructured
            return "sats=$sats/$satstype, $fom→$tom"
        }
        return value
    }
}
