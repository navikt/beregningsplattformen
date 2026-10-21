package no.nav.utbetaling.beregning.oppdrag

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.Filhjelper
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.assertSammeOppførselOmHverEnkeltLinjeKommerIEgenInnsending
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

            (Oppdrag.fraJson(json) is Oppdrag.Utbetaling) shouldBe true

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
