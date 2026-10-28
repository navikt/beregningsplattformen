package no.nav.utbetaling.beregning.oppdrag.intern

import java.util.UUID

import io.kotest.core.spec.style.FunSpec

import no.nav.utbetaling.beregning.oppdrag.Oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.YtelseKlassekode
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.linje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.perPeriode
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.skalVæreLik
import no.nav.utbetaling.beregning.tid.januar
import no.nav.utbetaling.beregning.tid.til

class YtelseKlassekodeTest :
    FunSpec({

        test("Klassekode uten fra og med dato") {
            val oppdragstidslinje =
                oppdragstidslinje(
                    linjer =
                        listOf(
                            linje(
                                fom = 1.januar,
                                tom = 31.januar,
                                kodeKlassifik = "AAPAU",
                            ),
                        ),
                )

            oppdragstidslinje.klassekoder() skalVæreLik
                mapOf(
                    1.januar til 31.januar to setOf(YtelseKlassekode("AAPAU")),
                )
        }

        test("Klassekode som endrer seg ved en senere innsending") {
            val fagsystemId = "${UUID.randomUUID()}"

            val innsending1 =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    linjer =
                        listOf(
                            linje(
                                fom = 1.januar,
                                tom = 31.januar,
                                kodeKlassifik = "AAPAU",
                                delytelseId = "1",
                            ),
                        ),
                )

            val innsending2 =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    linjer =
                        listOf(
                            linje(
                                fom = 1.januar,
                                tom = 15.januar,
                                kodeKlassifik = "AAPAU",
                                delytelseId = "2",
                                refDelytelseId = "1",
                            ),
                            linje(
                                fom = 16.januar,
                                tom = 31.januar,
                                kodeKlassifik = "AAPFAA",
                                delytelseId = "3",
                                refDelytelseId = "2",
                            ),
                        ),
                )

            val resultat = innsending1 + innsending2

            resultat.klassekoder() skalVæreLik
                mapOf(
                    1.januar til 15.januar to setOf(YtelseKlassekode("AAPAU")),
                    16.januar til 31.januar to setOf(YtelseKlassekode("AAPFAA")),
                )
        }
    })

private fun Oppdragstidslinje.klassekoder() = perPeriode { it.ytelseKlassekode.opplysning }
