package no.nav.utbetaling.beregning.oppdrag.intern

import java.util.UUID

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

import no.nav.utbetaling.beregning.oppdrag.Avventingsårsak
import no.nav.utbetaling.beregning.oppdrag.Oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.Opplysning
import no.nav.utbetaling.beregning.oppdrag.SaksbehandlerId
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.avventing
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.linje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.perPeriode
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.skalVæreLik
import no.nav.utbetaling.beregning.tid.januar
import no.nav.utbetaling.beregning.tid.mars
import no.nav.utbetaling.beregning.tid.til

class AvventingTest :
    FunSpec({
        test("Avventing fra første innsending beholdes når vi sender inn en ny innsending uten avventing") {
            val fagsystemId = "${UUID.randomUUID()}"

            val førsteInnsendingMedAvventing =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    linjer = listOf(linje(fom = 1.januar, tom = 31.januar, sats = 100, delytelseId = "1")),
                    avventinger = listOf(avventing(fom = 10.januar, tom = 20.januar)),
                )

            val andreInnsendingUtenAvventing =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    linjer = listOf(linje(fom = 1.januar, tom = 31.januar, sats = 200, refDelytelseId = "1", delytelseId = "2")),
                )

            val resultat = førsteInnsendingMedAvventing + andreInnsendingUtenAvventing

            resultat.satserMedAvventing() skalVæreLik
                mapOf(
                    1.januar til 9.januar to setOf(200 to null),
                    10.januar til 20.januar to setOf(200 to Avventingsårsak.Avregning(til = 1.mars)),
                    21.januar til 31.januar to setOf(200 to null),
                )
        }

        test("Avventing fra første innsending fjernes når vi sender inn en ny innsending med feilreg") {
            val fagsystemId = "${UUID.randomUUID()}"

            val linjer = listOf(linje(fom = 1.januar, tom = 31.januar, delytelseId = "1"))

            val førsteInnsendingMedAvventing =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    linjer = linjer,
                    avventinger = listOf(avventing(fom = 10.januar, tom = 20.januar, kodeArsak = "AVRK")),
                )

            førsteInnsendingMedAvventing.avventinger() skalVæreLik
                mapOf(
                    1.januar til 9.januar to setOf(null),
                    10.januar til 20.januar to setOf(Opplysning(SaksbehandlerId("Z999999"), Avventingsårsak.Refusjonskrav(til = 1.mars))),
                    21.januar til 31.januar to setOf(null),
                )

            val andreInnsendingFjernerAvventing =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    linjer = linjer,
                    avventinger = listOf(avventing(fom = 10.januar, tom = 20.januar, kodeArsak = "AVRK", feilreg = "j")),
                )

            val resultat = førsteInnsendingMedAvventing + andreInnsendingFjernerAvventing

            resultat.avventinger() skalVæreLik mapOf(januar to setOf(null))
        }

        test("Feilregistrering av avventing som ikke finnes kaster exception") {
            val fagsystemId = "${UUID.randomUUID()}"

            val linjer = listOf(linje(fom = 1.januar, tom = 31.januar, sats = 100, delytelseId = "1"))

            val førsteInnsendingMedAvventing =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    linjer = linjer,
                    avventinger = listOf(avventing(fom = 10.januar, tom = 20.januar)),
                )

            val andreInnsendingFjernerAvventing =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    linjer = linjer,
                    avventinger = listOf(avventing(fom = 1.januar, tom = 9.januar, feilreg = "j")),
                )

            val exception =
                shouldThrow<IllegalArgumentException> {
                    førsteInnsendingMedAvventing + andreInnsendingFjernerAvventing
                }

            exception.message shouldBe "Kan ikke feilregistrere en endring som ikke finnes."
        }
    })

private fun Oppdragstidslinje.avventinger() = perPeriode { it.avventingsårsak }

private fun Oppdragstidslinje.satserMedAvventing() = perPeriode { it.sats.opplysning.toInt() to it.avventingsårsak?.opplysning }
