package no.nav.utbetaling.beregning.oppdrag

import java.math.BigDecimal

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.Filhjelper
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.assertGjenskaptfraTidslinjer
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.assertSammeOppførselOmHverEnkeltLinjeKommerIEgenInnsending
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.satser
import no.nav.utbetaling.beregning.oppdrag.intern.FraXmlFormat
import no.nav.utbetaling.beregning.tid.april
import no.nav.utbetaling.beregning.tid.mars
import no.nav.utbetaling.beregning.tid.til

class OppdragstidslinjeFraXMLTest :
    FunSpec({
        test("Tolker oppdrag med skyldner") {
            val xml = Filhjelper.lesFil("xml/NyttOppdragMedSkyldner.xml")
            val oppdrag = FraXmlFormat.oppdrag110FraXml(xml).values
            oppdrag.size shouldBe 1

            val resultat = FraXmlFormat.opprett(oppdrag.single())
            resultat.tidslinjer.values.single().first().second.vedtaksSats?.opplysning shouldBe
                Sats(verdi = java.math.BigDecimal("974"), satsType = "")
            resultat.assertGjenskaptfraTidslinjer()

            val utenVedtakssats = xml.replace(Regex("<vedtakssats-157>.*?</vedtakssats-157>", RegexOption.DOT_MATCHES_ALL), "")
            val uten = FraXmlFormat.opprett(FraXmlFormat.oppdrag110FraXml(utenVedtakssats).values.single())
            uten.tidslinjer.values.single().first().second.vedtaksSats shouldBe null

            (Oppdrag.fraXml(xml) is Oppdrag.Utbetaling) shouldBe true
            val offentligResultat = (Oppdrag.fraXml(xml) as Oppdrag.Utbetaling).oppdragstidslinje
            val opplysninger =
                offentligResultat.tidslinjer.values
                    .single()
                    .single()
                    .second
            opplysninger.sats shouldBe Opplysning(SaksbehandlerId("Kelvin"), Sats(BigDecimal("974"), YtelseSatstype.DAG.kode))
            opplysninger.vedtaksSats shouldBe Opplysning(SaksbehandlerId("Kelvin"), Sats(BigDecimal("974"), ""))

            assertSammeOppførselOmHverEnkeltLinjeKommerIEgenInnsending(oppdrag.single()) { resultat ->
                resultat.satser() shouldBe mapOf(31.mars(2025) til 11.april(2025) to setOf(974))
                resultat.assertGjenskaptfraTidslinjer()
            }
        }
    })
