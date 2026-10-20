package no.nav.utbetaling.beregning.oppdrag.intern

import java.util.UUID

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

import no.nav.utbetaling.beregning.oppdrag.Status
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.delytelseIdMedStatus
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.linje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.skalVæreLik
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.statuser
import no.nav.utbetaling.beregning.oppdrag.xml.TkodeStatus
import no.nav.utbetaling.beregning.oppdrag.xml.TkodeStatusLinje
import no.nav.utbetaling.beregning.oppdrag.xml.TkodeStatusLinje.OPPH
import no.nav.utbetaling.beregning.tid.februar
import no.nav.utbetaling.beregning.tid.januar
import no.nav.utbetaling.beregning.tid.til

class StatusTest :
    FunSpec({

        test("Opphør på deler av linjen") {
            val fagsystemId = "${UUID.randomUUID()}"
            val førsteInnsending =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    linjer = listOf(linje(delytelseId = "1", fom = 1.januar, tom = 31.januar, sats = 100)),
                )

            førsteInnsending.statuser() shouldBe
                mapOf(
                    januar to setOf(Status.Aktiv),
                )
            val andreInnsending =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    linjer = listOf(linje(delytelseId = "2", refDelytelseId = "1", fom = 1.januar, tom = 31.januar, sats = 100, kodeStatusLinje150 = OPPH, datoStatusFom = 10.januar)),
                )
            val resultat = førsteInnsending + andreInnsending
            resultat.statuser() skalVæreLik
                mapOf(
                    1.januar til 9.januar to setOf(Status.Aktiv),
                    10.januar til 31.januar to setOf(Status.Opphørt),
                )
        }

        test("Status på linjen som strekker seg lengre tilbake kjeden enn linjen selv") {
            val fagsystemId = "${UUID.randomUUID()}"
            val førsteInnsending =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    linjer =
                        listOf(
                            linje(delytelseId = "1", fom = 1.januar, tom = 31.januar, sats = 100),
                            linje(delytelseId = "2", refDelytelseId = "1", fom = 1.februar, tom = 29.februar, sats = 200),
                        ),
                )
            førsteInnsending.statuser() skalVæreLik
                mapOf(
                    januar to setOf(Status.Aktiv),
                    februar to setOf(Status.Aktiv),
                )

            val andreInnsending =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    linjer =
                        listOf(
                            linje(delytelseId = "2", refDelytelseId = "1", fom = 1.februar, tom = 29.februar, sats = 200, kodeStatusLinje150 = TkodeStatusLinje.SPER, datoStatusFom = 15.januar),
                        ),
                )
            val resultat = førsteInnsending + andreInnsending
            resultat.statuser() skalVæreLik
                mapOf(
                    1.januar til 14.januar to setOf(Status.Aktiv),
                    15.januar til 31.januar to setOf(Status.Sperret),
                    februar to setOf(Status.Sperret),
                )
        }

        test("Hviling og reaktivering av linjen") {
            val fagsystemId = "${UUID.randomUUID()}"
            val førsteInnsending =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    linjer = listOf(linje(delytelseId = "1", fom = 1.januar, tom = 31.januar, sats = 100, kodeStatusLinje150 = TkodeStatusLinje.HVIL)),
                )
            førsteInnsending.statuser() shouldBe
                mapOf(
                    januar to setOf(Status.Hvilende),
                )
            val andreInnsending =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    linjer = listOf(linje(delytelseId = "2", refDelytelseId = "1", fom = 1.januar, tom = 31.januar, sats = 100, kodeStatusLinje150 = TkodeStatusLinje.REAK)),
                )
            val resultat = førsteInnsending + andreInnsending
            resultat.statuser() shouldBe
                mapOf(
                    januar to setOf(Status.Aktiv),
                )
        }

        test("Opphør på oppdragsnivå") {
            val fagsystemId = "${UUID.randomUUID()}"
            val førsteInnsending =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    linjer = listOf(linje(delytelseId = "1", fom = 1.januar, tom = 31.januar, sats = 100)),
                )

            førsteInnsending.statuser() shouldBe
                mapOf(
                    januar to setOf(Status.Aktiv),
                )
            val andreInnsending =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    linjer = emptyList(),
                    kodeStatus = TkodeStatus.OPPH,
                    datoStatusFom = 5.januar,
                )
            val resultat = førsteInnsending + andreInnsending
            resultat.statuser() shouldBe
                mapOf(
                    1.januar til 4.januar to setOf(Status.Aktiv),
                    5.januar til 31.januar to setOf(Status.Opphørt),
                )
        }

        test("Opprettelse og opphør på oppdragsnivå") {
            val fagsystemId = "${UUID.randomUUID()}"
            val førsteInnsending =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    linjer =
                        listOf(
                            linje(delytelseId = "1", fom = 1.januar, tom = 31.januar, sats = 100),
                            linje(delytelseId = "2", refDelytelseId = "1", fom = 1.februar, tom = 29.februar, sats = 200),
                        ),
                )
            førsteInnsending.statuser() skalVæreLik
                mapOf(
                    januar to setOf(Status.Aktiv),
                    februar to setOf(Status.Aktiv),
                )
            val andreInnsending =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    linjer =
                        listOf(
                            linje(delytelseId = "3", refDelytelseId = "2", fom = 2.januar, tom = 30.januar, sats = 300),
                            linje(delytelseId = "4", refDelytelseId = "3", fom = 2.februar, tom = 28.februar, sats = 400),
                        ),
                    kodeStatus = TkodeStatus.OPNY,
                )
            val resultat = førsteInnsending + andreInnsending
            resultat.delytelseIdMedStatus() skalVæreLik
                mapOf(
                    1.januar til 1.januar to setOf("1" to Status.Opphørt),
                    2.januar til 30.januar to setOf("3" to Status.Aktiv),
                    2.februar til 28.februar to setOf("4" to Status.Aktiv),
                )
        }

        test("Opphør på kjedenivå") {
            val fagsystemId = "${UUID.randomUUID()}"
            val førsteInnsending =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    linjer =
                        listOf(
                            linje(delytelseId = "1", fom = 1.januar, tom = 31.januar, sats = 100),
                            linje(delytelseId = "2", refDelytelseId = "1", fom = 1.februar, tom = 29.februar, sats = 200),
                        ),
                )
            førsteInnsending.statuser() skalVæreLik
                mapOf(
                    januar to setOf(Status.Aktiv),
                    februar to setOf(Status.Aktiv),
                )
            val andreInnsending =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    linjer =
                        listOf(
                            linje(delytelseId = "3", refDelytelseId = "2", fom = 1.februar, tom = 29.februar, sats = 200, kodeStatusLinje150 = OPPH, datoStatusFom = 1.januar),
                            linje(delytelseId = "4", refDelytelseId = "3", fom = 1.februar, tom = 29.februar, sats = 300),
                        ),
                )
            val resultat = førsteInnsending + andreInnsending
            resultat.delytelseIdMedStatus() skalVæreLik
                mapOf(
                    1.januar til 31.januar to setOf("1" to Status.Opphørt),
                    1.februar til 29.februar to setOf("4" to Status.Aktiv),
                )
        }
    })
