package no.nav.utbetaling.beregning.oppdrag.intern

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.linje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.oppdrag110
import no.nav.utbetaling.beregning.oppdrag.xml.OppdragsLinje150
import no.nav.utbetaling.beregning.tid.Periode.ÅpenPeriode
import no.nav.utbetaling.beregning.tid.februar
import no.nav.utbetaling.beregning.tid.januar
import no.nav.utbetaling.beregning.tid.mars
import no.nav.utbetaling.beregning.tid.til

class LinjerTest :
    FunSpec({
        test("sorterer kjeden etter referansene, uavhengig av input-rekkefølge") {
            val eldste =
                linje(
                    delytelseId = "1",
                    fom = 1.januar,
                    tom = 31.januar,
                    sats = 10,
                )
            val mellomste =
                linje(
                    delytelseId = "2",
                    refDelytelseId = "1",
                    fom = 1.februar,
                    tom = 28.februar,
                    sats = 20,
                )
            val nyeste =
                linje(
                    delytelseId = "3 ",
                    refDelytelseId = "2",
                    fom = 1.mars,
                    tom = null,
                    sats = 30,
                )
            val oppdrag = oppdrag(nyeste, eldste, mellomste)

            val aktiveLinjer =
                Linjer
                    .fra(oppdrag)
                    .values
                    .single()
                    .aktive

            aktiveLinjer[15.januar] shouldBe eldste
            aktiveLinjer[15.februar] shouldBe mellomste
            aktiveLinjer[15.mars] shouldBe nyeste
        }

        test("bruker første linje som eldste når rotlinjen ikke er med") {
            val eldste = linje(delytelseId = "2", refDelytelseId = "1", fom = 1.januar, tom = null)
            val nyeste = linje(delytelseId = "3", refDelytelseId = "2", fom = 1.februar, tom = null)

            val aktiveLinjer =
                Linjer
                    .fra(oppdrag(eldste, nyeste))
                    .values
                    .single()
                    .aktive

            aktiveLinjer.toList() shouldBe
                listOf(
                    (1.januar til 31.januar) to eldste,
                    ÅpenPeriode(1.februar) to nyeste,
                )
        }

        test("avviser to linjer som peker på samme ID") {
            val første = linje(delytelseId = "2", refDelytelseId = "1", fom = 1.januar, tom = null)
            val andre = linje(delytelseId = "3", refDelytelseId = "1", fom = 1.februar, tom = null)

            val feil = shouldThrow<IllegalArgumentException> { Linjer.fra(oppdrag(første, andre)) }

            feil.message shouldBe "Flere linjer kan ikke peke på samme linje"
        }

        listOf(null, "tidligere").forEach { referanse ->
            test("beholder en enkelt linje med referanse $referanse") {
                val eneste = linje(delytelseId = "1", refDelytelseId = referanse, fom = 1.januar, tom = null)

                val aktiveLinjer =
                    Linjer
                        .fra(oppdrag(eneste))
                        .values
                        .single()
                        .aktive

                aktiveLinjer.toList() shouldBe listOf(ÅpenPeriode(1.januar) to eneste)
            }
        }

        test("nyere linje avgrenser en eldre overlappende periode") {
            val eldste =
                linje(
                    delytelseId = "1",
                    fom = 1.januar,
                    tom = 31.mars,
                )
            val nyeste =
                linje(
                    delytelseId = "2",
                    refDelytelseId = "1",
                    fom = 1.februar,
                    tom = 28.februar,
                )

            val aktiveLinjer =
                Linjer
                    .fra(oppdrag(nyeste, eldste))
                    .values
                    .single()
                    .aktive

            aktiveLinjer.toList() shouldBe
                listOf(
                    (1.januar til 31.januar) to eldste,
                    (1.februar til 28.februar) to nyeste,
                )
            aktiveLinjer[1.mars] shouldBe null
        }

        test("gir feil når neste linje mangler etter fallback til første linje") {
            val eldste = linje(delytelseId = "2", refDelytelseId = "1", fom = 1.januar, tom = null)
            val nyeste = linje(delytelseId = "3", refDelytelseId = "2", fom = 1.februar, tom = null)

            val feil = shouldThrow<IllegalStateException> { Linjer.fra(oppdrag(nyeste, eldste)) }

            feil.message shouldBe "Fant ikke neste linje i kjeden"
        }
    })

private fun oppdrag(vararg linjer: OppdragsLinje150) =
    oppdrag110(
        linjer = linjer.toList(),
    )
