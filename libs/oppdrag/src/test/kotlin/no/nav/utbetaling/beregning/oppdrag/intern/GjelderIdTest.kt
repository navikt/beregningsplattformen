package no.nav.utbetaling.beregning.oppdrag.intern

import io.kotest.core.spec.style.FunSpec

import no.nav.utbetaling.beregning.oppdrag.Oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.Personidentifikator
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.linje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.perPeriode
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.skalVæreLik
import no.nav.utbetaling.beregning.tid.februar
import no.nav.utbetaling.beregning.tid.januar
import no.nav.utbetaling.beregning.tid.til

class GjelderIdTest :
    FunSpec({

        test("Personen oppdraget gjelder med identbytte i løpet av oppdraget") {
            val innsending1 =
                oppdragstidslinje(
                    oppdragGjelderId = "12345678901",
                    oppdragGjelderFom = 1.januar,
                    linjer = listOf(linje(fom = 1.januar, tom = 31.januar, delytelseId = "1")),
                )

            innsending1.gjelder() skalVæreLik
                mapOf(
                    1.januar til 31.januar to setOf(Personidentifikator("12345678901")),
                )

            val innsending2 =
                oppdragstidslinje(
                    oppdragGjelderId = "92345678901",
                    oppdragGjelderFom = 10.februar,
                    linjer = listOf(linje(fom = 1.februar, tom = 29.februar, delytelseId = "2", refDelytelseId = "1")),
                )

            innsending2.gjelder() skalVæreLik
                mapOf(
                    1.februar til 9.februar to setOf(null),
                    10.februar til 29.februar to setOf(Personidentifikator("92345678901")),
                )

            val resultat = innsending1 + innsending2

            resultat.gjelder() skalVæreLik
                mapOf(
                    1.januar til 31.januar to setOf(Personidentifikator("12345678901")),
                    1.februar til 9.februar to setOf(Personidentifikator("12345678901")),
                    10.februar til 29.februar to setOf(Personidentifikator("92345678901")),
                )
        }
    })

private fun Oppdragstidslinje.gjelder() = perPeriode { it.gjelderId?.opplysning }
