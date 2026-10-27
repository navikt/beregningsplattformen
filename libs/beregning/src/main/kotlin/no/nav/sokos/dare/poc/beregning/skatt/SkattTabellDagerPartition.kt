package no.nav.sokos.dare.poc.beregning.skatt

import java.math.BigDecimal
import java.math.RoundingMode

internal data class SkattTabellDagerPartition(
    val bruttoYtelse: BigDecimal = BigDecimal.ZERO,
    val vedtakssats: BigDecimal? = null,
) {
    fun leggTil(periode: SkattTabellDagerPeriode): SkattTabellDagerPartition =
        copy(
            bruttoYtelse = bruttoYtelse + periode.bruttoYtelse,
            vedtakssats = vedtakssats ?: periode.vedtakssats,
        )

    fun beregnDager(): Int {
        requireNotNull(vedtakssats) { "Vedtakssats mangler for verdiberegnet tabellkortdager" }
        require(vedtakssats > BigDecimal.ZERO) {
            "Vedtakssats må være større enn null for verdiberegnet tabellkortdager"
        }

        return bruttoYtelse
            .divide(vedtakssats, 10, RoundingMode.HALF_UP)
            .multiply(BigDecimal("1.4"))
            .setScale(0, RoundingMode.HALF_UP)
            .toInt()
    }
}
