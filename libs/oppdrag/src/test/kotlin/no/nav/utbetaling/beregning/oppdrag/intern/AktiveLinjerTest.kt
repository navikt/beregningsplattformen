package no.nav.utbetaling.beregning.oppdrag.intern

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.linje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.oppdrag110
import no.nav.utbetaling.beregning.oppdrag.xml.OppdragsLinje150
import no.nav.utbetaling.beregning.tid.Periode
import no.nav.utbetaling.beregning.tid.Periode.ÅpenPeriode
import no.nav.utbetaling.beregning.tid.april
import no.nav.utbetaling.beregning.tid.februar
import no.nav.utbetaling.beregning.tid.januar
import no.nav.utbetaling.beregning.tid.mars
import no.nav.utbetaling.beregning.tid.til

class AktiveLinjerTest :
    FunSpec({
        test("En linje uten datoVedtakTom gjelder for alltid") {
            aktiveLinjer(linje(delytelseId = "1", fom = 1.januar, tom = null)) shouldBe listOf(mapOf(ÅpenPeriode(1.januar) to "1"))
        }

        test("En nyere linje overtar plassen til den eldre i sin helhet") {
            val resultat =
                aktiveLinjer(
                    linje(delytelseId = "1", fom = 1.januar, tom = 31.januar),
                    linje(delytelseId = "2", refDelytelseId = "1", fom = 10.januar, tom = 20.januar),
                )

            resultat shouldBe
                listOf(
                    mapOf(
                        1.januar til 9.januar to "1",
                        10.januar til 20.januar to "2",
                    ),
                )
        }

        test("Bare den nyeste linjen får avgjøre hvor langt kjeden rekker") {
            val resultat =
                aktiveLinjer(
                    linje(delytelseId = "1", fom = 1.januar, tom = null),
                    linje(delytelseId = "2", refDelytelseId = "1", fom = 1.februar, tom = 28.februar),
                )

            resultat shouldBe
                listOf(
                    mapOf(
                        januar to "1",
                        1.februar til 28.februar to "2",
                    ),
                )
        }

        test("Rekkefølgen på linjene har ingenting å si") {
            val linje1 = linje(delytelseId = "1", fom = 1.januar, tom = null)
            val linje2 = linje(delytelseId = "2", refDelytelseId = "1", fom = 1.februar, tom = 28.februar)
            val linje3 = linje(delytelseId = "3", refDelytelseId = "2", fom = 1.april, tom = null)

            val forventet =
                listOf(
                    mapOf(
                        januar to "1",
                        1.februar til 28.februar to "2",
                        ÅpenPeriode(1.april) to "3",
                    ),
                )

            aktiveLinjer(linje1, linje2, linje3) shouldBe forventet
            aktiveLinjer(linje3, linje2, linje1) shouldBe forventet
            aktiveLinjer(linje2, linje3, linje1) shouldBe forventet
        }

        test("Linjer uten felles delytelseId blir hver sin kjede") {
            val resultat =
                aktiveLinjer(
                    linje(delytelseId = "1", fom = 1.januar, tom = 31.januar),
                    linje(delytelseId = "2", fom = 10.januar, tom = 20.januar),
                )

            resultat shouldBe
                listOf(
                    mapOf(januar to "1"),
                    mapOf(10.januar til 20.januar to "2"),
                )
        }

        test("En linje som peker på to løse kjeder binder dem sammen til én") {
            // Linje 2 kommer før den som binder dem sammen, så kjedene er to separate helt til linje 3 dukker opp
            val resultat =
                aktiveLinjer(
                    linje(delytelseId = "1", fom = 1.januar, tom = 31.januar),
                    linje(delytelseId = "3", refDelytelseId = "2", fom = 1.mars, tom = 31.mars),
                    linje(delytelseId = "2", refDelytelseId = "1", fom = 1.februar, tom = 28.februar),
                )

            resultat shouldBe
                listOf(
                    mapOf(
                        januar to "1",
                        1.februar til 28.februar to "2",
                        1.mars til 31.mars to "3",
                    ),
                )
        }

        test("DelytelseId må være unik på tvers av linjene i en kjede") {
            shouldThrow<IllegalArgumentException> {
                aktiveLinjer(
                    linje(delytelseId = "1", fom = 1.januar, tom = 31.januar),
                    linje(delytelseId = "1", refDelytelseId = "1", fom = 1.februar, tom = 28.februar),
                )
            }
        }

        test("Flere linjer kan ikke peke på samme linje") {
            shouldThrow<IllegalArgumentException> {
                aktiveLinjer(
                    linje(delytelseId = "1", fom = 1.januar, tom = 31.januar),
                    linje(delytelseId = "2", refDelytelseId = "1", fom = 1.februar, tom = 28.februar),
                    linje(delytelseId = "3", refDelytelseId = "1", fom = 1.mars, tom = 31.mars),
                )
            }
        }

        test("Et oppdrag uten linjer gir ingen kjeder") {
            aktiveLinjer() shouldBe emptyList()
        }

        test("Korrigerende innsending med flere linjer") {
            val resultat =
                aktiveLinjer(
                    linje(delytelseId = "2", refDelytelseId = "1", fom = 1.februar, tom = 29.februar),
                    linje(delytelseId = "3", refDelytelseId = "2", fom = 1.mars, tom = 31.mars),
                )
            resultat shouldBe
                listOf(
                    mapOf(
                        februar to "2",
                        mars to "3",
                    ),
                )
        }
    })

private fun aktiveLinjer(vararg linjer: OppdragsLinje150): List<Map<Periode, String?>> =
    Linjer
        .fra(oppdrag110(linjer = linjer.toList()))
        .map { (_, linjer) -> linjer.aktive.associate { (periode, linje) -> periode to linje.delytelseId } }
