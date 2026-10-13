package no.nav.utbetaling.beregning.tid

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.utbetaling.beregning.tid.Periode.LukketPeriode.Companion.til
import no.nav.utbetaling.beregning.tid.Periode.ÅpenPeriode

class TidslinjesamlingTest :
    FunSpec({
        fun tidslinje(
            periode: Periode,
            verdi: String,
        ) = Tidslinje.Builder<String>().periode(periode, verdi).build()

        test("skal gi periode sammen med map fra nøkkel til verdi") {
            val samling =
                DefaultTidslinjesamling(
                    mapOf(
                        "a" to tidslinje(1.januar til 20.januar, "A"),
                        "b" to tidslinje(10.januar til 31.januar, "B"),
                    ),
                )

            samling.toList() shouldBe
                listOf(
                    (1.januar til 9.januar) to mapOf("a" to "A"),
                    (10.januar til 20.januar) to mapOf("a" to "A", "b" to "B"),
                    (21.januar til 31.januar) to mapOf("b" to "B"),
                )
        }

        test("periodeIterator skal beholde begge verdiene når to tidslinjer har lik verdi i samme periode") {
            val samling =
                DefaultTidslinjesamling(
                    mapOf(
                        "a" to tidslinje(januar, "lik"),
                        "b" to tidslinje(januar, "lik"),
                    ),
                )

            samling.periodeIterator().toList() shouldBe listOf(januar to listOf("lik", "lik"))
        }

        test("skal la den kryssende visningen være åpen når en av tidslinjene er åpen") {
            val samling =
                DefaultTidslinjesamling(
                    mapOf(
                        "a" to tidslinje(januar, "A"),
                        "b" to tidslinje(ÅpenPeriode(1.februar), "B"),
                    ),
                )

            samling.toList() shouldBe
                listOf(
                    januar to mapOf("a" to "A"),
                    ÅpenPeriode(1.februar) to mapOf("b" to "B"),
                )
        }

        test("skal slå opp gjeldende map på en dato") {
            val samling =
                DefaultTidslinjesamling(
                    mapOf(
                        "a" to tidslinje(januar, "A"),
                        "b" to tidslinje(mars, "B"),
                    ),
                )

            samling[15.januar] shouldBe mapOf("a" to "A")
            samling[15.februar] shouldBe emptyMap()
            (15.januar in samling) shouldBe true
            (15.februar in samling) shouldBe false
        }

        test("skal være tom når alle tidslinjene er tomme") {
            DefaultTidslinjesamling(mapOf("a" to Tidslinje.Builder<String>().build())).isEmpty() shouldBe true
        }

        test("periodeIterator skal gi flat visning likt en tidslinjesamling") {
            val samling =
                DefaultTidslinjesamling(
                    mapOf(
                        "a" to tidslinje(1.januar til 20.januar, "A"),
                        "b" to tidslinje(10.januar til 31.januar, "B"),
                    ),
                )

            samling.periodeIterator().toList() shouldBe
                listOf(
                    (1.januar til 9.januar) to listOf("A"),
                    (10.januar til 20.januar) to listOf("A", "B"),
                    (21.januar til 31.januar) to listOf("B"),
                )
        }

        test("konstruktør fra liste skal feile når to tidslinjer grupperes på samme nøkkel") {
            val a = tidslinje(januar, "a:100")
            val b = tidslinje(februar, "a:200")

            shouldThrow<IllegalArgumentException> {
                DefaultTidslinjesamling(listOf(a, b)) { it.substringBefore(":") }
            }
        }

        test("konstruktør fra liste skal nøkle hver tidslinje på verdien og ignorere tomme") {
            val a = tidslinje(januar, "a:100")
            val b = tidslinje(januar, "b:200")
            val tom = Tidslinje.Builder<String>().build()

            val samling =
                DefaultTidslinjesamling(listOf(a, b, tom)) { it.substringBefore(":") }

            samling[15.januar] shouldBe mapOf("a" to "a:100", "b" to "b:200")
        }

        test("konstruktør fra liste skal feile når én tidslinje grupperes på flere nøkler") {
            val blandet =
                Tidslinje
                    .Builder<String>()
                    .periode(1.januar til 15.januar, "a:1")
                    .periode(16.januar til 31.januar, "b:2")
                    .build()

            shouldThrow<IllegalArgumentException> {
                DefaultTidslinjesamling(listOf(blandet)) { it.substringBefore(":") }
            }
        }

        test("kombinerMed skal kombinere verdier per nøkkel") {
            val satser =
                DefaultTidslinjesamling(
                    mapOf(
                        "a" to tidslinje(januar, "100"),
                        "b" to tidslinje(januar, "200"),
                    ),
                )
            val tillegg =
                DefaultTidslinjesamling(
                    mapOf(
                        "a" to tidslinje(1.januar til 15.januar, "10"),
                        "b" to tidslinje(januar, "20"),
                    ),
                )

            val resultat =
                satser.kombinerMed(tillegg) { _, sats, ekstra ->
                    sats?.toInt()?.plus(ekstra?.toInt() ?: 0)
                }

            resultat.toList() shouldBe
                listOf(
                    (1.januar til 15.januar) to mapOf("a" to 110, "b" to 220),
                    (16.januar til 31.januar) to mapOf("a" to 100, "b" to 220),
                )
        }

        test("kombinerMed skal ta med nøkler som bare finnes i den ene samlingen") {
            val denne = DefaultTidslinjesamling(mapOf("a" to tidslinje(januar, "A")))
            val andre = DefaultTidslinjesamling(mapOf("b" to tidslinje(januar, "B")))

            val resultat = denne.kombinerMed(andre) { _, d, a -> d ?: a }

            resultat[15.januar] shouldBe mapOf("a" to "A", "b" to "B")
        }
    })
