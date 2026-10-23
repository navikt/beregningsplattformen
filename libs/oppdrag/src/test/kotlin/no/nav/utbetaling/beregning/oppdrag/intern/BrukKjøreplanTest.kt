package no.nav.utbetaling.beregning.oppdrag.intern

import io.kotest.core.spec.style.FunSpec

import no.nav.utbetaling.beregning.oppdrag.BrukKjøreplan
import no.nav.utbetaling.beregning.oppdrag.Oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.linje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.perPeriode
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.skalVæreLik
import no.nav.utbetaling.beregning.tid.januar
import no.nav.utbetaling.beregning.tid.til

class BrukKjøreplanTest :
    FunSpec({

        test("Bruker kjøreplan om ingenting er satt") {
            val innsending =
                oppdragstidslinje(
                    linjer = listOf(linje(delytelseId = "1", fom = 1.januar, tom = 31.januar, kid = "111")),
                )
            innsending.kjøreplaner() skalVæreLik
                mapOf(
                    1.januar til 31.januar to setOf(BrukKjøreplan.Ja),
                )
        }

        test("Bruker kjøreplan om eksplisitt satt J") {
            val innsending =
                oppdragstidslinje(
                    linjer = listOf(linje(delytelseId = "1", fom = 1.januar, tom = 31.januar, brukKjøreplan = "J")),
                )
            innsending.kjøreplaner() skalVæreLik
                mapOf(
                    1.januar til 31.januar to setOf(BrukKjøreplan.Ja),
                )
        }

        test("Bruker ikke kjøreplan om eksplisitt satt N") {
            val innsending =
                oppdragstidslinje(
                    linjer = listOf(linje(delytelseId = "1", fom = 1.januar, tom = 31.januar, brukKjøreplan = "N")),
                )
            innsending.kjøreplaner() skalVæreLik
                mapOf(
                    1.januar til 31.januar to setOf(BrukKjøreplan.Nei),
                )
        }
    })

private fun Oppdragstidslinje.kjøreplaner() = perPeriode { it.brukKjøreplan.opplysning }
