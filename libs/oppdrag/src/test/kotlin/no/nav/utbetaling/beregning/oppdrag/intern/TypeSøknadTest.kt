package no.nav.utbetaling.beregning.oppdrag.intern

import java.util.UUID

import io.kotest.core.spec.style.FunSpec

import no.nav.utbetaling.beregning.oppdrag.Oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.TypeSøknad
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.linje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.perPeriode
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.skalVæreLik
import no.nav.utbetaling.beregning.tid.januar
import no.nav.utbetaling.beregning.tid.til

class TypeSøknadTest :
    FunSpec({
        test("typeSøknad mappes fra linjen") {
            val innsending =
                oppdragstidslinje(linjer = listOf(linje(fom = 1.januar, tom = 31.januar, delytelseId = "1", typeSoknad = "U")))

            innsending.typeSøknad() skalVæreLik mapOf(1.januar til 31.januar to setOf(TypeSøknad("U")))
        }

        test("typeSøknad videreføres ikke, men beholdes på linjen der den er satt") {
            val fagsystemId = "${UUID.randomUUID()}"
            val innsending1 =
                oppdragstidslinje(fagsystemId = fagsystemId, linjer = listOf(linje(fom = 1.januar, tom = 31.januar, delytelseId = "1", typeSoknad = "U")))
            val innsending2 =
                oppdragstidslinje(fagsystemId = fagsystemId, linjer = listOf(linje(fom = 1.januar, tom = 31.januar, delytelseId = "2", refDelytelseId = "1")))

            val resultat = innsending1 + innsending2

            resultat.typeSøknad() skalVæreLik mapOf(1.januar til 31.januar to setOf(TypeSøknad("U")))
        }

        test("Ingen typeSøknad gir null") {
            val innsending = oppdragstidslinje(linjer = listOf(linje(fom = 1.januar, tom = 31.januar, delytelseId = "1")))
            innsending.typeSøknad() skalVæreLik mapOf(1.januar til 31.januar to setOf(null))
        }
    })

private fun Oppdragstidslinje.typeSøknad() = perPeriode { it.typeSøknad?.opplysning }
