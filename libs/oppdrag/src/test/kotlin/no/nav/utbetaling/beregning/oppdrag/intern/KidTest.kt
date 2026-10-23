package no.nav.utbetaling.beregning.oppdrag.intern

import java.util.UUID

import io.kotest.core.spec.style.FunSpec

import no.nav.utbetaling.beregning.oppdrag.Oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.linje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.perPeriode
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.skalVæreLik
import no.nav.utbetaling.beregning.tid.februar
import no.nav.utbetaling.beregning.tid.januar
import no.nav.utbetaling.beregning.tid.til

class KidTest :
    FunSpec({

        test("Beholder KID når en korreksjon på samme dag ikke opplyser om KID") {
            val fagsystemId = "${UUID.randomUUID()}"
            val førsteInnsending =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    linjer = listOf(linje(delytelseId = "1", fom = 1.januar, tom = 31.januar, sats = 100, kid = "111")),
                )
            val innsendingUtenKid =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    linjer = listOf(linje(delytelseId = "2", refDelytelseId = "1", fom = 10.januar, tom = 31.januar, sats = 200)),
                )

            val resultat = førsteInnsending + innsendingUtenKid
            resultat.kidnumre() skalVæreLik
                mapOf(
                    1.januar til 9.januar to setOf("111"),
                    10.januar til 31.januar to setOf("111"),
                )
        }

        test("Kid bare satt for deler av perioden") {
            val innsending =
                oppdragstidslinje(
                    linjer = listOf(linje(delytelseId = "1", fom = 1.januar, tom = 31.januar, sats = 100, kid = "111", datoKidFom = 10.januar)),
                )

            innsending.kidnumre() skalVæreLik
                mapOf(
                    1.januar til 9.januar to setOf(null),
                    10.januar til 31.januar to setOf("111"),
                )
        }

        // TODO: Er kid også noe som skal videreføres fra tidligere linje i kjeden? Slik som gjelderId, utbetalesTil? Eller er det rett tolket som `null` slik testen er nå?
        test("Viderefører ikke KID fra tidligere linje i kjeden når en korreksjon ikke opplyser om KID") {
            val fagsystemId = "${UUID.randomUUID()}"
            val førsteInnsending =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    linjer = listOf(linje(delytelseId = "1", fom = 1.januar, tom = 31.januar, sats = 100, kid = "111")),
                )
            val innsendingUtenKid =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    linjer = listOf(linje(delytelseId = "2", refDelytelseId = "1", fom = 1.februar, tom = 29.februar, sats = 200, kid = "222", datoKidFom = 10.februar)),
                )

            val resultat = førsteInnsending + innsendingUtenKid
            resultat.kidnumre() skalVæreLik
                mapOf(
                    1.januar til 31.januar to setOf("111"),
                    1.februar til 9.februar to setOf(null),
                    10.februar til 29.februar to setOf("222"),
                )
        }
    })

private fun Oppdragstidslinje.kidnumre() = perPeriode { it.kidnummer?.opplysning?.nummer }
