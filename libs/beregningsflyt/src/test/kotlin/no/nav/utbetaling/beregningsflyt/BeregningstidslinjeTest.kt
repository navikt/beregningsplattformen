package no.nav.utbetaling.beregningsflyt

import java.math.BigDecimal

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

import no.nav.utbetaling.beregning.oppdrag.BrukKjøreplan
import no.nav.utbetaling.beregning.oppdrag.DelytelseId
import no.nav.utbetaling.beregning.oppdrag.Fagområde
import no.nav.utbetaling.beregning.oppdrag.FagsystemId
import no.nav.utbetaling.beregning.oppdrag.Opplysning
import no.nav.utbetaling.beregning.oppdrag.Opplysninger
import no.nav.utbetaling.beregning.oppdrag.Personidentifikator
import no.nav.utbetaling.beregning.oppdrag.SaksbehandlerId
import no.nav.utbetaling.beregning.oppdrag.Sats
import no.nav.utbetaling.beregning.oppdrag.Status
import no.nav.utbetaling.beregning.oppdrag.YtelseKlassekode
import no.nav.utbetaling.beregning.tid.Tidslinje
import no.nav.utbetaling.beregning.tid.desember
import no.nav.utbetaling.beregning.tid.februar
import no.nav.utbetaling.beregning.tid.januar
import no.nav.utbetaling.beregning.tid.november
import no.nav.utbetaling.beregning.tid.til

class BeregningstidslinjeTest :
    FunSpec({

        fun lagOpplysninger() =
            Opplysninger(
                fagområde = Fagområde("TEST"),
                fagsystemId = FagsystemId("fagsystem-1"),
                delytelseId = DelytelseId("delytelse-1"),
                refDelytelseId = null,
                gjelder = Opplysning(SaksbehandlerId("Z999999"), Personidentifikator("12345678910")),
                utbetalesTil = null,
                sats = Opplysning(SaksbehandlerId("Z999999"), Sats(BigDecimal("1000"), "MND")),
                vedtakssats = null,
                skyldner = null,
                refunderesTil = null,
                maksdato = null,
                søknadstype = null,
                henvisning = null,
                stønadId = null,
                forfall = null,
                utbetalingsfrekvens = null,
                bilagstype = null,
                avventingsårsak = null,
                kidnummer = null,
                status = Opplysning(SaksbehandlerId("Z999999"), Status.Aktiv),
                bostedsenhet = null,
                behandlendeEnhet = null,
                vedtakId = null,
                brukKjøreplan = Opplysning(SaksbehandlerId("Z999999"), BrukKjøreplan.Ja),
                ompostering = null,
                ytelseKlassekode = Opplysning(SaksbehandlerId("Z999999"), YtelseKlassekode("KLASSE")),
            )

        test("beregningstidslinje som spenner over mange år") {
            val periode = 31.desember(2018) til 31.desember(2026)
            val tidslinje = Tidslinje.Builder<Opplysninger>().periode(periode, lagOpplysninger()).build()

            val beregningstidslinje =
                beregningstidslinje(
                    beregningsperiode = periode,
                    tidslinje = tidslinje,
                    beregningstidspunkt = 2.januar(2026).atStartOfDay(),
                    emptyList(), emptyList()
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
