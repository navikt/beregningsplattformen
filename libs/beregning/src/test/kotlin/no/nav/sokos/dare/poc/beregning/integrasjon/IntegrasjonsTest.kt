package no.nav.sokos.dare.poc.beregning.integrasjon

import java.io.File

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
        val baseDir =
            File(
                IntegrasjonsTest::class.java.classLoader
                    .getResource("testsupport/integrasjon")!!
                    .toURI(),
            )

        baseDir
            .listFiles { f -> f.isDirectory }
            ?.sorted()
            ?.forEach { kategoriDir ->
                kategoriDir
                    .listFiles { f -> f.isDirectory && File(f, "kontekst.json").exists() }
                    ?.sorted()
                    ?.forEach { testDir ->
                        val testName = "${kategoriDir.name}/${testDir.name}"
                        test(testName) {
                            IntegrasjonsTestHjelper.kjoerBeregning(kategoriDir.name, testDir.name, KlassekodeTestdata.klassekodeLookup.values.toList())
                        }
                    }
            }
    })
