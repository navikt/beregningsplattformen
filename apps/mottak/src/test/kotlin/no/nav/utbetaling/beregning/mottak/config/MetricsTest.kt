package no.nav.utbetaling.beregning.mottak.config

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain

import no.nav.utbetaling.beregning.oppdrag.Oppdrag
import no.nav.utbetaling.beregning.oppdrag.Oppdragstidslinje

class MetricsTest :
    FunSpec({
        beforeEach {
            Metrics.oppdragFeltCounter.clear()
            Metrics.oppdragFeilCounter.clear()
        }

        afterEach {
            Metrics.oppdragFeltCounter.clear()
            Metrics.oppdragFeilCounter.clear()
        }

        listOf("utbetaling", "simulering").forEach { type ->
            test("registrerer felt og tagger for $type") {
                val metrikker = listOf(metrikk())
                val oppdrag =
                    when (type) {
                        "utbetaling" -> Oppdrag.Utbetaling(Oppdragstidslinje(), metrikker)
                        else -> Oppdrag.Simulering(Oppdragstidslinje(), metrikker)
                    }

                Metrics.registrerOppdrag(oppdrag)

                antallFelt() shouldBe 1.0
                Metrics.oppdragFeltCounter.labelValues(AKSJONSFELT, "1", "AAP", "AAPUAA").get() shouldBe 1.0
                antallFeil() shouldBe 0.0
            }
        }

        test("registrerer alle tillatte felt og filtrerer bort andre felt") {
            val tillatteFelt =
                listOf(
                    AKSJONSFELT,
                    "oppdrag_110_kodeEndring",
                    "oppdrag_110_fagsystemId",
                    "oppdrag_110_oppdrags_linje_150_kodeEndringLinje",
                    "oppdrag_110_oppdrags_linje_150_delytelseId",
                    "oppdrag_110_oppdrags_linje_150_refFagsystemId",
                )
            val utelattFelt = "oppdrag_110_utbetFrekvens"
            val metrikker = (tillatteFelt + utelattFelt).map { metrikk(felt = it, verdi = "skjult") }

            Metrics.registrerOppdrag(Oppdrag.Utbetaling(Oppdragstidslinje(), metrikker))

            antallFelt() shouldBe 6.0
            tillatteFelt.forEach { felt ->
                Metrics.oppdragFeltCounter.labelValues(felt, "skjult", "AAP", "AAPUAA").get() shouldBe 1.0
            }
            Metrics.prometheusMeterRegistry.scrape() shouldNotContain utelattFelt
        }

        test("teller samme kombinasjon én gang per oppdrag selv med ulike ekstra tagger") {
            val metrikk = metrikk()
            val metrikker = listOf(metrikk, metrikk, metrikk.copy(tags = metrikk.tags + ("ekstra" to "ignoreres")))

            Metrics.registrerOppdrag(Oppdrag.Utbetaling(Oppdragstidslinje(), metrikker))

            antallFelt() shouldBe 1.0
            Metrics.oppdragFeltCounter.labelValues(AKSJONSFELT, "1", "AAP", "AAPUAA").get() shouldBe 1.0
        }

        test("teller ulike kombinasjoner av felt, verdi, fagområde og klassifikasjon hver for seg") {
            val metrikker =
                listOf(
                    metrikk(),
                    metrikk(felt = "oppdrag_110_kodeEndring"),
                    metrikk(verdi = "2"),
                    metrikk(fagomraade = "DAGP"),
                    metrikk(klassifik = "AAPOR"),
                )

            Metrics.registrerOppdrag(Oppdrag.Utbetaling(Oppdragstidslinje(), metrikker))

            antallFelt() shouldBe 5.0
            metrikker.forEach { metrikk ->
                Metrics.oppdragFeltCounter
                    .labelValues(metrikk.felt, metrikk.tags["verdi"], metrikk.tags["kodeFagomraade"], metrikk.tags["kodeKlassifik"])
                    .get() shouldBe 1.0
            }
        }

        test("teller på nytt ved gjentatt behandling av samme oppdrag") {
            val oppdrag = Oppdrag.Utbetaling(Oppdragstidslinje(), listOf(metrikk(), metrikk()))

            repeat(2) { Metrics.registrerOppdrag(oppdrag) }

            antallFelt() shouldBe 2.0
            Metrics.oppdragFeltCounter.labelValues(AKSJONSFELT, "1", "AAP", "AAPUAA").get() shouldBe 2.0
        }

        listOf("verdi", "kodeFagomraade", "kodeKlassifik").forEach { tag ->
            test("håndterer manglende og blank $tag som ukjent") {
                val metrikk = metrikk()
                val metrikker =
                    listOf(
                        metrikk.copy(tags = metrikk.tags - tag),
                        metrikk.copy(tags = metrikk.tags + (tag to "")),
                        metrikk.copy(tags = metrikk.tags + (tag to " \t ")),
                    )

                Metrics.registrerOppdrag(Oppdrag.Utbetaling(Oppdragstidslinje(), metrikker))

                antallFelt() shouldBe 1.0
                Metrics.oppdragFeltCounter
                    .labelValues(
                        AKSJONSFELT,
                        if (tag == "verdi") "ukjent" else "1",
                        if (tag == "kodeFagomraade") "ukjent" else "AAP",
                        if (tag == "kodeKlassifik") "ukjent" else "AAPUAA",
                    ).get() shouldBe 1.0
            }
        }

        test("registrerer ingen tellere for et gyldig oppdrag uten metrikker") {
            Metrics.registrerOppdrag(Oppdrag.Utbetaling(Oppdragstidslinje(), emptyList()))

            antallFelt() shouldBe 0.0
            antallFeil() shouldBe 0.0
        }

        listOf("ugyldig_oppdrag", "ugyldig_kombinasjon").forEach { type ->
            test("teller $type uten å registrere felt fra det ugyldige oppdraget") {
                val feil = IllegalArgumentException("Ugyldige testdata")
                val metrikker = listOf(metrikk())
                val oppdrag =
                    when (type) {
                        "ugyldig_oppdrag" -> Oppdrag.UgyldigOppdrag(feil, metrikker)
                        else -> Oppdrag.UgyldigKombinasjon(feil, metrikker)
                    }

                repeat(2) { Metrics.registrerOppdrag(oppdrag) }

                antallFeil() shouldBe 2.0
                Metrics.oppdragFeilCounter.labelValues(type).get() shouldBe 2.0
                antallFelt() shouldBe 0.0
            }
        }
    })

private const val AKSJONSFELT = "oppdrag_110_kodeAksjon"

private fun metrikk(
    felt: String = AKSJONSFELT,
    verdi: String = "1",
    fagomraade: String = "AAP",
    klassifik: String = "AAPUAA",
) = Oppdrag.Metrikk(
    felt,
    mapOf("verdi" to verdi, "kodeFagomraade" to fagomraade, "kodeKlassifik" to klassifik),
)

private fun antallFelt() = antall("beregningsplattform_mottak_oppdrag_felt_total")

private fun antallFeil() = antall("beregningsplattform_mottak_oppdrag_feil_total")

private fun antall(navn: String) =
    Metrics.prometheusMeterRegistry
        .scrape()
        .lineSequence()
        .filter { it.startsWith("$navn{") }
        .sumOf { it.substringAfterLast(' ').toDouble() }
