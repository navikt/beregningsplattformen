package no.nav.utbetaling.beregning.oppdrag.intern

import java.util.UUID

import io.kotest.core.spec.style.FunSpec

import no.nav.utbetaling.beregning.oppdrag.Henvisning
import no.nav.utbetaling.beregning.oppdrag.Oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.linje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.perPeriode
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.skalVæreLik
import no.nav.utbetaling.beregning.tid.januar
import no.nav.utbetaling.beregning.tid.til

class HenvisningTest :
    FunSpec({
        test("Består når neste innsending ikke nevner den") {
            val fagsystemId = "${UUID.randomUUID()}"
            val førsteInnsending = oppdragstidslinje(fagsystemId = fagsystemId, linjer = listOf(linje(delytelseId = "1", fom = 1.januar, tom = 31.januar, sats = 100, henvisning = "H1")))
            val andreInnsending = oppdragstidslinje(fagsystemId = fagsystemId, linjer = listOf(linje(delytelseId = "2", refDelytelseId = "1", fom = 10.januar, tom = 31.januar, sats = 200)))

            val resultat = førsteInnsending + andreInnsending

            resultat.satserMedHenvisning() skalVæreLik
                mapOf(
                    1.januar til 9.januar to setOf(100 to Henvisning("H1")),
                    10.januar til 31.januar to setOf(200 to Henvisning("H1")),
                )
        }

        test("En ny innsending overstyrer den vi har") {
            val fagsystemId = "${UUID.randomUUID()}"
            val førsteInnsending = oppdragstidslinje(fagsystemId = fagsystemId, linjer = listOf(linje(delytelseId = "1", fom = 1.januar, tom = 31.januar, sats = 100, henvisning = "H1")))
            val andreInnsending =
                oppdragstidslinje(fagsystemId = fagsystemId, linjer = listOf(linje(delytelseId = "2", refDelytelseId = "1", fom = 10.januar, tom = 31.januar, sats = 200, henvisning = "H2")))

            val resultat = førsteInnsending + andreInnsending

            resultat.satserMedHenvisning() skalVæreLik
                mapOf(
                    1.januar til 9.januar to setOf(100 to Henvisning("H1")),
                    10.januar til 31.januar to setOf(200 to Henvisning("H2")),
                )
        }

        test("Følger kjeden, ikke oppdraget") {
            val fagsystemId = "${UUID.randomUUID()}"
            val medHenvisning = oppdragstidslinje(fagsystemId = fagsystemId, linjer = listOf(linje(delytelseId = "1", fom = 1.januar, tom = 31.januar, sats = 100, henvisning = "H1")))
            val egenKjedeSammeSak = oppdragstidslinje(fagsystemId = fagsystemId, linjer = listOf(linje(delytelseId = "2", fom = 1.januar, tom = 31.januar, sats = 200)))

            val resultat = medHenvisning + egenKjedeSammeSak

            resultat.satserMedHenvisning() skalVæreLik mapOf(januar to setOf(100 to Henvisning("H1"), 200 to null))
        }
    })

private fun Oppdragstidslinje.satserMedHenvisning() = perPeriode { it.sats.opplysning.toInt() to it.henvisning?.opplysning }
