package no.nav.utbetaling.beregning.oppdrag.intern

import java.util.UUID

import io.kotest.core.spec.style.FunSpec

import no.nav.utbetaling.beregning.oppdrag.Oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.Organisasjonsnummer
import no.nav.utbetaling.beregning.oppdrag.Part
import no.nav.utbetaling.beregning.oppdrag.Personidentifikator
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
        test("Skyldner klassifiseres som person, organisasjon eller samhandler") {
            val person =
                oppdragstidslinje(linjer = listOf(linje(fom = 1.januar, tom = 31.januar, delytelseId = "1", skyldnerId = "12345678901")))
            person.skyldner() skalVæreLik
                mapOf(1.januar til 31.januar to setOf(Part.Person(Personidentifikator("12345678901"))))

            val organisasjon =
                oppdragstidslinje(linjer = listOf(linje(fom = 1.januar, tom = 31.januar, delytelseId = "1", skyldnerId = "987654321")))
            organisasjon.skyldner() skalVæreLik
                mapOf(1.januar til 31.januar to setOf(Part.Organisasjon(Organisasjonsnummer("987654321"))))

            val samhandler =
                oppdragstidslinje(linjer = listOf(linje(fom = 1.januar, tom = 31.januar, delytelseId = "1", skyldnerId = "81111111111")))
            samhandler.skyldner() skalVæreLik
                mapOf(1.januar til 31.januar to setOf(Part.Samhandler(SamhandlerId(tssId = "81111111111"))))
        }

        test("Skyldner gjelder fra og med datoSkyldnerFom, ikke datoVedtakFom") {
            val innsending =
                oppdragstidslinje(
                    linjer =
                        listOf(
                            linje(
                                fom = 1.januar,
                                tom = 31.januar,
                                delytelseId = "1",
                                skyldnerId = "12345678901",
                                datoSkyldnerFom = 10.januar,
                            ),
                        ),
                )

            innsending.skyldner() skalVæreLik
                mapOf(
                    1.januar til 9.januar to setOf(null),
                    10.januar til 31.januar to setOf(Part.Person(Personidentifikator("12345678901"))),
                )
        }

        test("Skyldner videreføres når en senere innsending ikke nevner den") {
            val fagsystemId = "${UUID.randomUUID()}"
            val innsending1 =
                oppdragstidslinje(fagsystemId = fagsystemId, linjer = listOf(linje(fom = 1.januar, tom = 31.januar, delytelseId = "1", skyldnerId = "12345678901")))
            val innsending2 =
                oppdragstidslinje(fagsystemId = fagsystemId, linjer = listOf(linje(fom = 1.februar, tom = 28.februar, delytelseId = "2", refDelytelseId = "1")))

            val resultat = innsending1 + innsending2

            resultat.skyldner() skalVæreLik
                mapOf(
                    1.januar til 31.januar to setOf(Part.Person(Personidentifikator("12345678901"))),
                    1.februar til 28.februar to setOf(Part.Person(Personidentifikator("12345678901"))),
                )
        }

        test("En ny innsending overstyrer skyldner vi har") {
            val fagsystemId = "${UUID.randomUUID()}"
            val innsending1 =
                oppdragstidslinje(fagsystemId = fagsystemId, linjer = listOf(linje(fom = 1.januar, tom = 31.januar, delytelseId = "1", skyldnerId = "12345678901")))
            val innsending2 =
                oppdragstidslinje(fagsystemId = fagsystemId, linjer = listOf(linje(fom = 1.januar, tom = 31.januar, delytelseId = "2", refDelytelseId = "1", skyldnerId = "12345678902")))

            val resultat = innsending1 + innsending2

            resultat.skyldner() skalVæreLik
                mapOf(1.januar til 31.januar to setOf(Part.Person(Personidentifikator("12345678902"))))
        }

        test("Ingen skyldner gir null") {
            val innsending = oppdragstidslinje(linjer = listOf(linje(fom = 1.januar, tom = 31.januar, delytelseId = "1")))
            innsending.skyldner() skalVæreLik mapOf(1.januar til 31.januar to setOf(null))
        }
    })

private fun Oppdragstidslinje.skyldner() = perPeriode { it.skyldner?.opplysning }
