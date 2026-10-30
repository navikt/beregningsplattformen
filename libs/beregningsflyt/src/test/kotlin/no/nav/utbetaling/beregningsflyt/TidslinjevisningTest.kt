package no.nav.utbetaling.beregningsflyt

import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.utbetaling.beregning.oppdrag.DelytelseId
import no.nav.utbetaling.beregning.oppdrag.Part
import no.nav.utbetaling.beregning.oppdrag.Personidentifikator
import no.nav.utbetaling.beregning.oppdrag.Refusjonsinfo
import no.nav.utbetaling.beregning.oppdrag.TypeSøknad
import no.nav.utbetaling.beregning.tid.DefaultTidslinjesamling
import no.nav.utbetaling.beregning.tid.Periode.LukketPeriode
import no.nav.utbetaling.beregning.tid.Periode.ÅpenPeriode
import no.nav.utbetaling.beregningsflyt.kontekst.avgrensTil

class TidslinjevisningTest :
    FunSpec({
        val førsteKjede = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val andreKjede = UUID.fromString("00000000-0000-0000-0000-000000000002")
        val sats = BigDecimal("100.00")

        test("closed view clips both bounds inclusively and preserves source information") {
            val kilde = tidslinjeForTest(førsteKjede, lukket(1, 31) to sats)

            val visning = kilde.avgrensTil(DatoIntervall(dag(5), dag(20)))

            visning.tidslinjer.keys shouldBe setOf(førsteKjede)
            visning.tidslinjer.getValue(førsteKjede).map { it.first } shouldBe listOf(lukket(5, 20))
            visning[dag(5)].getValue(førsteKjede) shouldBeSameInstanceAs kilde[dag(5)].getValue(førsteKjede)
            visning[dag(20)].getValue(førsteKjede).sats.opplysning.verdi shouldBe sats
            visning[dag(4)] shouldBe emptyMap()
            visning[dag(21)] shouldBe emptyMap()
        }

        test("clipping parsed rates preserves dated debtor refund and application fields") {
            val kilde =
                lesTidslinje(
                    """{"kodeAksjon":"1","kodeEndring":"NY","kodeFagomraade":"AAP","fagsystemId":"fagsystemId",
                    "oppdragGjelderId":"11111111111","datoOppdragGjelderFom":"2000-01-01","saksbehId":"Z999999",
                    "oppdragsLinje150":[{"kodeEndringLinje":"NY","delytelseId":"1","kodeKlassifik":"AAPUAA",
                    "datoVedtakFom":"2026-01-01","datoVedtakTom":"2026-01-31","sats":100,
                    "vedtakssats157":{"vedtakssats":120},"fradragTillegg":"T","typeSats":"DAG","saksbehId":"LINJE1",
                    "skyldnerId":"12345678901","datoSkyldnerFom":"2026-01-10","typeSoknad":"NY",
                    "refusjonsinfo156":{"refunderesId":"12345678902","maksDato":"2026-01-31","datoFom":"2026-01-15"}}]}""",
                )
            val kjede = kilde.tidslinjer.keys.single()
            val visning = kilde.avgrensTil(DatoIntervall(dag(5), dag(20)))
            visning.tidslinjer.getValue(kjede).map { it.first } shouldBe listOf(lukket(5, 9), lukket(10, 14), lukket(15, 20))
            visning[dag(5)].getValue(kjede).skyldner shouldBe null
            visning[dag(14)].getValue(kjede).refusjonsinfo shouldBe null
            val opplysninger = visning[dag(20)].getValue(kjede)
            opplysninger shouldBeSameInstanceAs kilde[dag(20)].getValue(kjede)
            opplysninger.sats.opplysning.verdi shouldBe BigDecimal("100")
            opplysninger.sats.opplysning.satsType shouldBe "DAG"
            opplysninger.vedtaksSats?.opplysning?.verdi shouldBe BigDecimal("120")
            opplysninger.skyldner?.opplysning shouldBe Part.Person(Personidentifikator("12345678901"))
            opplysninger.refusjonsinfo?.opplysning shouldBe Refusjonsinfo(dag(31), Part.Person(Personidentifikator("12345678902")), dag(15))
            opplysninger.typeSøknad?.opplysning shouldBe TypeSøknad("NY")
            opplysninger.skyldner?.saksbehandlerId shouldBe opplysninger.sats.saksbehandlerId
            opplysninger.refusjonsinfo?.saksbehandlerId shouldBe opplysninger.sats.saksbehandlerId
            opplysninger.typeSøknad?.saksbehandlerId shouldBe opplysninger.sats.saksbehandlerId
        }

        test("closed view closes an open source end") {
            val kilde = tidslinjeForTest(førsteKjede, ÅpenPeriode(dag(1)) to sats)

            val visning = kilde.avgrensTil(DatoIntervall(dag(5), dag(20)))

            visning.tidslinjer.getValue(førsteKjede).map { it.first } shouldBe listOf(lukket(5, 20))
            visning[dag(21)] shouldBe emptyMap()
        }

        test("open view retains an open source end without a sentinel date") {
            val kilde = tidslinjeForTest(førsteKjede, ÅpenPeriode(dag(1)) to sats)

            val visning = kilde.avgrensTil(DatoIntervall(dag(5)))

            visning.tidslinjer.getValue(førsteKjede).map { it.first } shouldBe listOf(ÅpenPeriode(dag(5)))
            visning[LocalDate.MAX].getValue(førsteKjede).sats.opplysning.verdi shouldBe sats
        }

        test("open view does not extend a closed source") {
            val kilde = tidslinjeForTest(førsteKjede, lukket(1, 20) to sats)

            val visning = kilde.avgrensTil(DatoIntervall(dag(5)))

            visning.tidslinjer.getValue(førsteKjede).map { it.first } shouldBe listOf(lukket(5, 20))
            visning[dag(21)] shouldBe emptyMap()
        }

        test("independent chains remain distinct on overlapping dates even with the same delytelseId") {
            val første = tidslinjeForTest(førsteKjede, lukket(1, 20) to sats)
            val andre = tidslinjeForTest(andreKjede, lukket(10, 31) to BigDecimal("200.00"))
            val kilde = DefaultTidslinjesamling(første.tidslinjer + andre.tidslinjer)

            val visning = kilde.avgrensTil(DatoIntervall(dag(10), dag(15)))

            visning.tidslinjer.keys shouldBe setOf(førsteKjede, andreKjede)
            visning[dag(12)].size shouldBe 2
            visning[dag(12)].getValue(førsteKjede).sats.opplysning.verdi shouldBe sats
            visning[dag(12)].getValue(andreKjede).sats.opplysning.verdi shouldBe BigDecimal("200.00")
            visning.tidslinjer.values.forEach { tidslinje ->
                tidslinje.map { it.first } shouldBe listOf(lukket(10, 15))
            }
        }

        test("chains without overlap and already empty chains are omitted") {
            val første = tidslinjeForTest(førsteKjede, lukket(1, 5) to sats)
            val andre = tidslinjeForTest(andreKjede)
            val kilde = DefaultTidslinjesamling(første.tidslinjer + andre.tidslinjer)

            val visning = kilde.avgrensTil(DatoIntervall(dag(10), dag(15)))

            visning.tidslinjer shouldBe emptyMap()
            visning.isEmpty() shouldBe true
            visning.avgrensTil(DatoIntervall(dag(10))).isEmpty() shouldBe true
        }

        test("physical slices with the same delytelseId retain their own rates and decision rate") {
            val vedtakssats = BigDecimal("300.00")
            val kilde =
                tidslinjeForTest(
                    førsteKjede,
                    lukket(1, 10) to sats,
                    lukket(11, 20) to BigDecimal("200.00"),
                    delytelseId = DelytelseId("same-source-line"),
                    vedtakssats = vedtakssats,
                )

            val visning = kilde.avgrensTil(DatoIntervall(dag(5), dag(15)))
            val deler = visning.tidslinjer.getValue(førsteKjede).toList()

            deler.map { it.first } shouldBe listOf(lukket(5, 10), lukket(11, 15))
            deler.map { it.second.delytelseId } shouldBe List(2) { DelytelseId("same-source-line") }
            deler.map { it.second.sats.opplysning.verdi } shouldBe listOf(sats, BigDecimal("200.00"))
            deler.map { it.second.vedtaksSats?.opplysning?.verdi } shouldBe List(2) { vedtakssats }
            deler.forEach { (periode, opplysninger) ->
                opplysninger shouldBeSameInstanceAs kilde[periode.fom].getValue(førsteKjede)
            }
        }

        test("gaps survive clipping and the source remains unchanged") {
            val kilde = tidslinjeForTest(førsteKjede, lukket(1, 5) to sats, lukket(10, 20) to sats)
            val før = kilde.tidslinjer.getValue(førsteKjede).toList()

            val visning = kilde.avgrensTil(DatoIntervall(dag(3), dag(15)))

            visning.tidslinjer.getValue(førsteKjede).map { it.first } shouldBe listOf(lukket(3, 5), lukket(10, 15))
            (6..9).forEach { visning[dag(it)] shouldBe emptyMap() }
            kilde.tidslinjer.getValue(førsteKjede).toList() shouldBe før
            kilde[dag(1)].getValue(førsteKjede).sats.opplysning.verdi shouldBe sats
            kilde[dag(20)].getValue(førsteKjede).sats.opplysning.verdi shouldBe sats
        }
    })

private fun dag(dag: Int): LocalDate = LocalDate.of(2026, 1, dag)

private fun lukket(
    fom: Int,
    tom: Int,
): LukketPeriode = LukketPeriode(dag(fom), dag(tom))
