package no.nav.utbetaling.beregning.oppdrag.intern

import java.util.UUID

import io.kotest.core.spec.style.FunSpec

import no.nav.utbetaling.beregning.oppdrag.Oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.Organisasjonsnummer
import no.nav.utbetaling.beregning.oppdrag.Personidentifikator
import no.nav.utbetaling.beregning.oppdrag.SamhandlerId
import no.nav.utbetaling.beregning.oppdrag.UtbetalesTil
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.linje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.perPeriode
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.skalVæreLik
import no.nav.utbetaling.beregning.tid.februar
import no.nav.utbetaling.beregning.tid.januar
import no.nav.utbetaling.beregning.tid.til

class UtbetalesTilIdTest :
    FunSpec({
        test("Utbetales til person som endres i løpet av oppdraget") {
            val fagsystemId = "${UUID.randomUUID()}"
            val innsending1 =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    linjer =
                        listOf(
                            linje(
                                fom = 1.januar,
                                tom = 31.januar,
                                delytelseId = "1",
                                utbetalesTilId = "12345678901",
                            ),
                        ),
                )

            innsending1.utbetalesTil() skalVæreLik
                mapOf(
                    1.januar til 31.januar to setOf(UtbetalesTil.Person(Personidentifikator("12345678901"))),
                )

            val innsending2 =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    linjer =
                        listOf(
                            linje(
                                fom = 1.februar,
                                tom = 29.februar,
                                delytelseId = "2",
                                refDelytelseId = "1",
                                utbetalesTilId = "12345678902",
                                datoUtbetalesTilIdFom = 10.februar,
                            ),
                        ),
                )

            innsending2.utbetalesTil() skalVæreLik
                mapOf(
                    1.februar til 9.februar to setOf(null),
                    10.februar til 29.februar to setOf(UtbetalesTil.Person(Personidentifikator("12345678902"))),
                )

            val resultat = innsending1 + innsending2

            resultat.utbetalesTil() skalVæreLik
                mapOf(
                    1.januar til 31.januar to setOf(UtbetalesTil.Person(Personidentifikator("12345678901"))),
                    1.februar til 9.februar to setOf(UtbetalesTil.Person(Personidentifikator("12345678901"))),
                    10.februar til 29.februar to setOf(UtbetalesTil.Person(Personidentifikator("12345678902"))),
                )
        }

        test("Utbetales til organisasjon") {
            val innsending =
                oppdragstidslinje(
                    linjer =
                        listOf(
                            linje(
                                fom = 1.januar,
                                tom = 31.januar,
                                delytelseId = "1",
                                utbetalesTilId = "987654321",
                            ),
                        ),
                )
            innsending.utbetalesTil() skalVæreLik
                mapOf(
                    1.januar til 31.januar to setOf(UtbetalesTil.Organisasjon(Organisasjonsnummer("987654321"))),
                )
        }

        test("Utbetales til samhandler") {
            val innsending =
                oppdragstidslinje(
                    linjer =
                        listOf(
                            linje(
                                fom = 1.januar,
                                tom = 31.januar,
                                delytelseId = "1",
                                utbetalesTilId = "81111111111",
                            ),
                        ),
                )

            innsending.utbetalesTil() skalVæreLik
                mapOf(
                    1.januar til 31.januar to setOf(UtbetalesTil.Samhandler(SamhandlerId(tssId = "81111111111"))),
                )
        }
    })

private fun Oppdragstidslinje.utbetalesTil() = perPeriode { it.utbetalesTil?.opplysning }
