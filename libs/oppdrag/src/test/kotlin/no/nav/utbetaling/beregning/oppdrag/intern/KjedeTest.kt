package no.nav.utbetaling.beregning.oppdrag.intern

import java.util.UUID

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe

import no.nav.utbetaling.beregning.oppdrag.Oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.assertGjenskaptfraTidslinjer
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.linje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.satser
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.satserPerFagsystemId
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.skalVæreLik
import no.nav.utbetaling.beregning.tid.februar
import no.nav.utbetaling.beregning.tid.januar
import no.nav.utbetaling.beregning.tid.til

class KjedeTest :
    FunSpec({

        test("En innsending som innfører hull i kjeden") {
            val fagsystemId = "${UUID.randomUUID()}"
            val innsending1 =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    linjer =
                        listOf(
                            linje(fom = 1.januar, tom = 31.januar, sats = 100, delytelseId = "1"),
                            linje(fom = 1.februar, tom = 29.februar, sats = 200, delytelseId = "2", refDelytelseId = "1"),
                        ),
                )
            innsending1.satser() skalVæreLik
                mapOf(
                    januar to setOf(100),
                    februar to setOf(200),
                )

            val innsending2 =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    linjer =
                        listOf(
                            linje(fom = 2.januar, tom = 30.januar, sats = 300, delytelseId = "3", refDelytelseId = "2"),
                            linje(fom = 2.februar, tom = 28.februar, sats = 400, delytelseId = "4", refDelytelseId = "3"),
                        ),
                )
            val resultat = innsending1 + innsending2
            resultat.satser() skalVæreLik
                mapOf(
                    1.januar til 1.januar to setOf(100),
                    2.januar til 30.januar to setOf(300),
                    2.februar til 28.februar to setOf(400),
                )
        }

        test("En nyere linje på samme kjede overtar, og en egen kjede lever side om side") {
            val fagsystemId = "${UUID.randomUUID()}"
            val tidslinje1 =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    linjer =
                        listOf(
                            linje(fom = 1.januar, tom = 31.januar, sats = 100, delytelseId = "1"),
                        ),
                )
            val tidslinje2 =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    linjer =
                        listOf(
                            linje(fom = 10.januar, tom = 31.januar, sats = 50, refDelytelseId = "1", delytelseId = "2"),
                        ),
                )
            val tidslinje3 =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    linjer =
                        listOf(
                            linje(fom = 1.januar, tom = 31.januar, sats = 25, refDelytelseId = null, delytelseId = "3"),
                        ),
                )
            val resultat = tidslinje1 + tidslinje2 + tidslinje3
            resultat.satser() shouldBe
                mapOf(
                    1.januar til 9.januar to setOf(100, 25),
                    10.januar til 31.januar to setOf(50, 25),
                )
            resultat.assertGjenskaptfraTidslinjer()
        }

        test("Samme fagsystemId på to fagområder er to forskjellige saker") {
            val fagsystemId = "${UUID.randomUUID()}"
            val tidslinje1 =
                oppdragstidslinje(
                    kodeFagområde = "AAP",
                    fagsystemId = fagsystemId,
                    linjer =
                        listOf(
                            linje(fom = 1.januar, tom = 31.januar, sats = 100, delytelseId = "1"),
                        ),
                )
            val tidslinje2 =
                oppdragstidslinje(
                    kodeFagområde = "SP",
                    fagsystemId = fagsystemId,
                    linjer =
                        listOf(
                            linje(fom = 1.januar, tom = 31.januar, sats = 50, refDelytelseId = "1", delytelseId = "2"),
                        ),
                )
            val resultat = tidslinje1 + tidslinje2
            resultat.satser() shouldBe mapOf(1.januar til 31.januar to setOf(100, 50))
            resultat.assertGjenskaptfraTidslinjer()
        }

        test("Kjeder, saker og fagsystemIder holdes fra hverandre") {
            val fagsystemId = "${UUID.randomUUID()}"
            val annenFagsystemId = "${UUID.randomUUID()}"

            // Kjeden som endres: linje 2 peker på linje 1 og overtar fra 10. januar
            val førsteVedtak = oppdragstidslinje(fagsystemId = fagsystemId, linjer = listOf(linje(fom = 1.januar, tom = 31.januar, sats = 100, delytelseId = "1")))
            val endringPåSammeKjede = oppdragstidslinje(fagsystemId = fagsystemId, linjer = listOf(linje(fom = 10.januar, tom = 31.januar, sats = 200, refDelytelseId = "1", delytelseId = "2")))

            // Samme fagsystemId, men ingen peker mellom linjene — altså en egen kjede som lever side om side
            val nyKjedeSammeFagsystemId = oppdragstidslinje(fagsystemId = fagsystemId, linjer = listOf(linje(fom = 20.januar, tom = 31.januar, sats = 50, delytelseId = "3")))

            val annenSak = oppdragstidslinje(fagsystemId = annenFagsystemId, linjer = listOf(linje(fom = 25.januar, tom = 31.januar, sats = 25, delytelseId = "4")))

            val resultat = førsteVedtak + endringPåSammeKjede + nyKjedeSammeFagsystemId + annenSak
            resultat.satserPerFagsystemId() shouldBe
                mapOf(
                    1.januar til 9.januar to setOf(fagsystemId to 100),
                    10.januar til 19.januar to setOf(fagsystemId to 200),
                    20.januar til 24.januar to setOf(fagsystemId to 200, fagsystemId to 50),
                    25.januar til 31.januar to setOf(fagsystemId to 200, fagsystemId to 50, annenFagsystemId to 25),
                )
            resultat.assertGjenskaptfraTidslinjer()
        }

        test("En kjede som mangler på den ene siden beholdes") {
            val gammel = oppdragstidslinje(fagsystemId = "${UUID.randomUUID()}", linjer = listOf(linje(fom = 1.januar, tom = 31.januar, sats = 100, delytelseId = "1")))
            val ny = oppdragstidslinje(fagsystemId = "${UUID.randomUUID()}", linjer = listOf(linje(fom = 1.januar, tom = 31.januar, sats = 50, delytelseId = "2")))

            (gammel + ny).satser() shouldBe mapOf(januar to setOf(100, 50))
            (gammel + Oppdragstidslinje()) shouldBeEqual gammel
            ny.assertGjenskaptfraTidslinjer()
            gammel.assertGjenskaptfraTidslinjer()
        }
    })
