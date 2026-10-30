package no.nav.utbetaling.beregning.lab.dto

import java.time.LocalDate
import java.util.UUID

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class OppdragstidslinjeDTOTest :
    FunSpec({
        test("Plus legger nyeste kjedeversjon først ved lik kjedeId") {
            val kjedeId = UUID.randomUUID()
            val thisDto =
                OppdragstidslinjeDTO(
                    kjeder =
                        listOf(
                            kjede(
                                id = kjedeId,
                                versjoner = listOf(kjedeVersjon("gammel")),
                            ),
                        ),
                )
            val otherDto =
                OppdragstidslinjeDTO(
                    kjeder =
                        listOf(
                            kjede(
                                id = kjedeId,
                                versjoner = listOf(kjedeVersjon("ny")),
                            ),
                        ),
                )

            val resultat = thisDto + otherDto

            resultat.kjeder
                .single()
                .kjedeVersjoner
                .map {
                    it.kjedePerioder
                        .single()
                        .status.opplysning
                }.shouldContainExactly("ny", "gammel")
        }

        test("Plus beholder kjeder som bare finnes i den ene siden") {
            val venstreId = UUID.randomUUID()
            val høyreId = UUID.randomUUID()
            val thisDto =
                OppdragstidslinjeDTO(
                    kjeder = listOf(kjede(id = venstreId, versjoner = listOf(kjedeVersjon("venstre")))),
                )
            val otherDto =
                OppdragstidslinjeDTO(
                    kjeder = listOf(kjede(id = høyreId, versjoner = listOf(kjedeVersjon("høyre")))),
                )

            val resultat = thisDto + otherDto

            resultat.kjeder.map { it.id }.toSet() shouldBe setOf(venstreId, høyreId)
        }
    })

private fun kjede(
    id: UUID,
    versjoner: List<KjedeVersjonDTO>,
) = KjedeDTO(
    id = id,
    fagområde = "AAP",
    fagsystemId = "fagsystem-1",
    kjedeVersjoner = versjoner,
)

private fun kjedeVersjon(status: String) =
    KjedeVersjonDTO(
        innsending = 1,
        kjedePerioder =
            listOf(
                KjedePerioderDTO(
                    fraDato = LocalDate.parse("2026-01-01"),
                    tilDato = null,
                    delytelseId = "delytelse-1",
                    status = OpplysningDTO("Z123456", status),
                    sats = OpplysningDTO("Z123456", "100"),
                    avventingsårsak = null,
                    forfall = null,
                    utbetalesTil = null,
                    gjelderId = null,
                    henvisning = null,
                    stønadId = null,
                    utbetalingsfrekvens = null,
                    bilagstype = null,
                    kidnummer = null,
                    bostedsenhet = null,
                    behandlendeEnhet = null,
                    vedtakId = null,
                    brukKjøreplan = OpplysningDTO("Z123456", "Ja"),
                    ompostering = null,
                    ytelseKlassekode = OpplysningDTO("Z123456", "SPREF"),
                    skyldner = null,
                    refusjonsinfo = null,
                    typeSøknad = null,
                ),
            ),
    )
