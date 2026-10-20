package no.nav.utbetaling.beregning.oppdrag.intern

import java.util.UUID

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.linje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.satserMedForfall
import no.nav.utbetaling.beregning.tid.januar
import no.nav.utbetaling.beregning.tid.mars

class ForfallsdatoTest :
    FunSpec({
        test("Består når neste innsending ikke nevner den") {
            val fagsystemId = "${UUID.randomUUID()}"
            val førsteInnsending = oppdragstidslinje(fagsystemId = fagsystemId, forfall = 15.mars, linjer = listOf(linje(delytelseId = "1", fom = 1.januar, tom = 31.januar, sats = 100)))
            val andreInnsending = oppdragstidslinje(fagsystemId = fagsystemId, linjer = listOf(linje(delytelseId = "2", refDelytelseId = "1", fom = 1.januar, tom = 31.januar, sats = 200)))

            val resultat = førsteInnsending + andreInnsending

            resultat.satserMedForfall() shouldBe mapOf(januar to setOf(200 to 15.mars))
        }

        test("En ny innsending overstyrer den vi har") {
            val fagsystemId = "${UUID.randomUUID()}"
            val førsteInnsending = oppdragstidslinje(fagsystemId = fagsystemId, forfall = 15.mars, linjer = listOf(linje(delytelseId = "1", fom = 1.januar, tom = 31.januar, sats = 100)))
            val andreInnsending =
                oppdragstidslinje(fagsystemId = fagsystemId, forfall = 20.mars, linjer = listOf(linje(delytelseId = "2", refDelytelseId = "1", fom = 1.januar, tom = 31.januar, sats = 200)))

            val resultat = førsteInnsending + andreInnsending

            resultat.satserMedForfall() shouldBe mapOf(januar to setOf(200 to 20.mars))
        }

        test("Endres ikke på tvers av fagsystemId") {
            val medForfall = oppdragstidslinje(fagsystemId = "${UUID.randomUUID()}", forfall = 15.mars, linjer = listOf(linje(delytelseId = "1", fom = 1.januar, tom = 31.januar, sats = 100)))
            val utenForfall = oppdragstidslinje(fagsystemId = "${UUID.randomUUID()}", linjer = listOf(linje(delytelseId = "2", fom = 1.januar, tom = 31.januar, sats = 200)))

            val resultat = medForfall + utenForfall

            resultat.satserMedForfall() shouldBe mapOf(januar to setOf(100 to 15.mars, 200 to null))
        }
    })
