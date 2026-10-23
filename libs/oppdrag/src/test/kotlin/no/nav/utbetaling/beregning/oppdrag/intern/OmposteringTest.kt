package no.nav.utbetaling.beregning.oppdrag.intern

import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

import no.nav.utbetaling.beregning.oppdrag.Oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.erLik
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.linje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.perPeriode
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.skalVæreLik
import no.nav.utbetaling.beregning.oppdrag.xml.Ompostering116
import no.nav.utbetaling.beregning.tid.januar
import no.nav.utbetaling.beregning.tid.til

class OmposteringTest :
    FunSpec({
        test("Uten fom dato gjelder alle dagene i oppdraget") {
            val innsending =
                oppdragstidslinje(
                    ompostering = ompostering(),
                    linjer = listOf(linje(delytelseId = "1", fom = 1.januar, tom = 31.januar)),
                )

            innsending.skalOmposteres() skalVæreLik mapOf(januar to setOf(true))
        }

        test("Med fom dato gjelder den bare fra og med fom datoen") {
            val innsending =
                oppdragstidslinje(
                    ompostering = ompostering(fom = 10.januar),
                    linjer = listOf(linje(delytelseId = "1", fom = 1.januar, tom = 31.januar)),
                )

            innsending.skalOmposteres() skalVæreLik
                mapOf(
                    1.januar til 9.januar to setOf(false),
                    10.januar til 31.januar to setOf(true),
                )
        }

        test("Ompostering består når neste innsending ikke endrer den") {
            val fagsystemId = "${UUID.randomUUID()}"

            val førsteInnsending =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    ompostering = ompostering(),
                    linjer = listOf(linje(delytelseId = "1", fom = 1.januar, tom = 31.januar)),
                )

            val andreInnsending =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    linjer = listOf(linje(delytelseId = "2", refDelytelseId = "1", fom = 1.januar, tom = 31.januar)),
                )

            val resultat = førsteInnsending + andreInnsending

            resultat.skalOmposteres() skalVæreLik mapOf(januar to setOf(true))
        }

        test("Ompostering består med sin egen fom dato når neste innsending ikke endrer den") {
            val fagsystemId = "${UUID.randomUUID()}"

            val førsteInnsending =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    ompostering = ompostering(fom = 10.januar),
                    linjer = listOf(linje(delytelseId = "1", fom = 1.januar, tom = 31.januar)),
                )

            val andreInnsending =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    linjer = listOf(linje(delytelseId = "2", refDelytelseId = "1", fom = 1.januar, tom = 31.januar)),
                )

            val resultat = førsteInnsending + andreInnsending

            resultat.skalOmposteres() skalVæreLik
                mapOf(
                    1.januar til 9.januar to setOf(false),
                    10.januar til 31.januar to setOf(true),
                )
        }

        test("En ny innsending overstyrer bare fra og med sin egen fom dato") {
            val fagsystemId = "${UUID.randomUUID()}"

            val førsteInnsending =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    ompostering = ompostering(registrert = "2025-01-15-12.34.56.123456"),
                    linjer = listOf(linje(delytelseId = "1", fom = 1.januar, tom = 31.januar)),
                )

            val andreInnsending =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    ompostering = ompostering(fom = 10.januar, registrert = "2026-01-15-12.34.56.123456"),
                    linjer = listOf(linje(delytelseId = "2", refDelytelseId = "1", fom = 1.januar, tom = 31.januar)),
                )

            val resultat = førsteInnsending + andreInnsending

            resultat.omposeringRegistreringsår() skalVæreLik
                mapOf(
                    1.januar til 9.januar to setOf(2025),
                    10.januar til 31.januar to setOf(2026),
                )
        }

        test("En helt lik ompostering med feilreg opphever den vi har") {
            val fagsystemId = "${UUID.randomUUID()}"

            val førsteInnsending =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    ompostering = ompostering(fom = 10.januar),
                    linjer = listOf(linje(delytelseId = "1", fom = 1.januar, tom = 31.januar)),
                )

            val opphever =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    ompostering = ompostering(fom = 10.januar, feilreg = "J"),
                    linjer = listOf(linje(delytelseId = "2", refDelytelseId = "1", fom = 1.januar, tom = 31.januar)),
                )

            val resultat = førsteInnsending + opphever

            resultat.skalOmposteres() skalVæreLik mapOf(januar to setOf(false))
        }

        test("En feilreg som ikke matcher noe vi registrert har kaster exception") {
            val fagsystemId = "${UUID.randomUUID()}"

            val førsteInnsending =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    linjer = listOf(linje(delytelseId = "1", fom = 1.januar, tom = 31.januar)),
                )

            val opphever =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    ompostering = ompostering(fom = 20.januar, feilreg = "J"),
                    linjer = listOf(linje(delytelseId = "2", refDelytelseId = "1", fom = 1.januar, tom = 31.januar)),
                )

            val exception =
                shouldThrow<IllegalArgumentException> {
                    førsteInnsending + opphever
                }

            exception.message shouldBe "Kan ikke feilregistrere en endring som ikke finnes."
        }

        test("En ompostering med omPostering N er det samme som å ikke sende noe ompostering") {
            val fagsystemId = "${UUID.randomUUID()}"

            val utenOmpostering =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    linjer = listOf(linje(delytelseId = "1", fom = 1.januar, tom = 31.januar)),
                )

            val medOmposteringN =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    ompostering = ompostering(fom = 20.januar, kode = "N"),
                    linjer = listOf(linje(delytelseId = "1", fom = 1.januar, tom = 31.januar)),
                )

            utenOmpostering erLik medOmposteringN
        }
    })

private fun Oppdragstidslinje.skalOmposteres() = perPeriode { it.ompostering?.opplysning != null }

private fun Oppdragstidslinje.omposeringRegistreringsår() =
    perPeriode {
        it.ompostering
            ?.opplysning
            ?.registrert
            ?.atOffset(ZoneOffset.UTC)
            ?.year
    }

private fun ompostering(
    fom: LocalDate? = null,
    kode: String = "J",
    feilreg: String? = null,
    // Ser ut til at FP-Sak faktisk sender oss en timestamp
    // https://github.com/navikt/fp-ws-proxy/blob/fb3ee8d4dd38b46f85484086d6413775bc83da09/src/main/java/no/nav/foreldrepenger/ws/proxy/api/simulering/mapper/OppdragMapper.java#L71
    registrert: String = "2024-01-15-12.34.56.123456",
    saksbehandlerId: String = "Z999999",
) = Ompostering116(omPostering = kode, datoOmposterFom = fom, feilreg = feilreg, tidspktReg = registrert, saksbehId = saksbehandlerId)
