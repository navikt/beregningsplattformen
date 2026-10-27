package no.nav.sokos.dare.poc.beregning.beregningsmotor

import java.math.BigDecimal
import java.time.LocalDate

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

import no.nav.sokos.dare.poc.beregning.BeloepPerDag
import no.nav.sokos.dare.poc.beregning.Dagbeloep
import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.beregning.DelberegningType
import no.nav.sokos.dare.poc.beregning.Delberegningslinje
import no.nav.sokos.dare.poc.beregning.JustertTrekk

class KapasitetStyringTest :
    FunSpec({
        fun dag(
            dato: String,
            beloep: String,
        ) = Dagbeloep(dato = LocalDate.parse(dato), beloep = BigDecimal(beloep))

        test("addYtelse should update current capacity and snapshot should include all days in periode") {
            val styring = KapasitetStyring()
            styring.addYtelse(
                listOf(
                    dag("2026-03-06", "100"),
                    dag("2026-03-07", "0"),
                    dag("2026-03-08", "0"),
                    dag("2026-03-09", "200"),
                ),
            )

            styring.current() shouldHaveSize 2

            val snapshot = styring.snapshot(DatoIntervall("2026-03-06", "2026-03-09"))
            snapshot.dagbeloep.keys.map(LocalDate::toString) shouldContainExactly
                listOf("2026-03-06", "2026-03-07", "2026-03-08", "2026-03-09")
            snapshot.dagbeloep[LocalDate.parse("2026-03-06")] shouldBe BigDecimal("100")
            snapshot.dagbeloep[LocalDate.parse("2026-03-07")] shouldBe BigDecimal.ZERO
            snapshot.dagbeloep[LocalDate.parse("2026-03-08")] shouldBe BigDecimal.ZERO
            snapshot.dagbeloep[LocalDate.parse("2026-03-09")] shouldBe BigDecimal("200")
        }

        test("applyTidligereUtbetalt should reduce capacity with weekday water fill semantics") {
            val styring = KapasitetStyring()
            styring.addYtelse(
                listOf(
                    dag("2026-03-02", "100"),
                    dag("2026-03-03", "200"),
                    dag("2026-03-04", "300"),
                ),
            )

            styring.applyTidligereUtbetalt(DatoIntervall("2026-03-02", "2026-03-04"), BigDecimal("150"))

            val snapshot = styring.snapshot(DatoIntervall("2026-03-02", "2026-03-04"))
            snapshot.dagbeloep.values.toList() shouldContainExactly
                listOf(BigDecimal("100"), BigDecimal("175"), BigDecimal("175"))
        }

        test("adjustTrekk should not mutate current capacity before consumeTrekk") {
            val styring = KapasitetStyring()
            styring.addYtelse(
                listOf(
                    dag("2026-03-02", "100"),
                    dag("2026-03-03", "100"),
                    dag("2026-03-04", "100"),
                ),
            )

            val periode = DatoIntervall("2026-03-02", "2026-03-04")
            val justert =
                styring.adjustTrekk(
                    periode,
                    BeloepPerDag(
                        mapOf(
                            LocalDate.parse("2026-03-02") to BigDecimal("150"),
                            LocalDate.parse("2026-03-03") to BigDecimal("150"),
                            LocalDate.parse("2026-03-04") to BigDecimal("150"),
                        ),
                    ),
                )

            justert.shouldBeInstanceOf<JustertTrekk>()
            styring
                .snapshot(periode)
                .dagbeloep.values
                .sumOf { it } shouldBe BigDecimal("300")

            val linje =
                Delberegningslinje.create(
                    periode = periode,
                    type = DelberegningType.TREKK,
                    kilde = justert,
                    ekstraInfo = emptySet(),
                    kapasitet = styring.snapshot(periode),
                )

            styring.consumeTrekk(periode, linje)
            styring.current().values.sumOf { it } shouldBe BigDecimal.ZERO
        }

        test("consumeSkatt should reduce capacity across weekdays only") {
            val styring = KapasitetStyring()
            styring.addYtelse(
                listOf(
                    dag("2026-03-06", "100"),
                    dag("2026-03-09", "200"),
                ),
            )

            styring.consumeSkatt(DatoIntervall("2026-03-06", "2026-03-09"), BigDecimal("50"))

            val snapshot = styring.snapshot(DatoIntervall("2026-03-06", "2026-03-09"))
            snapshot.dagbeloep.values.toList() shouldContainExactly
                listOf(BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal("150"))
        }
    })
