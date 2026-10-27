package no.nav.sokos.dare.poc.beregning.skatt

import java.math.BigDecimal

import kotlinx.serialization.Serializable

@Serializable
enum class TabellkortPeriodetype {
    VERDIBEREGNET {
        override fun beregnAntallDager(perioder: List<SkattTabellDagerPeriode>): Int =
            perioder
                .groupBy { it.vedtakssats.normalizedKey() }
                .values
                .map { perioderForPartition ->
                    perioderForPartition.fold(SkattTabellDagerPartition()) { partition, periode -> partition.leggTil(periode) }
                }.sumOf { partition -> partition.beregnDager() }
    },
    KALENDERBEREGNET {
        override fun beregnAntallDager(perioder: List<SkattTabellDagerPeriode>): Int = perioder.sumOf { it.antallKalenderdager }
    },
    ;

    abstract fun beregnAntallDager(perioder: List<SkattTabellDagerPeriode>): Int
}

data class SkattTabellDagerPeriode(
    val bruttoYtelse: BigDecimal,
    val vedtakssats: BigDecimal,
    val antallKalenderdager: Int,
    val antallArbeidsdager: Int,
)

private fun BigDecimal.normalizedKey(): BigDecimal = stripTrailingZeros()
