package no.nav.utbetaling.beregning.oppdrag.intern

import java.util.UUID

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

import no.nav.utbetaling.beregning.oppdrag.Oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.Utbetalingsfrekvens
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.linje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.perPeriode
import no.nav.utbetaling.beregning.tid.februar
import no.nav.utbetaling.beregning.tid.januar
import no.nav.utbetaling.beregning.tid.til

class UtbetalingsfrekvensTest :
    FunSpec({
        test("Består når neste innsending ikke nevner den") {
            val fagsystemId = "${UUID.randomUUID()}"
            val førsteInnsending = oppdragstidslinje(fagsystemId = fagsystemId, utbetalingsfrekvens = "MND", linjer = listOf(linje(delytelseId = "1", fom = 1.januar, tom = 31.januar, sats = 100)))
            val andreInnsending = oppdragstidslinje(fagsystemId = fagsystemId, linjer = listOf(linje(delytelseId = "2", refDelytelseId = "1", fom = 1.januar, tom = 31.januar, sats = 200)))

            val resultat = førsteInnsending + andreInnsending

            resultat.satserMedUtbetalingsfrekvens() shouldBe mapOf(januar to setOf(200 to Utbetalingsfrekvens.Månedlig))
        }

        test("En ny innsending overstyrer den vi har") {
            val fagsystemId = "${UUID.randomUUID()}"
            val førsteInnsending = oppdragstidslinje(fagsystemId = fagsystemId, utbetalingsfrekvens = "MND", linjer = listOf(linje(delytelseId = "1", fom = 1.januar, tom = 31.januar, sats = 100)))
            val andreInnsending =
                oppdragstidslinje(fagsystemId = fagsystemId, utbetalingsfrekvens = "DAG", linjer = listOf(linje(delytelseId = "2", refDelytelseId = "1", fom = 1.januar, tom = 31.januar, sats = 200)))

            val resultat = førsteInnsending + andreInnsending

            resultat.satserMedUtbetalingsfrekvens() shouldBe mapOf(januar to setOf(200 to Utbetalingsfrekvens.Daglig))
        }

        test("Smitter ikke over på en annen sak") {
            val medFrekvens =
                oppdragstidslinje(fagsystemId = "${UUID.randomUUID()}", utbetalingsfrekvens = "MND", linjer = listOf(linje(delytelseId = "1", fom = 1.januar, tom = 31.januar, sats = 100)))
            val utenFrekvens = oppdragstidslinje(fagsystemId = "${UUID.randomUUID()}", linjer = listOf(linje(delytelseId = "2", fom = 1.januar, tom = 31.januar, sats = 200)))

            val resultat = medFrekvens + utenFrekvens

            resultat.satserMedUtbetalingsfrekvens() shouldBe mapOf(januar to setOf(100 to Utbetalingsfrekvens.Månedlig, 200 to null))
        }

        test("Blank verdi er det samme som at den ikke er satt") {
            val innsending = oppdragstidslinje(utbetalingsfrekvens = "  ", linjer = listOf(linje(delytelseId = "1", fom = 1.januar, tom = 31.januar, sats = 100)))

            innsending.satserMedUtbetalingsfrekvens() shouldBe mapOf(januar to setOf(100 to null))
        }

        test("En frekvens vi ikke kjenner til sier tydelig fra") {
            shouldThrow<IllegalStateException> {
                oppdragstidslinje(utbetalingsfrekvens = "KVARTAL", linjer = listOf(linje(delytelseId = "1", fom = 1.januar, tom = 31.januar, sats = 100)))
            }.message shouldBe "Kjenner ikke til utbetalingsfrekvens KVARTAL"
        }

        test("Null i en senere innsending viser bare at Oppdrag ikke gjentar den, ikke at den er fjernet") {
            val fagsystemId = "${UUID.randomUUID()}"
            val førsteInnsending = oppdragstidslinje(fagsystemId = fagsystemId, utbetalingsfrekvens = "MND", linjer = listOf(linje(delytelseId = "1", fom = 1.januar, tom = 31.januar, sats = 100)))
            val andreInnsending =
                oppdragstidslinje(fagsystemId = fagsystemId, utbetalingsfrekvens = null, linjer = listOf(linje(delytelseId = "2", refDelytelseId = "1", fom = 1.februar, tom = 28.februar, sats = 200)))

            val resultat = førsteInnsending + andreInnsending

            resultat.satserMedUtbetalingsfrekvens() shouldBe
                mapOf(
                    januar to setOf(100 to Utbetalingsfrekvens.Månedlig),
                    1.februar til 28.februar to setOf(200 to Utbetalingsfrekvens.Månedlig),
                )
        }
    })

private fun Oppdragstidslinje.satserMedUtbetalingsfrekvens() = perPeriode { it.sats.opplysning.toInt() to it.utbetalingsfrekvens?.opplysning }
