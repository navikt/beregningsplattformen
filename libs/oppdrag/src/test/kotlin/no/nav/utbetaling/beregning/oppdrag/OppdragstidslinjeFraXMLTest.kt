package no.nav.utbetaling.beregning.oppdrag

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.Filhjelper
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.assertGjenskaptfraTidslinjer
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.satser
import no.nav.utbetaling.beregning.tid.Periode.LukketPeriode.Companion.til
import no.nav.utbetaling.beregning.tid.april
import no.nav.utbetaling.beregning.tid.mars

/**
 * Tester for å bygge en Oppdragstidslinje fra et Oppdrag XML
 */
class OppdragstidslinjeFraXMLTest :
    FunSpec({
        context("Bygger tidslinje fra et enkelt oppdrag") {
            test("Leser et oppdrag fra XML") {
                val xml = Filhjelper.lesFil("xml/NyttOppdragMedSkyldnerOgKravhaver.xml")
                val tidslinje = fraXml(xml)
                tidslinje.satser() shouldBe mapOf(31.mars(2025) til 11.april(2025) to setOf(974))
                tidslinje.assertGjenskaptfraTidslinjer()
            }
        }
    })

private fun fraXml(vararg xml: String): Oppdragstidslinje =
    when (val oppdrag = Oppdrag.fraXml(*xml)) {
        is Oppdrag.Utbetaling -> oppdrag.oppdragstidslinje
        is Oppdrag.Simulering -> oppdrag.oppdragstidslinje
        is Oppdrag.UgyldigFormat -> throw oppdrag.feil
        is Oppdrag.UgyldigKombinasjon -> throw oppdrag.feil
        is Oppdrag.UgyldigOppdrag -> throw oppdrag.feil
    }
