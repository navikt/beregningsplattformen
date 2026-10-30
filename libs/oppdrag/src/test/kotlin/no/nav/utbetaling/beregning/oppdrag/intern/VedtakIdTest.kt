package no.nav.utbetaling.beregning.oppdrag.intern

import java.util.UUID

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

import no.nav.utbetaling.beregning.oppdrag.Oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.linje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.perPeriode
import no.nav.utbetaling.beregning.tid.januar
import no.nav.utbetaling.beregning.tid.til

class VedtakIdTest :
    FunSpec({

        test("Beholder vedtakId fra forrige linje når ny linje ikke har vedtakId") {
            val fagsystemId = "${UUID.randomUUID()}"
            val førsteInnsending =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    linjer = listOf(linje(delytelseId = "1", fom = 1.januar, tom = 31.januar, sats = 100, vedtakId = "111")),
                )
            val innsendingUtenVedtakId =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    linjer = listOf(linje(delytelseId = "2", refDelytelseId = "1", fom = 10.januar, tom = 31.januar, sats = 200)),
                )

            val resultat = førsteInnsending + innsendingUtenVedtakId
            resultat.satserMedVedtakId() shouldBe
                mapOf(
                    1.januar til 9.januar to setOf(100 to "111"),
                    // TODO sjekk om dette er riktig, eller om det skal være null. Det er ikke mulig å sende inn en linje uten vedtakId, så det er ikke sikkert at dette er et relevant scenario.
                    10.januar til 31.januar to setOf(200 to "111"),
                )
        }
    })

private fun Oppdragstidslinje.satserMedVedtakId() = perPeriode { it.sats.opplysning.verdi.toInt() to it.vedtakId?.opplysning?.id }
