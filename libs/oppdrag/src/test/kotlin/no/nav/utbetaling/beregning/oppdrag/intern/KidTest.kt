package no.nav.utbetaling.beregning.oppdrag.intern

import java.util.UUID

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.linje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.satserMedKid
import no.nav.utbetaling.beregning.tid.januar
import no.nav.utbetaling.beregning.tid.til

class KidTest :
    FunSpec({

        test("Beholder KID når en korreksjon på samme dag ikke opplyser om KID") {
            val fagsystemId = "${UUID.randomUUID()}"
            val førsteInnsending =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    linjer = listOf(linje(delytelseId = "1", fom = 1.januar, tom = 31.januar, sats = 100, kid = "111")),
                )
            val innsendingUtenKid =
                oppdragstidslinje(
                    fagsystemId = fagsystemId,
                    linjer = listOf(linje(delytelseId = "2", refDelytelseId = "1", fom = 10.januar, tom = 31.januar, sats = 200)),
                )

            val resultat = førsteInnsending + innsendingUtenKid
            resultat.satserMedKid() shouldBe
                mapOf(
                    1.januar til 9.januar to setOf(100 to "111"),
                    10.januar til 31.januar to setOf(200 to "111"),
                )
        }
    })
