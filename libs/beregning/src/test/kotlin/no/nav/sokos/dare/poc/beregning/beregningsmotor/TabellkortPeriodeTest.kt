package no.nav.sokos.dare.poc.beregning.beregningsmotor

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

import no.nav.sokos.dare.poc.TestUtil.oppdragFraFil
import no.nav.sokos.dare.poc.aktoer.AktoerId
import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.beregning.DelberegningType
import no.nav.sokos.dare.poc.beregning.LinjeKlassekode
import no.nav.sokos.dare.poc.beregning.dataklasser.BeregningsStatus
import no.nav.sokos.dare.poc.beregning.fordeler.BeregningsFordeler
import no.nav.sokos.dare.poc.beregning.fordeler.BeregningsScratchpad
import no.nav.sokos.dare.poc.beregning.fordeler.OppdragDelperiode
import no.nav.sokos.dare.poc.beregning.fordeler.SkatteKontekst
import no.nav.sokos.dare.poc.oppdrag.Oppdrag
import no.nav.sokos.dare.poc.oppdrag.OppdragId
import no.nav.sokos.dare.poc.oppdrag.Oppdragslinje
import no.nav.sokos.dare.poc.oppdrag.TypeSats
import no.nav.sokos.dare.poc.oppdrag.YtelseKlasseKode
import no.nav.sokos.dare.poc.oppdrag.YtelseKlasseKodeRef
import no.nav.sokos.dare.poc.skattekort.domain.Forskuddstrekk
import no.nav.sokos.dare.poc.skattekort.domain.ForskuddstrekkKode
import no.nav.sokos.dare.poc.skattekort.domain.ForskuddstrekkType
import no.nav.sokos.dare.poc.skattekort.domain.Skattekort
import no.nav.sokos.dare.poc.testFordelingInput
import no.nav.sokos.dare.poc.trekk.Faggruppe
import no.nav.sokos.dare.poc.trekk.FagomraadeKode

class TabellkortPeriodeTest :
    FunSpec({
        test("verdiberegnet should derive day count and part info from tabellkort periode") {
            val periode = tabellkortPeriode(FagomraadeKode.AAP)

            periode.tabellkortPeriodetype() shouldBe FagomraadeKode.AAP.tabellkortPeriodetype
            periode.tilSkattTabellDagerPerioder() shouldHaveSize 1

            val (dager, ekstraInfo) = listOf(periode).beregnTabellkortDager()

            dager shouldBe 3
            ekstraInfo shouldHaveSize 1
        }

        test("kalenderberegnet should derive day count from tabellkort periode") {
            val periode = tabellkortPeriode(FagomraadeKode.SP)

            periode.tabellkortPeriodetype() shouldBe FagomraadeKode.SP.tabellkortPeriodetype
            periode.tilSkattTabellDagerPerioder() shouldHaveSize 1

            val (dager, ekstraInfo) = listOf(periode).beregnTabellkortDager()

            dager shouldBe 2
            ekstraInfo shouldHaveSize 0
        }

        test("should preserve verdiberegnet part info when mixing tabellkort period types") {
            val verdiberegnetPeriode = tabellkortPeriode(FagomraadeKode.AAP)
            val kalenderberegnetPeriode = tabellkortPeriode(FagomraadeKode.SP)

            val (dager, ekstraInfo) = listOf(verdiberegnetPeriode, kalenderberegnetPeriode).beregnTabellkortDager()

            dager shouldBe 5
            ekstraInfo shouldHaveSize 1
        }

        test("skal gi hovedarbeidsgivers tabellskatt standard klassekode og reskontrokonto") {
            val periode = DatoIntervall("2026-03-02", "2026-03-06")
            val skattekort =
                Skattekort(
                    aktoerId = AktoerId(1),
                    inntektsaar = 2026,
                    kilde = "TEST",
                    identifikator = 1,
                    utstedtDato = LocalDate.parse("2026-01-01"),
                    forskuddsTrekk =
                        listOf(
                            Forskuddstrekk(
                                typeForskuddsTrekk = ForskuddstrekkType.TABELLKORT,
                                trekkKode = ForskuddstrekkKode.LOENN_FRA_NAV,
                                trekktabell = "8010",
                                prosentsats = BigDecimal("30"),
                            ),
                        ),
                )
            val fordelingsmotor = Beregningsmotor(Ytelsesmotor(), Trekkmotor(), Skattemotor())
            val kontekst =
                BeregningsFordeler(fordelingsmotor).byggBeregningsmotorKontekst(
                    periode = periode,
                    oppdrag = listOf(oppdragFraFil("testdata/SkattemotorTest/simple.xml").copy(id = OppdragId(1))),
                    trekk = emptyList(),
                    input = testFordelingInput(),
                    skatteKontekst =
                        SkatteKontekst(
                            skattekort = skattekort,
                            allokeringer = mapOf(ForskuddstrekkKode.LOENN_FRA_NAV to Faggruppe.ARBYT),
                        ),
                    relaterteBeregninger = emptyList(),
                    overføringsdato = LocalDate.parse("2026-10-19"),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )

            val resultat = Skattemotor().beregnSkatteTrekk(Ytelsesmotor().beregnYtelse(kontekst))
            val skattelinjer =
                resultat.segmenter
                    .flatMap { it.scratchpad.values }
                    .flatMap { it.beregninger.values }
                    .flatten()
                    .filter { it.type == DelberegningType.SKATT }

            skattelinjer.isNotEmpty() shouldBe true
            skattelinjer.forEach { skattelinje ->
                skattelinje.satsType shouldBe no.nav.sokos.dare.poc.beregning.dataklasser.Satstype.TABELL
                skattelinje.kodeKlasse shouldBe LinjeKlassekode.Skatt("FSKTSKAT")
                skattelinje.reskontroKonto shouldBe "0510000"
            }
        }
    })

