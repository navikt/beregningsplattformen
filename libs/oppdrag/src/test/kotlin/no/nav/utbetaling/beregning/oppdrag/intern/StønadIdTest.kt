package no.nav.utbetaling.beregning.oppdrag.intern

import java.util.UUID

import io.kotest.core.spec.style.FunSpec

import no.nav.utbetaling.beregning.oppdrag.Oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.StønadId
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.linje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.perPeriode
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.skalVæreLik
import no.nav.utbetaling.beregning.tid.februar
import no.nav.utbetaling.beregning.tid.januar
import no.nav.utbetaling.beregning.tid.til

class StønadIdTest :
    FunSpec({
        test("Består når neste innsending ikke nevner den") {
            val fagsystemId = "${UUID.randomUUID()}"
            val førsteInnsending = oppdragstidslinje(fagsystemId = fagsystemId, stønadId = "S1", linjer = listOf(linje(delytelseId = "1", fom = 1.januar, tom = 31.januar, sats = 100)))
            val andreInnsending = oppdragstidslinje(fagsystemId = fagsystemId, linjer = listOf(linje(delytelseId = "2", refDelytelseId = "1", fom = 1.januar, tom = 31.januar, sats = 200)))

            val resultat = førsteInnsending + andreInnsending

            resultat.satserMedStønadId() skalVæreLik mapOf(januar to setOf(200 to StønadId("S1")))
        }

        test("En ny innsending overstyrer den vi har") {
            val fagsystemId = "${UUID.randomUUID()}"
            val førsteInnsending = oppdragstidslinje(fagsystemId = fagsystemId, stønadId = "S1", linjer = listOf(linje(delytelseId = "1", fom = 1.januar, tom = 31.januar, sats = 100)))
            val andreInnsending =
                oppdragstidslinje(fagsystemId = fagsystemId, stønadId = "S2", linjer = listOf(linje(delytelseId = "2", refDelytelseId = "1", fom = 1.januar, tom = 31.januar, sats = 200)))

            val resultat = førsteInnsending + andreInnsending

            resultat.satserMedStønadId() skalVæreLik mapOf(januar to setOf(200 to StønadId("S2")))
        }

        test("Smitter ikke over på en annen sak") {
            val medStønadId = oppdragstidslinje(fagsystemId = "${UUID.randomUUID()}", stønadId = "S1", linjer = listOf(linje(delytelseId = "1", fom = 1.januar, tom = 31.januar, sats = 100)))
            val utenStønadId = oppdragstidslinje(fagsystemId = "${UUID.randomUUID()}", linjer = listOf(linje(delytelseId = "2", fom = 1.januar, tom = 31.januar, sats = 200)))

            val resultat = medStønadId + utenStønadId

            resultat.satserMedStønadId() skalVæreLik mapOf(januar to setOf(100 to StønadId("S1"), 200 to null))
        }

        test("Blank verdi er det samme som at den ikke er satt") {
            val innsending = oppdragstidslinje(stønadId = "  ", linjer = listOf(linje(delytelseId = "1", fom = 1.januar, tom = 31.januar, sats = 100)))

            innsending.satserMedStønadId() skalVæreLik mapOf(januar to setOf(100 to null))
        }

        test("Null i en senere innsending viser bare at Oppdrag ikke gjentar den, ikke at den er fjernet") {
            val fagsystemId = "${UUID.randomUUID()}"
            val førsteInnsending = oppdragstidslinje(fagsystemId = fagsystemId, stønadId = "S1", linjer = listOf(linje(delytelseId = "1", fom = 1.januar, tom = 31.januar, sats = 100)))
            val andreInnsending =
                oppdragstidslinje(fagsystemId = fagsystemId, stønadId = null, linjer = listOf(linje(delytelseId = "2", refDelytelseId = "1", fom = 1.februar, tom = 28.februar, sats = 200)))

            val resultat = førsteInnsending + andreInnsending

            resultat.satserMedStønadId() skalVæreLik
                mapOf(
                    januar to setOf(100 to StønadId("S1")),
                    1.februar til 28.februar to setOf(200 to StønadId("S1")),
                )
        }
    })

private fun Oppdragstidslinje.satserMedStønadId() = perPeriode { it.sats.opplysning.toInt() to it.stønadId?.opplysning }
