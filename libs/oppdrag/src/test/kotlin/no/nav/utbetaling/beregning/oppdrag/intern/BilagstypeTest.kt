package no.nav.utbetaling.beregning.oppdrag.intern

import java.util.UUID

import io.kotest.core.spec.style.FunSpec

import no.nav.utbetaling.beregning.oppdrag.Bilagstype
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.linje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.satserMedBilagstype
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.skalVæreLik
import no.nav.utbetaling.beregning.tid.januar

class BilagstypeTest :
    FunSpec({
        test("Består når neste innsending ikke nevner den") {
            val fagsystemId = "${UUID.randomUUID()}"
            val førsteInnsending = oppdragstidslinje(fagsystemId = fagsystemId, bilagstype = "K231", linjer = listOf(linje(fom = 1.januar, tom = 31.januar, sats = 100, delytelseId = "1")))
            val andreInnsending = oppdragstidslinje(fagsystemId = fagsystemId, linjer = listOf(linje(fom = 1.januar, tom = 31.januar, sats = 200, delytelseId = "2", refDelytelseId = "1")))

            val resultat = førsteInnsending + andreInnsending

            resultat.satserMedBilagstype() skalVæreLik mapOf(januar to setOf(200 to Bilagstype("K231")))
        }

        test("En ny innsending overstyrer den vi har") {
            val fagsystemId = "${UUID.randomUUID()}"
            val førsteInnsending = oppdragstidslinje(fagsystemId = fagsystemId, bilagstype = "K231", linjer = listOf(linje(fom = 1.januar, tom = 31.januar, sats = 100, delytelseId = "1")))
            val andreInnsending =
                oppdragstidslinje(fagsystemId = fagsystemId, bilagstype = "K232", linjer = listOf(linje(fom = 1.januar, tom = 31.januar, sats = 200, delytelseId = "2", refDelytelseId = "1")))

            val resultat = førsteInnsending + andreInnsending

            resultat.satserMedBilagstype() skalVæreLik mapOf(januar to setOf(200 to Bilagstype("K232")))
        }

        test("Endrer ikke på tvers av fagsystemId") {
            val medBilagstype = oppdragstidslinje(fagsystemId = "${UUID.randomUUID()}", bilagstype = "K231", linjer = listOf(linje(fom = 1.januar, tom = 31.januar, sats = 100, delytelseId = "1")))
            val utenBilagstype = oppdragstidslinje(fagsystemId = "${UUID.randomUUID()}", linjer = listOf(linje(fom = 1.januar, tom = 31.januar, sats = 200, delytelseId = "2")))

            val resultat = medBilagstype + utenBilagstype

            resultat.satserMedBilagstype() skalVæreLik mapOf(januar to setOf(100 to Bilagstype("K231"), 200 to null))
        }
    })