private fun tabellkortPeriode(fagomraade: FagomraadeKode): TabellkortPeriode {
    val oppdrag =
        Oppdrag(
            id = OppdragId(1),
            fagsystemId = "TEST",
            fagomraade = fagomraade,
            bruker = "test",
            tidspktReg = LocalDateTime.parse("2026-01-01T00:00:00"),
        ).medOppdragslinjer(
            listOf(
                Oppdragslinje(
                    linjeId = 1,
                    delytelseId = "1",
                    sats = BigDecimal("100"),
                    typeSats = TypeSats.DAG,
                    datoVedtakFom = LocalDate.parse("2026-01-05"),
                    vedtakId = "vedtak-1",
                    kodeKlasse = YtelseKlasseKode(kode = YtelseKlasseKodeRef("KLASSE"), beskrivelse = "Test", grunnlagSkatt = false, grunnlagTrekk = false, fagomraader = emptyList()),
                    bruker = "test",
                    tidspktReg = LocalDateTime.parse("2026-01-01T00:00:00"),
                    vedtakssats = BigDecimal("100"),
                ),
            ),
        )

    val delperiode =
        OppdragDelperiode(
            oppdrag = oppdrag,
            periode =
                DatoIntervall(
                    fom = LocalDate.parse("2026-01-05"),
                    tom = LocalDate.parse("2026-01-06"),
                ),
            konto = "1234",
            gjelder = AktoerId(1),
            beregnet = LocalDateTime.parse("2026-01-01T00:00:00"),
            utbetalesTil = null,
            status = BeregningsStatus.BEREGNET,
            trekk = emptyList(),
            ekstraInfo = mutableListOf(),
            overfoeringsdato = null,
            tidligereTrukneTrekkvedtakIMaaneden = emptySet(),
        )

    return TabellkortPeriode(
        delperiode = delperiode,
        scratchpad = BeregningsScratchpad(),
        skatteGrunnlag = BigDecimal("200"),
        forskuddstrekk =
            Forskuddstrekk(
                typeForskuddsTrekk = ForskuddstrekkType.TABELLKORT,
                trekkKode = ForskuddstrekkKode.LOENN_FRA_HOVEDARBEIDSGIVER,
                trekktabell = "1",
            ),
    )
}
