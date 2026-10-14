package no.nav.utbetaling.beregning.oppdrag

import java.util.UUID

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.OMPOSTERINGSKODE
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.linje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.ompostering
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.satserMedOmpostering
import no.nav.utbetaling.beregning.tid.Periode.LukketPeriode.Companion.til
import no.nav.utbetaling.beregning.tid.januar

class OmposteringTest :
    FunSpec({
        test("Uten fom dato gjelder alle dagene i oppdraget") {
            val innsending =
                oppdragstidslinje(
                    ompostering = ompostering(),
                    linjer = listOf(linje(delytelseId = "1", fom = 1.januar, tom = 31.januar, sats = 100)),
                )

            innsending.satserMedOmpostering() shouldBe mapOf(januar to setOf(100 to OMPOSTERINGSKODE))
        }

        test("Med fom dato gjelder den bare fra og med fom datoen") {
            val innsending =
                oppdragstidslinje(
                    ompostering = ompostering(fom = 10.januar),
                    linjer = listOf(linje(delytelseId = "1", fom = 1.januar, tom = 31.januar, sats = 100)),
                )

            innsending.satserMedOmpostering() shouldBe
                mapOf(
                    1.januar til 9.januar to setOf(100 to null),
                    10.januar til 31.januar to setOf(100 to OMPOSTERINGSKODE),
                )
        }

        test("Ompostering består når neste innsending ikke endrer den") {
            val fagsystemId = "${UUID.randomUUID()}"

            val førsteInnsending =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    ompostering = ompostering(),
                    linjer = listOf(linje(delytelseId = "1", fom = 1.januar, tom = 31.januar, sats = 100)),
                )

            val andreInnsending =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    linjer = listOf(linje(delytelseId = "2", refDelytelseId = "1", fom = 1.januar, tom = 31.januar, sats = 200)),
                )

            val resultat = førsteInnsending + andreInnsending

            resultat.satserMedOmpostering() shouldBe mapOf(januar to setOf(200 to OMPOSTERINGSKODE))
        }

        test("Ompostering består med sin egen fom dato når neste innsending ikke endrer den") {
            val fagsystemId = "${UUID.randomUUID()}"

            val førsteInnsending =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    ompostering = ompostering(fom = 10.januar),
                    linjer = listOf(linje(delytelseId = "1", fom = 1.januar, tom = 31.januar, sats = 100)),
                )

            val andreInnsending =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    linjer = listOf(linje(delytelseId = "2", refDelytelseId = "1", fom = 1.januar, tom = 31.januar, sats = 200)),
                )

            val resultat = førsteInnsending + andreInnsending

            resultat.satserMedOmpostering() shouldBe
                mapOf(
                    1.januar til 9.januar to setOf(200 to null),
                    10.januar til 31.januar to setOf(200 to OMPOSTERINGSKODE),
                )
        }

        test("En ny innsending overstyrer bare fra og med sin egen fom dato") {
            val fagsystemId = "${UUID.randomUUID()}"

            val førsteInnsending =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    ompostering = ompostering(),
                    linjer = listOf(linje(delytelseId = "1", fom = 1.januar, tom = 31.januar, sats = 100)),
                )

            val andreInnsending =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    ompostering = ompostering(fom = 10.januar, kode = "N"),
                    linjer = listOf(linje(delytelseId = "2", refDelytelseId = "1", fom = 1.januar, tom = 31.januar, sats = 200)),
                )

            val resultat = førsteInnsending + andreInnsending

            resultat.satserMedOmpostering() shouldBe
                mapOf(
                    1.januar til 9.januar to setOf(200 to OMPOSTERINGSKODE),
                    10.januar til 31.januar to setOf(200 to Omposteringskode("N")),
                )
        }

        test("En helt lik ompostering med feilreg opphever den vi har") {
            val fagsystemId = "${UUID.randomUUID()}"

            val førsteInnsending =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    ompostering = ompostering(fom = 10.januar),
                    linjer = listOf(linje(delytelseId = "1", fom = 1.januar, tom = 31.januar, sats = 100)),
                )

            val opphever =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    ompostering = ompostering(fom = 10.januar, feilreg = "J"),
                    linjer = listOf(linje(delytelseId = "2", refDelytelseId = "1", fom = 1.januar, tom = 31.januar, sats = 200)),
                )

            val resultat = førsteInnsending + opphever

            resultat.satserMedOmpostering() shouldBe mapOf(januar to setOf(200 to null))
        }

        test("En feilreg som ikke matcher noe vi registrert har kaster exception") {
            val fagsystemId = "${UUID.randomUUID()}"

            val førsteInnsending =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    linjer = listOf(linje(delytelseId = "1", fom = 1.januar, tom = 31.januar, sats = 100)),
                )

            val opphever =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    ompostering = ompostering(fom = 20.januar, feilreg = "J"),
                    linjer = listOf(linje(delytelseId = "2", refDelytelseId = "1", fom = 1.januar, tom = 31.januar, sats = 200)),
                )

            val exception =
                shouldThrow<IllegalArgumentException> {
                    førsteInnsending + opphever
                }

            exception.message shouldBe "Kan ikke feilregistrere en endring som ikke finnes."
        }
    })
