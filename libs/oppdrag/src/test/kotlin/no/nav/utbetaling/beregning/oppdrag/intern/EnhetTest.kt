package no.nav.utbetaling.beregning.oppdrag.intern

import io.kotest.core.spec.style.FunSpec

import no.nav.utbetaling.beregning.oppdrag.BehandlendeEnhet
import no.nav.utbetaling.beregning.oppdrag.Bostedsenhet
import no.nav.utbetaling.beregning.oppdrag.Oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.linje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.perPeriode
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.skalVæreLik
import no.nav.utbetaling.beregning.oppdrag.xml.LinjeEnhet160
import no.nav.utbetaling.beregning.oppdrag.xml.OppdragsEnhet120
import no.nav.utbetaling.beregning.tid.januar
import no.nav.utbetaling.beregning.tid.til

internal class EnhetTest :
    FunSpec({
        context("Enhet på oppdragsnivå") {
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

            test("Hvis behandlende enhet ikke er satt, så skal bostedsenhet brukes som behandlende enhet") {
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
                            ),
                    )

                oppdragstidslinje.enheter() skalVæreLik
                    mapOf(
                        1.januar(2024) til 31.januar(2024) to
                            setOf(
                                Bostedsenhet("1234") to BehandlendeEnhet("1234"),
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
        }

        xtest("Enhet på tvers av oppdrag og linje") {
            val oppdragstidslinje =
                oppdragstidslinje(
                    linjer =
                        listOf(
                            linje(
                                delytelseId = "1",
                                fom = 1.januar(2024),
                                tom = 31.januar(2024),
                                enheter =
                                    listOf(
                                        LinjeEnhet160(
                                            typeEnhet = "BEH",
                                            enhet = "4567",
                                            datoEnhetFom = 10.januar(2024),
                                        ),
                                        LinjeEnhet160(
                                            typeEnhet = "BOS",
                                            enhet = "4321",
                                            datoEnhetFom = 15.januar(2024),
                                        ),
                                    ),
                            ),
                        ),
                    enheter =
                        listOf(
                            OppdragsEnhet120(
                                typeEnhet = "BOS",
                                enhet = "1234",
                                datoEnhetFom = 1.januar(2024),
                            ),
                        ),
                )

            oppdragstidslinje.enheter() skalVæreLik
                mapOf(
                    1.januar(2024) til 9.januar(2024) to
                        setOf(
                            Bostedsenhet("1234") to BehandlendeEnhet("1234"),
                        ),
                    10.januar(2024) til 14.januar(2024) to
                        setOf(
                            Bostedsenhet("1234") to BehandlendeEnhet("4567"),
                        ),
                    15.januar(2024) til 31.januar(2024) to
                        setOf(
                            Bostedsenhet("4321") to BehandlendeEnhet("4567"),
                        ),
                )
        }
    })

private fun Oppdragstidslinje.enheter() = perPeriode { it.bostedsenhet.opplysning to it.behandlendeEnhet.opplysning }
