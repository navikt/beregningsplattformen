package no.nav.utbetaling.beregning.oppdrag

import java.math.BigDecimal

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.Filhjelper
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.assertSammeOppførselOmHverEnkeltLinjeKommerIEgenInnsending
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.perPeriode
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.satser
import no.nav.utbetaling.beregning.oppdrag.intern.FraXmlFormat
import no.nav.utbetaling.beregning.tid.februar
import no.nav.utbetaling.beregning.tid.mars
import no.nav.utbetaling.beregning.tid.til

class OppdragstidslinjeFraJSONTest :
    FunSpec({
        test("Tolker oppdrag med komplisert kjeding") {
            val json = Filhjelper.lesFil("json/oppdrag_med_komplisert_kjeding.json")
            val oppdrag = FraXmlFormat.oppdrag110FraJson(json).values
            oppdrag.size shouldBe 1

            val resultat = FraXmlFormat.opprett(oppdrag.single())
            resultat.tidslinjer.values.first().first().second.vedtaksSats?.opplysning shouldBe
                Sats(verdi = java.math.BigDecimal("974"), satsType = "")

            (Oppdrag.fraJson(json) is Oppdrag.Utbetaling) shouldBe true
            val offentligResultat = (Oppdrag.fraJson(json) as Oppdrag.Utbetaling).oppdragstidslinje
            offentligResultat.perPeriode { it.sats.opplysning.satsType }.values.toSet() shouldBe
                setOf(setOf(YtelseSatstype.DAG.kode))
            offentligResultat.perPeriode { it.vedtaksSats } shouldBe
                mapOf(
                    1.februar(2026) til 14.februar(2026) to setOf(Opplysning(SaksbehandlerId("Kelvin"), Sats(BigDecimal("974"), ""))),
                    15.februar(2026) til 21.februar(2026) to setOf(Opplysning(SaksbehandlerId("Kelvin"), Sats(BigDecimal("1001"), ""))),
                    24.februar(2026) til 26.februar(2026) to setOf(Opplysning(SaksbehandlerId("Kelvin"), Sats(BigDecimal("1021"), ""))),
                    27.februar(2026) til 2.mars(2026) to setOf(Opplysning(SaksbehandlerId("Kelvin"), Sats(BigDecimal("742"), ""))),
                )

            assertSammeOppførselOmHverEnkeltLinjeKommerIEgenInnsending(oppdrag.single()) { resultat ->
                resultat.satser() shouldBe
                    mapOf(
                        1.februar(2026) til 14.februar(2026) to setOf(974),
                        15.februar(2026) til 21.februar(2026) to setOf(1001),
                        24.februar(2026) til 26.februar(2026) to setOf(1021),
                        27.februar(2026) til 2.mars(2026) to setOf(742),
                    )
            }
        }
    })
