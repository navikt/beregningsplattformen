package no.nav.utbetaling.beregning.oppdrag.intern

import java.util.UUID

import io.kotest.core.spec.style.FunSpec

import no.nav.utbetaling.beregning.oppdrag.Oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.Søknadstype
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.linje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.perPeriode
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.skalVæreLik
import no.nav.utbetaling.beregning.tid.januar
import no.nav.utbetaling.beregning.tid.til

class SøknadstypeTest :
    FunSpec({
        test("typeSoknad mappes fra linjen") {
            val innsending =
                oppdragstidslinje(linjer = listOf(linje(fom = 1.januar, tom = 31.januar, delytelseId = "1", typeSoknad = "U")))

            innsending.søknadstype() skalVæreLik mapOf(1.januar til 31.januar to setOf(Søknadstype("U")))
        }

        test("typeSoknad videreføres ikke, men beholdes på linjen der den er satt") {
            val fagsystemId = "${UUID.randomUUID()}"
            val innsending1 =
                oppdragstidslinje(fagsystemId = fagsystemId, linjer = listOf(linje(fom = 1.januar, tom = 31.januar, delytelseId = "1", typeSoknad = "U")))
            val innsending2 =
                oppdragstidslinje(fagsystemId = fagsystemId, linjer = listOf(linje(fom = 1.januar, tom = 31.januar, delytelseId = "2", refDelytelseId = "1")))

            val resultat = innsending1 + innsending2

            resultat.søknadstype() skalVæreLik mapOf(1.januar til 31.januar to setOf(Søknadstype("U")))
        }

        test("Ingen typeSoknad gir null") {
            val innsending = oppdragstidslinje(linjer = listOf(linje(fom = 1.januar, tom = 31.januar, delytelseId = "1")))
            innsending.søknadstype() skalVæreLik mapOf(1.januar til 31.januar to setOf(null))
        }
    })

private fun Oppdragstidslinje.søknadstype() = perPeriode { it.søknadstype?.opplysning }
