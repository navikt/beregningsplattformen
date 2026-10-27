package no.nav.sokos.dare.poc.beregning.integrasjon

import java.math.BigDecimal

import no.nav.sokos.dare.poc.beregning.Delberegning.Companion.COLUMN_HEADER_WIDTH
import no.nav.sokos.dare.poc.beregning.Delberegning.Companion.COLUMN_WIDTH
import no.nav.sokos.dare.poc.beregning.DelberegningType
import no.nav.sokos.dare.poc.beregning.EkstraInfo
import no.nav.sokos.dare.poc.beregning.MASK
import no.nav.sokos.dare.poc.beregning.TekstEkstraInfo
import no.nav.sokos.dare.poc.beregning.dto.BeregningsmotorKontekstPresentasjon
import no.nav.sokos.dare.poc.beregning.dto.DelBeregningsPresentasjonV2
import no.nav.sokos.dare.poc.beregning.dto.Linje

// Dette er vibekodet bare for at jeg skal kunne se på resultater i test.
// Ikke bruk dette formatet til noe viktig!

object BeregningsPresentasjonRenderer {
    fun BeregningsmotorKontekstPresentasjon.render(): String =
        buildString {
            appendLine("=".repeat(80))
            appendLine("BEREGNINGSMOTORKONTEKST ETTER BEREGNING")
            appendLine("=".repeat(80))
            appendLine()

            appendLine("Periode: ${periode.fom} -> ${periode.tom}")
            appendLine()

            renderBeregninger(this, this@render)
            renderSaldoBoetter(this, this@render)
            renderSatsBoetter(this, this@render)
            appendLine("Rest frikortbeløp: ${restFrikortBeloep?.toPlainString() ?: "ikke satt"}")
            appendLine()
        }

    private fun renderBeregninger(
        sb: StringBuilder,
        presentasjon: BeregningsmotorKontekstPresentasjon,
    ) {
        sb.appendLine("-".repeat(80))
        sb.appendLine("BEREGNINGER (${presentasjon.beregninger.size} stk)")
        sb.appendLine("-".repeat(80))
        if (presentasjon.beregninger.isEmpty()) {
            sb.appendLine("  (ingen)")
        } else {
            sb.appendLine()
            presentasjon.beregninger.forEachIndexed { idx, beregning ->
                sb.appendLine("  Beregning #${idx + 1}")
                sb.appendLine("    BeregningId: ${beregning.id?.id ?: "-"}")
                sb.appendLine("    Periode:     ${beregning.periode.fom} -> ${beregning.periode.tom}")
                sb.appendLine("    Status:      ${beregning.status}")
                if (beregning.statusflagg.isNotEmpty()) sb.appendLine("    Statusflagg:       ${beregning.statusflagg.joinToString(", ")}")
                sb.appendLine("    Fagområde:   ${beregning.fagomraade}")
                sb.appendLine("    Gjelder:     ${beregning.gjelder.id}")
                sb.appendLine("    Overføres:   ${beregning.overfoeres}")
                sb.appendLine("    Beregnet:    ${beregning.beregnet}")
                beregning.utbetalesTil?.let { sb.appendLine("    UtbetTil:    ${it.id}") }
                sb.appendLine()

                beregning.delberegninger.forEachIndexed { dIdx, del ->
                    renderDelberegning(sb, dIdx, del)
                }
            }
        }
        sb.appendLine()
    }

    private fun renderDelberegning(
        sb: StringBuilder,
        idx: Int,
        del: DelBeregningsPresentasjonV2,
    ) {
        sb.appendLine("    Delberegning #${idx + 1}")
        sb.appendLine("      OppdragId:  ${del.oppdragId?.id ?: "-"}")
        sb.appendLine("      Periode:    ${del.datoIntervall.fom} -> ${del.datoIntervall.tom}")
        sb.appendLine("      Kolonner:   ${del.columns.size} dager")
        if (del.ekstraInfo.isNotEmpty()) {
            sb.appendLine("      EkstraInfo:")
            del.ekstraInfo.forEach { renderEkstraInfo(sb, it, "        ") }
        }
        sb.appendLine()
        sb.appendLine("      Linjer (${del.rows.size} stk):")
        sb.append("      ")
        sb.append(" ".repeat(COLUMN_HEADER_WIDTH))
        sb.append(del.columns.joinToString("") { "%02d-%02d".format(it.monthValue, it.dayOfMonth).padStart(COLUMN_WIDTH) })
        sb.append("beløp".padStart(COLUMN_WIDTH))
        sb.appendLine("sum".padStart(COLUMN_WIDTH))
        del.rows.forEach { linje -> renderLinje(sb, linje, del.columns.size) }
        sb.appendLine("      ${"─".repeat(40)}")
        sb.appendLine("      ${del.sums.format(del.columns.size).trimEnd()}")
        sb.appendLine()
    }

