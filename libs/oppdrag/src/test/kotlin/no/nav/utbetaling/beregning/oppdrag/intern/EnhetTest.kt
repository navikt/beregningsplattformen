package no.nav.utbetaling.beregning.oppdrag.intern

import java.util.UUID

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.assertThrows

import no.nav.utbetaling.beregning.oppdrag.BehandlendeEnhet
import no.nav.utbetaling.beregning.oppdrag.Bostedsenhet
import no.nav.utbetaling.beregning.oppdrag.Oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.linje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.perPeriode
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.skalVæreLik
import no.nav.utbetaling.beregning.oppdrag.xml.OppdragsEnhet120
import no.nav.utbetaling.beregning.tid.februar
import no.nav.utbetaling.beregning.tid.januar
import no.nav.utbetaling.beregning.tid.til

internal class EnhetTest :
    FunSpec({
        test("Behandlende enhet og bostedsenhet er satt") {
            val oppdragstidslinje =
                oppdragstidslinje(
                    linjer =
                        listOf(
                            linje(
                                delytelseId = "1",
                                fom = 1.januar(2024),
                                tom = 31.januar(2024),
                            ),
                        ),
                    enheter =
                        listOf(
                            OppdragsEnhet120(
                                typeEnhet = "BOS",
                                enhet = "1234",
                                datoEnhetFom = 1.januar(2024),
                            ),
                            OppdragsEnhet120(
                                typeEnhet = "BEH",
                                enhet = "4567",
                                datoEnhetFom = 1.januar(2024),
                            ),
                        ),
                )

            oppdragstidslinje.enheter() skalVæreLik
                mapOf(
                    1.januar(2024) til 31.januar(2024) to
                        setOf(
                            Bostedsenhet("1234") to BehandlendeEnhet("4567"),
                        ),
                )
        }

        test("Flere behandlende enheter og bostedsenheter er satt til forskjellige tidspunkt") {
            val oppdragstidslinje =
                oppdragstidslinje(
                    linjer =
                        listOf(
                            linje(
                                delytelseId = "1",
                                fom = 1.januar(2024),
                                tom = 31.januar(2024),
                            ),
                        ),
                    enheter =
                        listOf(
                            OppdragsEnhet120(
                                typeEnhet = "BOS",
                                enhet = "1234",
                                datoEnhetFom = 1.januar(2024),
                            ),
                            OppdragsEnhet120(
                                typeEnhet = "BOS",
                                enhet = "4321",
                                datoEnhetFom = 10.januar(2024),
                            ),
                            OppdragsEnhet120(
                                typeEnhet = "BEH",
                                enhet = "4567",
                                datoEnhetFom = 1.januar(2024),
                            ),
                            OppdragsEnhet120(
                                typeEnhet = "BEH",
                                enhet = "7654",
                                datoEnhetFom = 31.januar(2024),
                            ),
                        ),
                )

            oppdragstidslinje.enheter() skalVæreLik
                mapOf(
                    1.januar(2024) til 9.januar(2024) to
                        setOf(
                            Bostedsenhet("1234") to BehandlendeEnhet("4567"),
                        ),
                    10.januar(2024) til 30.januar(2024) to
                        setOf(
                            Bostedsenhet("4321") to BehandlendeEnhet("4567"),
                        ),
                    31.januar(2024) til 31.januar(2024) to
                        setOf(
                            Bostedsenhet("4321") to BehandlendeEnhet("7654"),
                        ),
                )
        }

        test("Feiler om man opplyser om enhet senere enn linjene i oppdraget") {
            val feil =
                assertThrows<IllegalArgumentException> {
                    oppdragstidslinje(
                        linjer =
                            listOf(
                                linje(
                                    delytelseId = "1",
                                    fom = 10.januar,
                                    tom = 31.januar,
                                ),
                            ),
                        enheter = listOf(OppdragsEnhet120(typeEnhet = "BOS", enhet = "1234", datoEnhetFom = 1.februar)),
                    )
                }

            feil.message shouldBe "Oppgitt BOS-enhet fra og med Fra og med 01-02-2024, men siste linje slutter 2024-01-31."
        }

        test("Fjerne behandlende enhet hvis man setter den til null") {
            val fagsystemId = "${UUID.randomUUID()}"

            val førsteInnsending =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    linjer =
                        listOf(
                            linje(
                                delytelseId = "1",
                                fom = 1.januar,
                                tom = 31.januar,
                            ),
                        ),
                    enheter =
                        listOf(
                            OppdragsEnhet120(typeEnhet = "BOS", enhet = "1234", datoEnhetFom = 1.januar),
                            OppdragsEnhet120(typeEnhet = "BEH", enhet = "4567", datoEnhetFom = 1.januar),
                        ),
                )

            førsteInnsending.enheter() skalVæreLik
                mapOf(
                    1.januar til 31.januar to
                        setOf(
                            Bostedsenhet("1234") to BehandlendeEnhet("4567"),
                        ),
                )

            val andreInnsending =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    linjer =
                        listOf(
                            linje(
                                delytelseId = "1",
                                fom = 1.januar,
                                tom = 31.januar,
                            ),
                        ),
                    enheter =
                        listOf(
                            OppdragsEnhet120(typeEnhet = "BEH", enhet = null, datoEnhetFom = 1.januar),
                        ),
                )

            val resultat = førsteInnsending + andreInnsending

            resultat.enheter() skalVæreLik
                mapOf(
                    1.januar til 31.januar to
                        setOf(
                            Bostedsenhet("1234") to null,
                        ),
                )
        }
    })

private fun Oppdragstidslinje.enheter() = perPeriode { it.bostedsenhet?.opplysning to it.behandlendeEnhet?.opplysning }
