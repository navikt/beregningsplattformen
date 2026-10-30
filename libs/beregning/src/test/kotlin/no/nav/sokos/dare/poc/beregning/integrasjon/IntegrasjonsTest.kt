package no.nav.sokos.dare.poc.beregning.integrasjon

import io.kotest.core.spec.style.FunSpec

import no.nav.sokos.dare.poc.KlassekodeTestdata

/**
 * For å kjøre bare én integrasjonstest i intellij, sett en systemproperty under "run configuration". Endre
 *
 * :beregning:test --tests 'no.nav.sokos.dare.poc.beregning.integrasjon.IntegrasjonsTest'
 *
 * til
 *
 * :beregning:test --tests 'no.nav.sokos.dare.poc.beregning.integrasjon.IntegrasjonsTest' -Dkotest.filter.tests='*AvventTest*'
 * eller
 * :beregning:test --tests 'no.nav.sokos.dare.poc.beregning.integrasjon.IntegrasjonsTest' -Dkotest.filter.tests='*AvventTest/avav_enkel'
 *
 * Dette fungerer også på kommandolinje.
 */
class IntegrasjonsTest :
    FunSpec({
        IntegrasjonsScenario.alle().forEach { scenario ->
            test(scenario.navn) {
                IntegrasjonsTestHjelper.kjoerBeregning(
                    scenario.mappe.parentFile.name,
                    scenario.mappe.name,
                    KlassekodeTestdata.klassekodeLookup.values.toList(),
                )
            }
        }
    })
