package no.nav.utbetaling.beregning.oppdrag.intern

import java.util.UUID

import io.kotest.core.spec.style.FunSpec

import no.nav.utbetaling.beregning.oppdrag.Oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.linje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.perPeriode
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.skalVæreLik
import no.nav.utbetaling.beregning.tid.desember
import no.nav.utbetaling.beregning.tid.februar
import no.nav.utbetaling.beregning.tid.januar
import no.nav.utbetaling.beregning.tid.juni
import no.nav.utbetaling.beregning.tid.til

class MaksdatoTest :
    FunSpec({
        test("Maksdato gjelder fra og med datoFom i refusjonsinfo") {
            val innsending =
                oppdragstidslinje(
                    linjer =
                        listOf(
                            linje(
                                fom = 1.januar,
                                tom = 31.januar,
                                delytelseId = "1",
                                refusjonMaksDato = 31.desember,
                                refusjonDatoFom = 10.januar,
                            ),
                        ),
                )

            innsending.maksdato() skalVæreLik
                mapOf(
                    1.januar til 9.januar to setOf(null),
                    10.januar til 31.januar to setOf(31.desember),
                )
        }

        test("Ingen maksdato gir null") {
            val innsending = oppdragstidslinje(linjer = listOf(linje(fom = 1.januar, tom = 31.januar, delytelseId = "1")))

            innsending.maksdato() skalVæreLik mapOf(1.januar til 31.januar to setOf(null))
        }

        test("Maksdato videreføres når en senere innsending ikke nevner den") {
            val fagsystemId = "${UUID.randomUUID()}"
            val innsending1 =
                oppdragstidslinje(fagsystemId = fagsystemId, linjer = listOf(linje(fom = 1.januar, tom = 31.januar, delytelseId = "1", refusjonMaksDato = 31.desember)))
            val innsending2 =
                oppdragstidslinje(fagsystemId = fagsystemId, linjer = listOf(linje(fom = 1.februar, tom = 28.februar, delytelseId = "2", refDelytelseId = "1")))

            val resultat = innsending1 + innsending2

            resultat.maksdato() skalVæreLik
                mapOf(
                    1.januar til 31.januar to setOf(31.desember),
                    1.februar til 28.februar to setOf(31.desember),
                )
        }

        test("Eksplisitt maksdato i senere innsending vinner over videreført") {
            val fagsystemId = "${UUID.randomUUID()}"
            val innsending1 =
                oppdragstidslinje(fagsystemId = fagsystemId, linjer = listOf(linje(fom = 1.januar, tom = 31.januar, delytelseId = "1", refusjonMaksDato = 31.desember)))
            val innsending2 =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    linjer = listOf(linje(fom = 1.februar, tom = 28.februar, delytelseId = "2", refDelytelseId = "1", refusjonMaksDato = 30.juni)),
                )

            val resultat = innsending1 + innsending2

            resultat.maksdato() skalVæreLik
                mapOf(
                    1.januar til 31.januar to setOf(31.desember),
                    1.februar til 28.februar to setOf(30.juni),
                )
        }
    })

private fun Oppdragstidslinje.maksdato() = perPeriode { it.maksdato?.opplysning }
