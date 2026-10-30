package no.nav.utbetaling.beregning.mottak.config

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.Tag

import no.nav.utbetaling.beregning.oppdrag.Oppdrag
import no.nav.utbetaling.beregning.oppdrag.Oppdragstidslinje

class MetricsTest :
    FunSpec({
        beforeEach {
            resettCounters()
        }

        afterEach {
            resettCounters()
        }

        listOf("utbetaling", "simulering", "ugyldig_oppdrag", "ugyldig_kombinasjon").forEach { type ->
            test("registrerer felt og tagger for $type") {
                val metrikk = metrikk()
                val metrikker = listOf(metrikk())
                val oppdrag =
                    when (type) {
                        "utbetaling" -> Oppdrag.Utbetaling(Oppdragstidslinje(), metrikker)
                        "simulering" -> Oppdrag.Simulering(Oppdragstidslinje(), metrikker)
                        "ugyldig_oppdrag" -> Oppdrag.UgyldigOppdrag(IllegalArgumentException("Ugyldige testdata"), metrikker)
                        else -> Oppdrag.UgyldigKombinasjon(IllegalArgumentException("Ugyldige testdata"), metrikker)
                    }

                Metrics.registrerOppdrag(oppdrag)

                antallFelt() shouldBe 1.0
                antall(metrikk, type) shouldBe 1.0
                Metrics.prometheusMeterRegistry.meters
                    .single()
                    .id.tags
                    .associate { it.key to it.value } shouldBe
                    (metrikk.tags + ("oppdragstype" to type))
            }
        }

        test("registrerer vilkårlige feltnavn og tagger fra oppdraget") {
            val metrikker =
                listOf(
                    metrikk(felt = "et_felt"),
                    metrikk(felt = "et_annet_felt", tags = mapOf("annen_tag" to "annen_verdi")),
                )

            Metrics.registrerOppdrag(Oppdrag.Utbetaling(Oppdragstidslinje(), metrikker))

            Metrics.prometheusMeterRegistry.meters.size shouldBe 2
            metrikker.forEach { antall(it) shouldBe 1.0 }
            metrikker.forEach { metrikk ->
                Metrics.prometheusMeterRegistry
                    .get("beregningsplattform_mottak_${metrikk.felt}")
                    .counter()
                    .id.tags
                    .associate { it.key to it.value } shouldBe (metrikk.tags + ("oppdragstype" to "utbetaling"))
            }
        }

        test("teller hver forekomst av samme metrikk") {
            val metrikk = metrikk()

            Metrics.registrerOppdrag(Oppdrag.Utbetaling(Oppdragstidslinje(), listOf(metrikk, metrikk)))

            antallFelt() shouldBe 2.0
            antall(metrikk) shouldBe 2.0
        }

        test("skiller tellere med samme felt og ulike taggverdier") {
            val metrikker =
                listOf(
                    metrikk(),
                    metrikk(tags = mapOf("testtag" to "annen_verdi")),
                )

            Metrics.registrerOppdrag(Oppdrag.Utbetaling(Oppdragstidslinje(), metrikker))

            Metrics.prometheusMeterRegistry.meters.size shouldBe 2
            metrikker.forEach { antall(it) shouldBe 1.0 }
        }

        test("teller på nytt ved gjentatt behandling av samme oppdrag") {
            val metrikk = metrikk()
            val oppdrag = Oppdrag.Utbetaling(Oppdragstidslinje(), listOf(metrikk, metrikk))

            repeat(2) { Metrics.registrerOppdrag(oppdrag) }

            antallFelt() shouldBe 4.0
            antall(metrikk) shouldBe 4.0
        }

        test("skiller tellere etter oppdragstype") {
            val metrikk = metrikk()

            Metrics.registrerOppdrag(Oppdrag.Utbetaling(Oppdragstidslinje(), listOf(metrikk)))
            Metrics.registrerOppdrag(Oppdrag.Simulering(Oppdragstidslinje(), listOf(metrikk)))

            antallFelt() shouldBe 2.0
            antall(metrikk, "utbetaling") shouldBe 1.0
            antall(metrikk, "simulering") shouldBe 1.0
        }

        test("registrerer ingen tellere for et oppdrag uten metrikker") {
            Metrics.registrerOppdrag(Oppdrag.Utbetaling(Oppdragstidslinje(), emptyList()))

            antallFelt() shouldBe 0.0
        }
    })

private fun metrikk(
    felt: String = "testfelt",
    tags: Map<String, String> = mapOf("testtag" to "testverdi"),
) = Oppdrag.Metrikk(felt, tags)

private fun resettCounters() {
    Metrics.prometheusMeterRegistry.meters.forEach { Metrics.prometheusMeterRegistry.remove(it) }
}

private fun antallFelt() =
    Metrics.prometheusMeterRegistry.meters.sumOf {
        Metrics.prometheusMeterRegistry
            .get(it.id.name)
            .tags(it.id.tags)
            .counter()
            .count()
    }

private fun antall(
    metrikk: Oppdrag.Metrikk,
    type: String = "utbetaling",
) = Metrics.prometheusMeterRegistry
    .get("beregningsplattform_mottak_${metrikk.felt}")
    .tags((metrikk.tags + ("oppdragstype" to type)).map { (navn, verdi) -> Tag.of(navn, verdi) })
    .counter()
    .count()
