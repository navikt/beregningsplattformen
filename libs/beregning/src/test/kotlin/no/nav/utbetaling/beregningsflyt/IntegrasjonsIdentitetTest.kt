package no.nav.utbetaling.beregningsflyt

import java.util.UUID

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

import no.nav.sokos.dare.poc.aktoer.AktoerId
import no.nav.sokos.dare.poc.oppdrag.OppdragId
import no.nav.sokos.dare.poc.oppdrag.OppdragslinjekjedeId
import no.nav.utbetaling.beregning.oppdrag.Fagområde
import no.nav.utbetaling.beregning.oppdrag.FagsystemId
import no.nav.utbetaling.beregning.oppdrag.Oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.Organisasjonsnummer
import no.nav.utbetaling.beregning.oppdrag.Personidentifikator
import no.nav.utbetaling.beregning.oppdrag.SamhandlerId
import no.nav.utbetaling.beregning.oppdrag.UtbetalesTil
import no.nav.utbetaling.beregningsflyt.identitet.Oppdragsnokkel

class IntegrasjonsIdentitetTest :
    FunSpec({
        test("fixture assignment and actor associations never interpret the source person as a numeric actor ID") {
            listOf("11111111111", "synthetic-actor").forEach { person ->
                val identitet = IntegrasjonsIdentitet(identitetskilde(person))
                identitet.oppdragId(Oppdragsnokkel(Fagområde("AAP"), FagsystemId("fixture-assignment"))) shouldBe OppdragId(0)
                identitet.gjelderId(Personidentifikator(person)) shouldBe AktoerId(1)
                identitet.mottakerId(UtbetalesTil.Person(Personidentifikator(person))) shouldBe AktoerId(1)
            }
        }

        test("distinct typed recipient aliases remain deterministic and the actor recipient equals the actor") {
            val kilde = identitetskilde("11111111111", listOf("11111111111", "22222222222", "999999999", "80000000000", "22222222222"))
            val mottakere =
                listOf(
                    UtbetalesTil.Person(Personidentifikator("11111111111")),
                    UtbetalesTil.Person(Personidentifikator("22222222222")),
                    UtbetalesTil.Organisasjon(Organisasjonsnummer("999999999")),
                    UtbetalesTil.Samhandler(SamhandlerId("80000000000")),
                )
            repeat(2) {
                val identitet = IntegrasjonsIdentitet(kilde)
                mottakere.map(identitet::mottakerId) shouldBe (1L..4L).map(::AktoerId)
                identitet.mottakerId(mottakere[0]) shouldBe identitet.gjelderId(Personidentifikator("11111111111"))
                identitet.mottakerId(mottakere[1]) shouldBe AktoerId(2)
            }
        }

        test("result chain IDs are allocated only on emission and restart for each run independently of random UUIDs") {
            repeat(2) {
                val kilde = identitetskilde("synthetic-actor", listOf(null, null))
                val identitet = IntegrasjonsIdentitet(kilde)
                val kjeder = kilde.tidslinjer.keys.toList()
                identitet.resultatKjedeId(kjeder[1]) shouldBe OppdragslinjekjedeId(0)
                identitet.resultatKjedeId(kjeder[1]) shouldBe OppdragslinjekjedeId(0)
                identitet.resultatKjedeId(kjeder[0]) shouldBe OppdragslinjekjedeId(1)
                IntegrasjonsIdentitet(kilde).resultatKjedeId(kjeder[0]) shouldBe OppdragslinjekjedeId(0)
            }
        }

        test("unknown assignment actor recipient and chain fail rather than receiving default identities") {
            val identitet = IntegrasjonsIdentitet(identitetskilde("synthetic-actor"))
            shouldThrow<NoSuchElementException> { identitet.oppdragId(Oppdragsnokkel(Fagområde("AAP"), FagsystemId("unknown"))) }
            shouldThrow<NoSuchElementException> { identitet.gjelderId(Personidentifikator("unknown")) }
            shouldThrow<NoSuchElementException> { identitet.mottakerId(UtbetalesTil.Person(Personidentifikator("unknown"))) }
            shouldThrow<IllegalArgumentException> { identitet.resultatKjedeId(UUID.randomUUID()) }
        }
    })

private fun identitetskilde(
    person: String,
    mottakere: List<String?> = listOf(null),
): Oppdragstidslinje =
    lesTidslinje(
        """{"kodeAksjon":"1","kodeEndring":"NY","kodeFagomraade":"AAP","fagsystemId":"fixture-assignment",
        "oppdragGjelderId":"$person","datoOppdragGjelderFom":"2000-01-01","saksbehId":"Z999999",
        "oppdragsLinje150":[${
            mottakere.mapIndexed { i, mottaker ->
                """{"kodeEndringLinje":"NY","delytelseId":"$i","kodeKlassifik":"AAPUAA",
                "datoVedtakFom":"2026-03-01","datoVedtakTom":"2026-03-31","sats":100,
                "fradragTillegg":"T","typeSats":"DAG","saksbehId":"Z999999"
                ${mottaker?.let { ""","utbetalesTilId":"$it"""" }.orEmpty()}}"""
            }.joinToString(",")
        }]}""",
    )
