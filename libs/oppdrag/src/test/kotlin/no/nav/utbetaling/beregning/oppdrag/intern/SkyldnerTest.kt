package no.nav.utbetaling.beregning.oppdrag.intern

import java.util.UUID

import io.kotest.core.spec.style.FunSpec

import no.nav.utbetaling.beregning.oppdrag.Oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.SamhandlerId
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.linje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.perPeriode
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.skalVæreLik
import no.nav.utbetaling.beregning.tid.februar
import no.nav.utbetaling.beregning.tid.januar
import no.nav.utbetaling.beregning.tid.til

class SkyldnerTest :
    FunSpec({

        test("Skyldner gjelder fra og med datoSkyldnerFom, ikke datoVedtakFom") {
            val innsending =
                oppdragstidslinje(
                    linjer =
                        listOf(
                            linje(
                                fom = 1.januar,
                                tom = 31.januar,
                                delytelseId = "1",
                                skyldnerId = "81111111111",
                                datoSkyldnerFom = 10.januar,
                            ),
                        ),
                )

            innsending.skyldner() skalVæreLik
                mapOf(
                    1.januar til 9.januar to setOf(null),
                    10.januar til 31.januar to setOf(SamhandlerId("81111111111")),
                )
        }

        test("Skyldner videreføres når en senere innsending ikke nevner den") {
            val fagsystemId = "${UUID.randomUUID()}"
            val innsending1 =
                oppdragstidslinje(fagsystemId = fagsystemId, linjer = listOf(linje(fom = 1.januar, tom = 31.januar, delytelseId = "1", skyldnerId = "81111111111")))
            val innsending2 =
                oppdragstidslinje(fagsystemId = fagsystemId, linjer = listOf(linje(fom = 1.februar, tom = 28.februar, delytelseId = "2", refDelytelseId = "1")))

            val resultat = innsending1 + innsending2

            resultat.skyldner() skalVæreLik
                mapOf(
                    1.januar til 31.januar to setOf(SamhandlerId("81111111111")),
                    1.februar til 28.februar to setOf(SamhandlerId("81111111111")),
                )
        }

        test("En ny innsending overstyrer skyldner vi har") {
            val fagsystemId = "${UUID.randomUUID()}"
            val innsending1 =
                oppdragstidslinje(fagsystemId = fagsystemId, linjer = listOf(linje(fom = 1.januar, tom = 31.januar, delytelseId = "1", skyldnerId = "81111111111")))
            val innsending2 =
                oppdragstidslinje(fagsystemId = fagsystemId, linjer = listOf(linje(fom = 1.januar, tom = 31.januar, delytelseId = "2", refDelytelseId = "1", skyldnerId = "81111111112")))

            val resultat = innsending1 + innsending2

            resultat.skyldner() skalVæreLik
                mapOf(1.januar til 31.januar to setOf(SamhandlerId("81111111112")))
        }

        test("Ingen skyldner gir null") {
            val innsending = oppdragstidslinje(linjer = listOf(linje(fom = 1.januar, tom = 31.januar, delytelseId = "1")))
            innsending.skyldner() skalVæreLik mapOf(1.januar til 31.januar to setOf(null))
        }
    })

private fun Oppdragstidslinje.skyldner() = perPeriode { it.skyldner?.opplysning }
