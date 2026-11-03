package no.nav.utbetaling.beregningsflyt.verifikasjon

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank

class ScenarioMappingTest :
    FunSpec({
        test("Scenario.alle finner og deserialiserer alle scenarioer på classpath") {
            Scenario.alle.shouldNotBeEmpty()
        }

        test("hvert scenario har et navn") {
            Scenario.alle.forEach { scenario ->
                scenario.navn.shouldNotBeBlank()
            }
        }

        test("et kjent scenario er med i Scenario.alle") {
            Scenario.alle.map { it.navn } shouldContain "SkattAarsskifteTest/aarsskifte_frikort"
        }

        test("navnene er unike") {
            val navn = Scenario.alle.map { it.navn }
            navn.toSet().size shouldBe navn.size
        }
    })
