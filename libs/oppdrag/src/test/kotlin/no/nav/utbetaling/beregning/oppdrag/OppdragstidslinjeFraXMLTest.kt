package no.nav.utbetaling.beregning.oppdrag

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.Filhjelper
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.assertGjenskaptfraTidslinjer
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.assertSammeOppførselOmHverEnkeltLinjeKommerIEgenInnsending
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.perPeriode
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.satser
import no.nav.utbetaling.beregning.oppdrag.intern.FraXmlFormat
import no.nav.utbetaling.beregning.tid.april
import no.nav.utbetaling.beregning.tid.mai
import no.nav.utbetaling.beregning.tid.mars
import no.nav.utbetaling.beregning.tid.oktober
import no.nav.utbetaling.beregning.tid.september
import no.nav.utbetaling.beregning.tid.til

class OppdragstidslinjeFraXMLTest :
    FunSpec({
        test("Tolker oppdrag med skyldner") {
            val xml = Filhjelper.lesFil("xml/NyttOppdragMedSkyldner.xml")
            val oppdrag = FraXmlFormat.oppdrag110FraXml(xml).values
            oppdrag.size shouldBe 1

            val resultat = FraXmlFormat.opprett(oppdrag.single())
            resultat.tidslinjer.values
                .single()
                .first()
                .second.vedtakssats
                ?.opplysning shouldBe
                Sats(verdi = java.math.BigDecimal("974"), satsType = "")
            resultat.assertGjenskaptfraTidslinjer()

            val utenVedtakssats = xml.replace(Regex("<vedtakssats-157>.*?</vedtakssats-157>", RegexOption.DOT_MATCHES_ALL), "")
            val uten = FraXmlFormat.opprett(FraXmlFormat.oppdrag110FraXml(utenVedtakssats).values.single())
            uten.tidslinjer.values
                .single()
                .first()
                .second.vedtakssats shouldBe null

            (Oppdrag.fraXml(xml) is Oppdrag.Utbetaling) shouldBe true

            assertSammeOppførselOmHverEnkeltLinjeKommerIEgenInnsending(oppdrag.single()) { resultat ->
                resultat.satser() shouldBe mapOf(31.mars(2025) til 11.april(2025) to setOf(974))
                resultat.assertGjenskaptfraTidslinjer()
            }
        }

        test("Tolker oppdrag med opphør av eksisterende linje") {
            val xml = Filhjelper.lesFil("xml/OpphørAvEksisterendeLinje.xml")
            val oppdrag = FraXmlFormat.oppdrag110FraXml(xml).values
            oppdrag.size shouldBe 1

            val resultat = FraXmlFormat.opprett(oppdrag.single())
            resultat.tidslinjer.values
                .single()
                .first()
                .second.vedtakssats
                ?.opplysning shouldBe
                Sats(verdi = java.math.BigDecimal("1000"), satsType = "")
            resultat.assertGjenskaptfraTidslinjer()

            val utenVedtakssats = xml.replace(Regex("<vedtakssats-157>.*?</vedtakssats-157>", RegexOption.DOT_MATCHES_ALL), "")
            val uten = FraXmlFormat.opprett(FraXmlFormat.oppdrag110FraXml(utenVedtakssats).values.single())
            uten.tidslinjer.values
                .single()
                .first()
                .second.vedtakssats shouldBe null

            (Oppdrag.fraXml(xml) is Oppdrag.Utbetaling) shouldBe true

            assertSammeOppførselOmHverEnkeltLinjeKommerIEgenInnsending(oppdrag.single()) { resultat ->
                resultat.satser() shouldBe
                    mapOf(
                        12.mai(2026) til 12.mai(2026) to setOf(300),
                        13.mai(2026) til 15.mai(2026) to setOf(300),
                    )
                resultat.perPeriode { it.status.opplysning } shouldBe
                    mapOf(
                        12.mai(2026) til 12.mai(2026) to setOf(Status.Aktiv),
                        13.mai(2026) til 15.mai(2026) to setOf(Status.Opphørt),
                    )
                resultat.assertGjenskaptfraTidslinjer()
            }
        }

        test("Tolker oppdrag som reaktiverer en opphørt linje") {
            val xml = Filhjelper.lesFil("xml/ReaktiveringAvOpphørtLinje.xml")
            val oppdrag = FraXmlFormat.oppdrag110FraXml(xml).values
            oppdrag.size shouldBe 1

            val resultat = FraXmlFormat.opprett(oppdrag.single())
            resultat.tidslinjer.values
                .single()
                .first()
                .second.vedtakssats
                ?.opplysning shouldBe
                Sats(verdi = java.math.BigDecimal("1000"), satsType = "")
            resultat.assertGjenskaptfraTidslinjer()

            val utenVedtakssats = xml.replace(Regex("<vedtakssats-157>.*?</vedtakssats-157>", RegexOption.DOT_MATCHES_ALL), "")
            val uten = FraXmlFormat.opprett(FraXmlFormat.oppdrag110FraXml(utenVedtakssats).values.single())
            uten.tidslinjer.values
                .single()
                .first()
                .second.vedtakssats shouldBe null

            (Oppdrag.fraXml(xml) is Oppdrag.Utbetaling) shouldBe true

            assertSammeOppførselOmHverEnkeltLinjeKommerIEgenInnsending(oppdrag.single()) { resultat ->
                resultat.satser() shouldBe
                    mapOf(
                        12.mai(2026) til 12.mai(2026) to setOf(300),
                        13.mai(2026) til 15.mai(2026) to setOf(1000),
                    )
                resultat.perPeriode { it.status.opplysning } shouldBe
                    mapOf(
                        12.mai(2026) til 12.mai(2026) to setOf(Status.Opphørt),
                        13.mai(2026) til 15.mai(2026) to setOf(Status.Aktiv),
                    )
                resultat.assertGjenskaptfraTidslinjer()
            }
        }

        test("Tolker oppdrag som endrer en linje og så opphører den") {
            val xml = Filhjelper.lesFil("xml/EndrerOgOpphørerLinje.xml")
            val oppdrag = FraXmlFormat.oppdrag110FraXml(xml).values
            oppdrag.size shouldBe 1

            val resultat = FraXmlFormat.opprett(oppdrag.single())
            // println(resultat.tidslinjer)
            resultat.tidslinjer.values
                .single()
                .first()
                .second.vedtakssats
                ?.opplysning shouldBe
                Sats(verdi = java.math.BigDecimal("1000"), satsType = "")
            resultat.assertGjenskaptfraTidslinjer()

            val utenVedtakssats = xml.replace(Regex("<vedtakssats-157>.*?</vedtakssats-157>", RegexOption.DOT_MATCHES_ALL), "")
            val uten = FraXmlFormat.opprett(FraXmlFormat.oppdrag110FraXml(utenVedtakssats).values.single())
            uten.tidslinjer.values
                .single()
                .first()
                .second.vedtakssats shouldBe null

            (Oppdrag.fraXml(xml) is Oppdrag.Utbetaling) shouldBe true

            assertSammeOppførselOmHverEnkeltLinjeKommerIEgenInnsending(oppdrag.single()) { resultat ->
                resultat.satser() shouldBe
                    mapOf(
                        12.mai(2026) til 14.mai(2026) to setOf(300),
                        15.mai(2026) til 20.mai(2026) to setOf(300),
                    )
                resultat.perPeriode { it.status.opplysning } shouldBe
                    mapOf(
                        12.mai(2026) til 14.mai(2026) to setOf(Status.Aktiv),
                        15.mai(2026) til 20.mai(2026) to setOf(Status.Opphørt),
                    )

                resultat.assertGjenskaptfraTidslinjer()
            }
        }

        test("Tolker nytt oppdrag fra dagpenger") {
            val xml = Filhjelper.lesFil("xml/NyttOppdragDP.xml")
            val oppdrag = FraXmlFormat.oppdrag110FraXml(xml).values
            oppdrag.size shouldBe 1

            val resultat = FraXmlFormat.opprett(oppdrag.single())
            resultat.tidslinjer.values
                .single()
                .first()
                .second.vedtakssats
                ?.opplysning shouldBe
                Sats(verdi = java.math.BigDecimal("1386"), satsType = "")
            resultat.assertGjenskaptfraTidslinjer()

            val utenVedtakssats = xml.replace(Regex("<vedtakssats-157>.*?</vedtakssats-157>", RegexOption.DOT_MATCHES_ALL), "")
            val uten = FraXmlFormat.opprett(FraXmlFormat.oppdrag110FraXml(utenVedtakssats).values.single())
            uten.tidslinjer.values
                .single()
                .first()
                .second.vedtakssats shouldBe null

            (Oppdrag.fraXml(xml) is Oppdrag.Utbetaling) shouldBe true

            assertSammeOppførselOmHverEnkeltLinjeKommerIEgenInnsending(oppdrag.single()) { resultat ->
                resultat.satser() shouldBe
                    mapOf(
                        29.september(2026) til 9.oktober(2026) to setOf(924),
                    )
                resultat.perPeriode { it.status.opplysning } shouldBe
                    mapOf(
                        29.september(2026) til 9.oktober(2026) to setOf(Status.Aktiv),
                    )

                resultat.assertGjenskaptfraTidslinjer()
            }
        }
    })
