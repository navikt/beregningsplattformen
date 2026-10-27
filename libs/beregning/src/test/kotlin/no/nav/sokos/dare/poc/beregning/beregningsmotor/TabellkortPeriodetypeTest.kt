package no.nav.sokos.dare.poc.beregning.beregningsmotor

import java.math.BigDecimal

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

import no.nav.sokos.dare.poc.beregning.skatt.SkattTabellDagerPeriode
import no.nav.sokos.dare.poc.beregning.skatt.TabellkortPeriodetype
import no.nav.sokos.dare.poc.trekk.FagomraadeKode

class TabellkortPeriodetypeTest :
    FunSpec({
        test("verdiberegnet grupperer perioder med lik vedtakssats før dager summeres") {
            val dager =
                TabellkortPeriodetype.VERDIBEREGNET.beregnAntallDager(
                    listOf(
                        SkattTabellDagerPeriode(BigDecimal("1000"), BigDecimal("1000"), 1, 1),
                        SkattTabellDagerPeriode(BigDecimal("1000"), BigDecimal("1000"), 1, 1),
                        SkattTabellDagerPeriode(BigDecimal("2000"), BigDecimal("2000"), 1, 1),
                        SkattTabellDagerPeriode(BigDecimal("3000"), BigDecimal("3000"), 1, 1),
                    ),
                )

            dager shouldBe 5
        }

        test("kalenderberegnet summerer kalenderdager for periodene") {
            val dager =
                TabellkortPeriodetype.KALENDERBEREGNET.beregnAntallDager(
                    listOf(
                        SkattTabellDagerPeriode(BigDecimal("1000"), BigDecimal("1000"), 1, 1),
                        SkattTabellDagerPeriode(BigDecimal("1000"), BigDecimal("1000"), 2, 2),
                        SkattTabellDagerPeriode(BigDecimal("2000"), BigDecimal("2000"), 3, 3),
                    ),
                )

            dager shouldBe 6
        }

        test("fagomraade eksponerer forventet tabell-dager-algoritme") {
            listOf(
                FagomraadeKode.AAP.tabellkortPeriodetype,
                FagomraadeKode.SP.tabellkortPeriodetype,
                FagomraadeKode.PENAP.tabellkortPeriodetype,
            ) shouldContainExactlyInAnyOrder
                listOf(
                    TabellkortPeriodetype.VERDIBEREGNET,
                    TabellkortPeriodetype.KALENDERBEREGNET,
                    TabellkortPeriodetype.KALENDERBEREGNET,
                )
        }
    })