    private fun renderLinje(
        sb: StringBuilder,
        linje: Linje,
        kolonner: Int,
    ) {
        sb.append("      ")
        sb.appendLine(linje.format(kolonner).trimEnd())
        if (linje.type == DelberegningType.TREKK && linje.trekkVedtakId != null) {
            sb.appendLine("        trekkvedtakId=${linje.trekkVedtakId}")
        }
        if (linje.ekstra.isNotEmpty()) {
            linje.ekstra.forEach { renderEkstraInfo(sb, it, "        ") }
        }
    }

    private fun Linje.format(columns: Int): String =
        buildString {
            append(rowName.padEnd(COLUMN_HEADER_WIDTH))
            val hasColumnValues = columnValues.isNotEmpty() && columnValues.any { it.compareTo(BigDecimal.ZERO) != 0 }
            if (hasColumnValues) {
                append(columnValues.joinToString("") { it.toPlainString().padStart(COLUMN_WIDTH) })
            } else {
                append(" ".repeat(COLUMN_WIDTH * columns))
            }
            val sum = if (hasColumnValues) columnValues.fold(BigDecimal.ZERO, BigDecimal::add) + rowValue else rowValue
            append(rowValue.toPlainString().padStart(COLUMN_WIDTH))
            append(sum.toPlainString().padStart(COLUMN_WIDTH)).append('\n')
        }

    private fun renderSaldoBoetter(
        sb: StringBuilder,
        presentasjon: BeregningsmotorKontekstPresentasjon,
    ) {
        sb.appendLine("-".repeat(80))
        sb.appendLine("SALDOBØTTER")
        sb.appendLine("-".repeat(80))
        val initielle = presentasjon.initielleSaldoBoetter.associateBy { it.trekkvedtakId }
        val resulterende = presentasjon.saldoBoetter.associateBy { it.trekkvedtakId }
        val alleIder = (initielle.keys + resulterende.keys).distinct()
        if (alleIder.isEmpty()) {
            sb.appendLine("  (ingen)")
        } else {
            alleIder.forEach { trekkvedtakId ->
                val initial = initielle[trekkvedtakId]?.restSaldo
                val rest = resulterende[trekkvedtakId]?.restSaldo
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
                sb.appendLine("  TrekkvedtakId=$trekkvedtakId  $detaljer")
            }
        }
        sb.appendLine()
    }

    private fun renderSatsBoetter(
        sb: StringBuilder,
        presentasjon: BeregningsmotorKontekstPresentasjon,
    ) {
        sb.appendLine("-".repeat(80))
        sb.appendLine("SATSBØTTER")
        sb.appendLine("-".repeat(80))
        val initielle = presentasjon.initielleSatsBoetter.associateBy { it.trekkperiodeId to it.boette }
        val resulterende = presentasjon.satsBoetter.associateBy { it.trekkperiodeId to it.boette }
        val alleIder = (initielle.keys + resulterende.keys).distinct()
        if (alleIder.isEmpty()) {
            sb.appendLine("  (ingen)")
        } else {
            alleIder.forEach { id ->
                val initial = initielle[id]?.restSats
                val rest = resulterende[id]?.restSats
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
                sb.appendLine("  TrekkperiodeId=${id.first}  bøtte=${id.second}  $detaljer")
            }
        }
        sb.appendLine()
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
        val trekktypeMatch = Regex("""Trekktype\(kodeTrekktype=(\w+), beskrivelse=([^,]+),.*""").find(value)
        if (trekktypeMatch != null) {
            val (kode, beskr) = trekktypeMatch.destructured
            return "$kode/$beskr"
        }
        val periodeMatch =
            Regex("""Trekkperiode\(.*datoFom=([^,]+), datoTom=([^,]+), sats=([^,]+), satstype=(\w+).*\)""").find(value)
        if (periodeMatch != null) {
            val (fom, tom, sats, satstype) = periodeMatch.destructured
            return "sats=$sats/$satstype, $fom→$tom"
        }
        return value
    }
}
