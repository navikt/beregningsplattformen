package no.nav.utbetaling.beregning.oppdragsmottak.metrics

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.Tag
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

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
                val forekomst = forekomst()
                val metrikker = listOf(Oppdrag.Metrikk("testfelt", forekomster = listOf(forekomst)))
                val oppdrag =
                    when (type) {
                        "utbetaling" -> Oppdrag.Utbetaling(Oppdragstidslinje(), metrikker)
                        "simulering" -> Oppdrag.Simulering(Oppdragstidslinje(), metrikker)
                        "ugyldig_oppdrag" -> Oppdrag.UgyldigOppdrag(IllegalArgumentException("Ugyldige testdata"), metrikker)
                        else -> Oppdrag.UgyldigKombinasjon(IllegalArgumentException("Ugyldige testdata"), metrikker)
                    }

                Metrics.registrerOppdrag(oppdrag)

                antallFelt() shouldBe 1.0
                antall(forekomst, type) shouldBe 1.0
                Metrics.prometheusMeterRegistryMottak.relevanteMeters
                    .single()
                    .id.tags
                    .associate { it.key to it.value } shouldBe
                    (forekomst.tagsUtenFelt + ("oppdragstype" to type))
            }
        }

        test("registrerer vilkårlige feltnavn og tagger fra oppdraget") {
            val forekomster =
                listOf(
                    forekomst(felt = "et_felt"),
                    forekomst(felt = "et_annet_felt", tags = mapOf("annen_tag" to "annen_verdi")),
                )

            Metrics.registrerOppdrag(Oppdrag.Utbetaling(Oppdragstidslinje(), listOf(Oppdrag.Metrikk("testmetrikk", forekomster = forekomster))))

            Metrics.prometheusMeterRegistryMottak.relevanteMeters.size shouldBe 2
            forekomster.forEach { antall(it) shouldBe 1.0 }
            forekomster.forEach { metrikk ->
                Metrics.prometheusMeterRegistryMottak
                    .get("${MOTTAK_METRICS_NAMESPACE}_${metrikk.felt}")
                    .counter()
                    .id.tags
                    .associate { it.key to it.value } shouldBe (metrikk.tagsUtenFelt + ("oppdragstype" to "utbetaling"))
            }
        }

        test("teller hver forekomst av samme metrikk") {
            val forekomst = forekomst()

            Metrics.registrerOppdrag(Oppdrag.Utbetaling(Oppdragstidslinje(), listOf(Oppdrag.Metrikk("testmetrikk", forekomster = listOf(forekomst, forekomst)))))

            antallFelt() shouldBe 2.0
            antall(forekomst) shouldBe 2.0
        }

        test("skiller tellere med samme felt og ulike taggverdier") {
            val forekomster =
                listOf(
                    forekomst(),
                    forekomst(tags = mapOf("testtag" to "annen_verdi")),
                )

            Metrics.registrerOppdrag(Oppdrag.Utbetaling(Oppdragstidslinje(), listOf(Oppdrag.Metrikk("testmetrikk", forekomster = forekomster))))

            Metrics.prometheusMeterRegistryMottak.relevanteMeters.size shouldBe 2
            forekomster.forEach { antall(it) shouldBe 1.0 }
        }

        test("teller på nytt ved gjentatt behandling av samme oppdrag") {
            val forekomst = forekomst()
            val oppdrag = Oppdrag.Utbetaling(Oppdragstidslinje(), listOf(Oppdrag.Metrikk("testmetrikk", forekomster = listOf(forekomst, forekomst))))

            repeat(2) { Metrics.registrerOppdrag(oppdrag) }

            antallFelt() shouldBe 4.0
            antall(forekomst) shouldBe 4.0
        }

        test("skiller tellere etter oppdragstype") {
            val forekomst = forekomst()

            Metrics.registrerOppdrag(Oppdrag.Utbetaling(Oppdragstidslinje(), listOf(Oppdrag.Metrikk("testmetrikk", forekomster = listOf(forekomst)))))
            Metrics.registrerOppdrag(Oppdrag.Simulering(Oppdragstidslinje(), listOf(Oppdrag.Metrikk("testmetrikk", forekomster = listOf(forekomst)))))

            antallFelt() shouldBe 2.0
            antall(forekomst, "utbetaling") shouldBe 1.0
            antall(forekomst, "simulering") shouldBe 1.0
        }

        test("registrerer ingen tellere for et oppdrag uten metrikker") {
            Metrics.registrerOppdrag(Oppdrag.Utbetaling(Oppdragstidslinje(), emptyList()))

            antallFelt() shouldBe 0.0
        }
    })

private fun forekomst(
    felt: String = "testfelt",
    tags: Map<String, String> = mapOf("testtag" to "testverdi"),
) = Oppdrag.Metrikk.Forekomst(tags + ("felt" to felt))

private val Oppdrag.Metrikk.Forekomst.felt get() = tags.getValue("felt")
private val Oppdrag.Metrikk.Forekomst.tagsUtenFelt get() = tags.minus("felt")
private val PrometheusMeterRegistry.relevanteMeters get() = meters.filter { it.id.name.startsWith("beregningsplattformen") }

private fun resettCounters() {
    Metrics.prometheusMeterRegistryMottak.relevanteMeters.forEach { Metrics.prometheusMeterRegistryMottak.remove(it) }
}

private fun antallFelt() =
    Metrics.prometheusMeterRegistryMottak.relevanteMeters.sumOf {
        Metrics.prometheusMeterRegistryMottak
            .get(it.id.name)
            .tags(it.id.tags)
            .counter()
            .count()
    }

private fun antall(
    forekomst: Oppdrag.Metrikk.Forekomst,
    type: String = "utbetaling",
) = Metrics.prometheusMeterRegistryMottak
    .get("${MOTTAK_METRICS_NAMESPACE}_${forekomst.felt}")
    .tags((forekomst.tagsUtenFelt + ("oppdragstype" to type)).map { (navn, verdi) -> Tag.of(navn, verdi) })
    .counter()
    .count()
