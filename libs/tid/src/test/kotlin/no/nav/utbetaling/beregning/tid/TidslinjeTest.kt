package no.nav.utbetaling.beregning.tid

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.utbetaling.beregning.tid.Periode.LukketPeriode.Companion.til
import no.nav.utbetaling.beregning.tid.Periode.ÅpenPeriode
import java.time.LocalDate

class TidslinjeTest :
    FunSpec({
        data class MinVerdi(
            val verdi: String,
            val dato: LocalDate,
        )

        val verdi1 = MinVerdi("A", 6.august)
        val verdi2 = MinVerdi("B", 6.august)
        val verdi3 = MinVerdi("C", 6.august)

        context("Builder") {
            test("skal avvise overlappende perioder for ulike verdier") {
                shouldThrow<IllegalArgumentException> {
                    Tidslinje
                        .Builder<MinVerdi>()
                        .periode(1.januar til 10.januar, verdi1)
                        .periode(5.januar til 15.januar, verdi2)
                        .build()
                }
            }

            test("skal avvise en åpen periode som starter før siste lukkede periode") {
                shouldThrow<IllegalArgumentException> {
                    Tidslinje
                        .Builder<MinVerdi>()
                        .periode(januar, verdi1)
                        .periode(ÅpenPeriode(15.januar), verdi2)
                        .build()
                }
            }

            test("skal lukke tidligere åpne perioder når flere sendes inn, slik at bare den siste er åpen") {
                val tidslinje =
                    Tidslinje
                        .Builder<MinVerdi>()
                        .periode(ÅpenPeriode(1.februar), verdi1)
                        .periode(ÅpenPeriode(1.mars), verdi2)
                        .build()

                tidslinje.toList() shouldContainExactly listOf(februar to verdi1, ÅpenPeriode(1.mars) to verdi2)
            }

            test("Flere åpne perioder som starter samtidig er OK så lenge det er samme verdi") {
                val tidslinje =
                    Tidslinje
                        .Builder<MinVerdi>()
                        .periode(ÅpenPeriode(1.februar), verdi1)
                        .periode(ÅpenPeriode(1.februar), verdi1)
                        .build()

                tidslinje.toList() shouldContainExactly listOf(ÅpenPeriode(1.februar) to verdi1)
            }

            test("Flere åpne perioder som starter samtidig er ikke OK med ulike verdier") {
                shouldThrow<IllegalArgumentException> {
                    Tidslinje
                        .Builder<MinVerdi>()
                        .periode(ÅpenPeriode(1.februar), verdi1)
                        .periode(ÅpenPeriode(1.februar), verdi2)
                        .build()
                }
            }

            test("skal godta at en lukket periode legges til etter den åpne") {
                val tidslinje =
                    Tidslinje
                        .Builder<MinVerdi>()
                        .periode(ÅpenPeriode(1.februar), verdi2)
                        .periode(januar, verdi1)
                        .build()

                tidslinje[15.januar] shouldBe verdi1
                tidslinje[15.mars] shouldBe verdi2
            }

            test("build() kan kalles flere ganger og gir samme tidslinje") {
                val builder =
                    Tidslinje
                        .Builder<MinVerdi>()
                        .periode(ÅpenPeriode(1.januar), verdi1)
                        .periode(ÅpenPeriode(1.februar), verdi2)

                val første = builder.build()
                val andre = builder.build()

                andre shouldBe første
                andre.toList() shouldContainExactly listOf(januar to verdi1, ÅpenPeriode(1.februar) to verdi2)
            }
        }

        context("oppslag") {
            val tidslinje =
                Tidslinje
                    .Builder<MinVerdi>()
                    .periode(januar, verdi1)
                    .periode(mars, verdi2)
                    .build()

            test("skal gi verdien som gjelder på datoen") {
                tidslinje[1.januar] shouldBe verdi1
                tidslinje[31.januar] shouldBe verdi1
                tidslinje[15.mars] shouldBe verdi2
            }

            test("skal gi null i oppholdet mellom periodene") {
                tidslinje[15.februar].shouldBeNull()
            }

            test("skal gi null etter siste periode når tidslinjen er lukket") {
                tidslinje[1.april].shouldBeNull()
            }

            test("skal svare på contains for datoer med verdi") {
                (15.januar in tidslinje) shouldBe true
                (15.februar in tidslinje) shouldBe false
            }

            test("skal være tom når ingen perioder er lagt til") {
                Tidslinje.Builder<MinVerdi>().build().isEmpty() shouldBe true
            }
        }

        context("iterasjon") {
            test("skal gi én lukket periode per segment") {
                val tidslinje =
                    Tidslinje
                        .Builder<MinVerdi>()
                        .periode(januar, verdi1)
                        .periode(mars, verdi2)
                        .build()

                tidslinje.toList() shouldContainExactly listOf(januar to verdi1, mars to verdi2)
            }

            test("skal gi en åpen periode for det siste segmentet når tidslinjen er åpen") {
                val tidslinje =
                    Tidslinje
                        .Builder<MinVerdi>()
                        .periode(januar, verdi1)
                        .periode(ÅpenPeriode(1.februar), verdi2)
                        .build()

                tidslinje.toList() shouldContainExactly listOf(januar to verdi1, ÅpenPeriode(1.februar) to verdi2)
            }
        }

        context("bygging av åpne tidslinjer") {
            test("Flere åpne haler") {
                val tidslinje =
                    Tidslinje
                        .Builder<MinVerdi>()
                        .periode(ÅpenPeriode(1.januar), verdi1)
                        .periode(ÅpenPeriode(1.februar), verdi2)
                        .build()

                tidslinje.toList() shouldContainExactly listOf(januar to verdi1, ÅpenPeriode(1.februar) to verdi2)
            }

            test("skal slå sammen den åpne perioden med perioden foran når verdien er den samme") {
                val tidslinje =
                    Tidslinje
                        .Builder<MinVerdi>()
                        .periode(januar, verdi1)
                        .periode(februar, verdi2)
                        .periode(ÅpenPeriode(1.mars), verdi2)
                        .build()

                tidslinje.toString() shouldContain "Fra og med 01-02-2024"
            }

            test("skal beholde et eget knekkpunkt når den åpne perioden har en annen verdi") {
                val tidslinje =
                    Tidslinje
                        .Builder<MinVerdi>()
                        .periode(januar, verdi1)
                        .periode(februar, verdi2)
                        .periode(ÅpenPeriode(1.mars), verdi1)
                        .build()

                tidslinje.toString() shouldContain "Fra og med 01-03-2024"
            }

            test("skal ikke slå sammen over et opphold selv om verdien er den samme") {
                val tidslinje =
                    Tidslinje
                        .Builder<MinVerdi>()
                        .periode(1.januar til 30.januar, verdi1)
                        .periode(1.februar til 27.februar, verdi1)
                        .periode(ÅpenPeriode(1.mars), verdi1)
                        .build()

                tidslinje.toString() shouldContain "Fra og med 01-03-2024"
            }
        }

        context("plus") {
            operator fun Tidslinje<MinVerdi>.plus(other: Tidslinje<MinVerdi>) =
                kombinerMed(other) { _, eksiterende, ny ->
                    ny ?: eksiterende
                }
            test("skal la den nye tidslinjen overstyre den gamle dag for dag") {
                val tidslinje1 = Tidslinje.Builder<MinVerdi>().periode(januar, verdi1).build()
                val tidslinje2 =
                    Tidslinje
                        .Builder<MinVerdi>()
                        .dato(3.januar, verdi2)
                        .dato(10.januar, verdi2)
                        .dato(30.januar, verdi2)
                        .build()

                val forventet =
                    Tidslinje
                        .Builder<MinVerdi>()
                        .periode(1.januar til 2.januar, verdi1)
                        .dato(3.januar, verdi2)
                        .periode(4.januar til 9.januar, verdi1)
                        .dato(10.januar, verdi2)
                        .periode(11.januar til 29.januar, verdi1)
                        .dato(30.januar, verdi2)
                        .dato(31.januar, verdi1)
                        .build()

                (tidslinje1 + tidslinje2) shouldBeEqual forventet
            }

            test("skal dele opp en åpen periode når den andre tidslinjen har nyere verdier") {
                val tidslinje1 =
                    Tidslinje
                        .Builder<MinVerdi>()
                        .periode(januar, verdi1)
                        .periode(ÅpenPeriode(5.februar), verdi2)
                        .build()
                val tidslinje2 =
                    Tidslinje
                        .Builder<MinVerdi>()
                        .periode(10.januar til 15.januar, verdi2)
                        .periode(ÅpenPeriode(1.mars), verdi3)
                        .build()

                val forventet =
                    Tidslinje
                        .Builder<MinVerdi>()
                        .periode(1.januar til 9.januar, verdi1)
                        .periode(10.januar til 15.januar, verdi2)
                        .periode(16.januar til 31.januar, verdi1)
                        .periode(5.februar til 29.februar, verdi2)
                        .periode(ÅpenPeriode(1.mars), verdi3)
                        .build()

                (tidslinje1 + tidslinje2) shouldBeEqual forventet
            }
        }

        context("minus") {
            operator fun Tidslinje<MinVerdi>.minus(other: Tidslinje<MinVerdi>) =
                kombinerMed(other) { _, eksiterende, ny ->
                    if (ny == eksiterende) null else ny ?: eksiterende
                }

            test("skal utelate uendrede verdier fra differansen") {
                val eksisterende = Tidslinje.Builder<MinVerdi>().periode(januar, verdi1).build()
                val ny =
                    Tidslinje
                        .Builder<MinVerdi>()
                        .periode(januar, verdi1)
                        .periode(februar, verdi2)
                        .build()

                val forventet = Tidslinje.Builder<MinVerdi>().periode(februar, verdi2).build()

                (eksisterende - ny) shouldBeEqual forventet
            }

            test("skal ta med en ny åpen periode i differansen") {
                val eksisterende = Tidslinje.Builder<MinVerdi>().periode(januar, verdi1).build()
                val ny =
                    Tidslinje
                        .Builder<MinVerdi>()
                        .periode(januar, verdi1)
                        .periode(ÅpenPeriode(1.februar), verdi2)
                        .build()

                val resultat = eksisterende - ny
                val forventet = Tidslinje.Builder<MinVerdi>().periode(ÅpenPeriode(1.februar), verdi2).build()

                resultat shouldBeEqual forventet
                resultat.toString() shouldContain "Fra og med 01-02-2024"
            }
        }

        context("begrensetTil") {
            test("skal ikke endre en tom tidslinje") {
                val tom = Tidslinje.Builder<MinVerdi>().build()

                tom.begrensetTil(15.mars) shouldBeEqual tom
            }

            test("skal ikke endre en lukket tidslinje som slutter før datoen") {
                val tidslinje = Tidslinje.Builder<MinVerdi>().periode(januar, verdi1).build()

                tidslinje.begrensetTil(15.mars) shouldBeEqual tidslinje
            }

            test("skal ikke endre en lukket tidslinje som slutter på samme dato") {
                val tidslinje = Tidslinje.Builder<MinVerdi>().periode(januar, verdi1).build()

                tidslinje.begrensetTil(31.januar) shouldBeEqual tidslinje
            }

            test("skal begrense en åpen tidslinje til og med datoen") {
                val tidslinje =
                    Tidslinje
                        .Builder<MinVerdi>()
                        .periode(ÅpenPeriode(1.januar), verdi1)
                        .build()

                val forventet = Tidslinje.Builder<MinVerdi>().periode(1.januar til 15.mars, verdi1).build()

                tidslinje.begrensetTil(15.mars) shouldBeEqual forventet
            }

            test("skal kutte bort perioder etter datoen og beholde verdien som gjelder på datoen") {
                val tidslinje =
                    Tidslinje
                        .Builder<MinVerdi>()
                        .periode(januar, verdi1)
                        .periode(februar, verdi2)
                        .periode(mars, verdi3)
                        .build()

                val forventet =
                    Tidslinje
                        .Builder<MinVerdi>()
                        .periode(januar, verdi1)
                        .periode(1.februar til 15.februar, verdi2)
                        .build()

                tidslinje.begrensetTil(15.februar) shouldBeEqual forventet
            }

            test("skal gi en tom tidslinje når datoen er før første periode") {
                val tidslinje = Tidslinje.Builder<MinVerdi>().periode(februar, verdi1).build()

                tidslinje.begrensetTil(15.januar).isEmpty() shouldBe true
            }
        }

        context("mapVerdier") {
            test("skal transformere verdiene og beholde den åpne perioden") {
                val tidslinje =
                    Tidslinje
                        .Builder<Int>()
                        .periode(januar, 1)
                        .periode(ÅpenPeriode(1.februar), 2)
                        .build()

                val forventet =
                    Tidslinje
                        .Builder<String>()
                        .periode(januar, "verdi-1")
                        .periode(ÅpenPeriode(1.februar), "verdi-2")
                        .build()

                tidslinje.mapVerdier { "verdi-$it" } shouldBeEqual forventet
            }

            test("skal fjerne verdien når transformasjonen gir null") {
                val tidslinje =
                    Tidslinje
                        .Builder<Int>()
                        .periode(januar, 1)
                        .periode(februar, 2)
                        .build()

                val forventet = Tidslinje.Builder<Int>().periode(februar, 2).build()

                tidslinje.mapVerdier { it.takeIf { verdi -> verdi > 1 } } shouldBeEqual forventet
            }

            test("skal slå sammen nabosegmenter som får samme verdi") {
                val tidslinje =
                    Tidslinje
                        .Builder<Int>()
                        .periode(januar, 1)
                        .periode(februar, 2)
                        .build()

                val forventet = Tidslinje.Builder<String>().periode(1.januar til 29.februar, "fast").build()

                tidslinje.mapVerdier { "fast" } shouldBeEqual forventet
            }
        }

        context("kombinerMed") {
            test("skal la venstre side avgjøre om dagen har en verdi") {
                val venstre = Tidslinje.Builder<String>().periode(januar, "A").build()
                val høyre = Tidslinje.Builder<Int>().periode(mars, 42).build()

                val resultat = venstre.kombinerMed(høyre) { _, denne, andre -> denne?.let { "$it${andre ?: ""}" } }
                val forventet = Tidslinje.Builder<String>().periode(januar, "A").build()

                resultat shouldBeEqual forventet
            }

            test("skal dele opp en åpen periode når den andre verdien utløper") {
                val linjer = Tidslinje.Builder<String>().periode(ÅpenPeriode(1.januar), "Linje").build()
                val avventinger = Tidslinje.Builder<Int>().periode(mars, 42).build()

                val resultat = linjer.kombinerMed(avventinger) { _, linje, avventing -> linje?.let { "$it-${avventing ?: "ingen"}" } }
                val forventet =
                    Tidslinje
                        .Builder<String>()
                        .periode(1.januar til 29.februar, "Linje-ingen")
                        .periode(mars, "Linje-42")
                        .periode(ÅpenPeriode(1.april), "Linje-ingen")
                        .build()

                resultat shouldBeEqual forventet
            }
        }

        context("toString") {
            test("skal si fra når tidslinjen er tom") {
                Tidslinje.Builder<MinVerdi>().build().toString() shouldBeEqual "Tom Tidslinje"
            }

            test("skal vise verditype og perioder") {
                val tidslinje = Tidslinje.Builder<MinVerdi>().periode(januar, verdi1).build()

                tidslinje.toString() shouldContain "Tidslinje med MinVerdi"
                tidslinje.toString() shouldContain "01-01-2024 til 31-01-2024"
            }
        }
    })
