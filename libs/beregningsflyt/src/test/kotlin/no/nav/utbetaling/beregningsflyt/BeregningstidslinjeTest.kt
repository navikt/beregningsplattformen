package no.nav.utbetaling.beregningsflyt

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

import no.nav.utbetaling.beregning.oppdrag.Opplysninger
import no.nav.utbetaling.beregning.tid.Tidslinje
import no.nav.utbetaling.beregning.tid.desember
import no.nav.utbetaling.beregning.tid.februar
import no.nav.utbetaling.beregning.tid.januar
import no.nav.utbetaling.beregning.tid.november
import no.nav.utbetaling.beregning.tid.til
import no.nav.utbetaling.beregningsflyt.testhjelpere.opplysninger

class BeregningstidslinjeTest :
    FunSpec({

        test("beregningstidslinje som spenner over mange år") {
            val periode = 31.desember(2018) til 31.desember(2026)
            val opplysninger = opplysninger()
            val tidslinje =
                Tidslinje
                    .Builder<Map<Oppdragsidentifikator, List<Opplysninger>>>()
                    .periode(periode, mapOf(Oppdragsidentifikator(opplysninger.fagområde, opplysninger.fagsystemId) to listOf(opplysninger)))
                    .build()

            val beregningstidslinje =
                beregningstidslinje(
                    beregningsperiode = periode,
                    tidslinje = tidslinje,
                    beregningstidspunkt = 2.januar(2026).atStartOfDay(),
                    trekk = emptyList(),
                    relaterteBeregninger = emptyList(),
                )

            tidslinje.single().first shouldBe periode
            beregningstidslinje.map { it.first } shouldBe
                listOf(
                    31.desember(2018) til 31.desember(2018),
                    1.januar(2019) til 31.desember(2019),
                    1.januar(2020) til 31.desember(2020),
                    1.januar(2021) til 31.desember(2021),
                    1.januar(2022) til 31.desember(2022),
                    1.januar(2023) til 31.desember(2023),
                    1.januar(2024) til 31.desember(2024),
                    1.januar(2025) til 30.november(2025),
                    1.desember(2025) til 31.desember(2025), // Splittes i egen måned på grunn av skattekortoppdeling
                    1.januar(2026) til 31.januar(2026), // Splittes i egen måned på grunn av skattekortoppdeling
                    1.februar(2026) til 31.desember(2026),
                )
        }
    })
