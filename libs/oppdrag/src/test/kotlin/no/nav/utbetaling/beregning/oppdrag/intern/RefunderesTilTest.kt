package no.nav.utbetaling.beregning.oppdrag.intern

import java.util.UUID

import io.kotest.core.spec.style.FunSpec

import no.nav.utbetaling.beregning.oppdrag.Oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.Organisasjonsnummer
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.linje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.perPeriode
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.skalVæreLik
import no.nav.utbetaling.beregning.tid.februar
import no.nav.utbetaling.beregning.tid.januar
import no.nav.utbetaling.beregning.tid.til

class RefunderesTilTest :
    FunSpec({
        test("Refusjonsinfo grupperes og gjelder fra og med datoFom") {
            val innsending =
                oppdragstidslinje(
                    linjer =
                        listOf(
                            linje(
                                fom = 1.januar,
                                tom = 31.januar,
                                delytelseId = "1",
                                refunderesId = "999999999",
                                refusjonDatoFom = 10.januar,
                            ),
                        ),
                )

            innsending.refunderesTil() skalVæreLik
                mapOf(
                    1.januar til 9.januar to setOf(null),
                    10.januar til 31.januar to setOf(Organisasjonsnummer("999999999")),
                )
        }

        test("Refusjonsinfo uten refunderesId gir null refunderes") {
            val innsending =
                oppdragstidslinje(linjer = listOf(linje(fom = 1.januar, tom = 31.januar, delytelseId = "1")))

            innsending.refunderesTil() skalVæreLik
                mapOf(1.januar til 31.januar to setOf(null))
        }

        test("Refusjonsinfo videreføres når en senere innsending ikke nevner den") {
            val fagsystemId = "${UUID.randomUUID()}"
            val innsending1 =
                oppdragstidslinje(fagsystemId = fagsystemId, linjer = listOf(linje(fom = 1.januar, tom = 31.januar, delytelseId = "1", refunderesId = "999999999")))
            val innsending2 =
                oppdragstidslinje(fagsystemId = fagsystemId, linjer = listOf(linje(fom = 1.februar, tom = 28.februar, delytelseId = "2", refDelytelseId = "1")))

            val resultat = innsending1 + innsending2

            val forventet = Organisasjonsnummer("999999999")
            resultat.refunderesTil() skalVæreLik
                mapOf(
                    1.januar til 31.januar to setOf(forventet),
                    1.februar til 28.februar to setOf(forventet),
                )
        }

        test("Ingen refusjonsinfo gir null") {
            val innsending = oppdragstidslinje(linjer = listOf(linje(fom = 1.januar, tom = 31.januar, delytelseId = "1")))
            innsending.refunderesTil() skalVæreLik mapOf(1.januar til 31.januar to setOf(null))
        }
    })

private fun Oppdragstidslinje.refunderesTil() = perPeriode { it.refunderesTil?.opplysning }
