package no.nav.sokos.dare.poc.beregning.beregningsmotor

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

import no.nav.sokos.dare.poc.TestObjects
import no.nav.sokos.dare.poc.TestUtil.oppdragFraFil
import no.nav.sokos.dare.poc.aktoer.AktoerId
import no.nav.sokos.dare.poc.beregning.Beregning
import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.beregning.Delberegning
import no.nav.sokos.dare.poc.beregning.DelberegningType
import no.nav.sokos.dare.poc.beregning.Delberegningslinje
import no.nav.sokos.dare.poc.beregning.EkstraInfo.Reasons.TREKK_MAANED_HALVERING
import no.nav.sokos.dare.poc.beregning.TekstEkstraInfo
import no.nav.sokos.dare.poc.beregning.fordeler.BeregningsFordeler
import no.nav.sokos.dare.poc.beregning.fordeler.SkatteKontekst
import no.nav.sokos.dare.poc.oppdrag.OppdragId
import no.nav.sokos.dare.poc.testFordelingInput
import no.nav.sokos.dare.poc.trekk.FagomraadeKode
import no.nav.sokos.dare.poc.trekk.TrekkAlternativ
import no.nav.sokos.dare.poc.trekk.TrekkvedtakId

class TrekkHalveringTest :
    FunSpec({
        listOf(1, 14, 15, 28).forEach { dag ->
            test("skal halvere månedssats ved første utbetaling den $dag. i måneden") {
                val linje = beregnTrekk(overfoeringsdato = LocalDate.of(2026, 3, dag))

                linje.sum shouldBe BigDecimal("-501")
                halveringsforklaring(linje) shouldNotBe null
            }
        }

        test("skal ikke halvere månedssats når samme trekk er trukket for oppdraget i måneden") {
            val linje =
                beregnTrekk(
                    overfoeringsdato = LocalDate.of(2026, 3, 14),
                    relaterteBeregninger = listOf(tidligereUtbetaling(trekkvedtakId = TrekkvedtakId(101))),
                )

            linje.sum shouldBe BigDecimal("-901")
            halveringsforklaring(linje) shouldBe null
        }

        test("skal halvere selv om samme oppdrag har en tidligere utbetaling uten trekk") {
            val linje = beregnTrekk(LocalDate.of(2026, 3, 14), relaterteBeregninger = listOf(tidligereUtbetaling()))

            linje.sum shouldBe BigDecimal("-501")
            halveringsforklaring(linje) shouldNotBe null
        }

        test("skal halvere selv om samme oppdrag har et annet tidligere trekk") {
            val linje =
                beregnTrekk(
                    LocalDate.of(2026, 3, 14),
                    relaterteBeregninger = listOf(tidligereUtbetaling(trekkvedtakId = TrekkvedtakId(999))),
                )

            linje.sum shouldBe BigDecimal("-501")
            halveringsforklaring(linje) shouldNotBe null
        }

        test("skal halvere selv om samme trekk er trukket for et annet oppdrag") {
            val linje =
                beregnTrekk(
                    LocalDate.of(2026, 3, 14),
                    relaterteBeregninger = listOf(tidligereUtbetaling(trekkvedtakId = TrekkvedtakId(101), oppdragId = OppdragId(999))),
                )

            linje.sum shouldBe BigDecimal("-501")
            halveringsforklaring(linje) shouldNotBe null
        }

        test("skal avgjøre halvering uavhengig for hvert trekkvedtak") {
            val linjer =
                beregnTrekkLinjer(
                    LocalDate.of(2026, 3, 14),
                    relaterteBeregninger = listOf(tidligereUtbetaling(trekkvedtakId = TrekkvedtakId(101))),
                    trekkvedtakIder = listOf(TrekkvedtakId(101), TrekkvedtakId(102)),
                ).associateBy { it.trekkvedtakId }

            linjer.getValue(TrekkvedtakId(101)).sum shouldBe BigDecimal("-901")
            halveringsforklaring(linjer.getValue(TrekkvedtakId(101))) shouldBe null
            linjer.getValue(TrekkvedtakId(102)).sum shouldBe BigDecimal("-501")
            halveringsforklaring(linjer.getValue(TrekkvedtakId(102))) shouldNotBe null
        }

        test("skal ikke koble oppdrag og trekk fra forskjellige delberegninger") {
            val utenTrekk = tidligereUtbetaling()
            val annetOppdrag = tidligereUtbetaling(trekkvedtakId = TrekkvedtakId(101), oppdragId = OppdragId(999))
            val historikk = utenTrekk.copy(delberegning = utenTrekk.delberegning + annetOppdrag.delberegning)
            val linje = beregnTrekk(LocalDate.of(2026, 3, 14), relaterteBeregninger = listOf(historikk))

            linje.sum shouldBe BigDecimal("-501")
            halveringsforklaring(linje) shouldNotBe null
        }

        test("skal ikke halvere månedssats for faggruppe uten halveringsinnstilling") {
            val linje =
                beregnTrekk(
                    overfoeringsdato = LocalDate.of(2026, 3, 14),
                    oppdragetsFagomraade = FagomraadeKode.SP,
                )

            linje.sum shouldBe BigDecimal("-1001")
            halveringsforklaring(linje) shouldBe null
        }

        test("skal ikke halvere dagsats, prosentsats eller YTELPER-trekk") {
            val tilfeller =
                listOf(
                    TrekkAlternativ.LOPD to TestObjects.TrekkType.UTBETALINGSDATO_IKKE_SKATTEREDUSERENDE,
                    TrekkAlternativ.LOPP to TestObjects.TrekkType.UTBETALINGSDATO_IKKE_SKATTEREDUSERENDE,
                    TrekkAlternativ.LOPM to TestObjects.TrekkType.YTELSESPERIODE_IKKE_SKATTEREDUSERENDE,
                )

            tilfeller.forEach { (trekkalternativ, trekktype) ->
                val linje =
                    beregnTrekk(
                        overfoeringsdato = LocalDate.of(2026, 3, 14),
                        trekkalternativ = trekkalternativ,
                        trekktype = trekktype,
                    )

                halveringsforklaring(linje) shouldBe null
            }
        }

        test("skal fortsatt begrense halvert månedssats med saldo") {
            val linje =
                beregnTrekk(
                    overfoeringsdato = LocalDate.of(2026, 3, 14),
                    trekkalternativ = TrekkAlternativ.SALM,
                    belopSaldo = BigDecimal("400"),
                )

            linje.sum shouldBe BigDecimal("-400")
            halveringsforklaring(linje) shouldNotBe null
        }

        test("skal fortsatt begrense halvert månedssats med kapasitet") {
            val linje =
                beregnTrekk(
                    overfoeringsdato = LocalDate.of(2026, 3, 14),
                    sats = BigDecimal("10000"),
                )

            linje.sum shouldBe BigDecimal("-4870")
            halveringsforklaring(linje) shouldNotBe null
        }
    })

private val BEREGNINGS_PERIODE = DatoIntervall("2026-03-02", "2026-03-06")
private val beregningsFordeler = BeregningsFordeler(Beregningsmotor(Ytelsesmotor(), Trekkmotor(), Skattemotor()))

private fun beregnTrekk(
    overfoeringsdato: LocalDate,
    sats: BigDecimal = BigDecimal("1001"),
    trekkalternativ: TrekkAlternativ = TrekkAlternativ.LOPM,
    trekktype: no.nav.sokos.dare.poc.trekk.Trekktype = TestObjects.TrekkType.UTBETALINGSDATO_IKKE_SKATTEREDUSERENDE,
    belopSaldo: BigDecimal? = null,
    oppdragetsFagomraade: FagomraadeKode = FagomraadeKode.AAP,
    relaterteBeregninger: List<Beregning> = emptyList(),
): Delberegningslinje = beregnTrekkLinjer(overfoeringsdato, sats, trekkalternativ, trekktype, belopSaldo, oppdragetsFagomraade, relaterteBeregninger).single()

private fun beregnTrekkLinjer(
    overfoeringsdato: LocalDate,
    sats: BigDecimal = BigDecimal("1001"),
    trekkalternativ: TrekkAlternativ = TrekkAlternativ.LOPM,
    trekktype: no.nav.sokos.dare.poc.trekk.Trekktype = TestObjects.TrekkType.UTBETALINGSDATO_IKKE_SKATTEREDUSERENDE,
    belopSaldo: BigDecimal? = null,
    oppdragetsFagomraade: FagomraadeKode = FagomraadeKode.AAP,
    relaterteBeregninger: List<Beregning> = emptyList(),
    trekkvedtakIder: List<TrekkvedtakId> = listOf(TrekkvedtakId(101)),
): List<Delberegningslinje> {
    val oppdrag =
        oppdragFraFil("aapApiTest/simple.xml").copy(
            id = OppdragId(1),
            fagomraade = oppdragetsFagomraade,
        )
    val trekk =
        trekkvedtakIder.map { id ->
            val vedtak =
                TestObjects.trekkVedtak(
                    trekkalt = trekkalternativ,
                    type = trekktype,
                    perioder = listOf(DatoIntervall("2026-03-01", "2026-03-31")),
                    sats = sats,
                    belopSaldo = belopSaldo,
                )
            vedtak.copy(id = id, trekkperioder = vedtak.trekkperioder.map { it.copy(trekkvedtakId = id.id) })
        }
    val input =
        testFordelingInput(
            beregnet = LocalDateTime.parse("2026-03-01T00:00:00"),
            overfoeringsdato = overfoeringsdato,
        )
    val kontekst =
        beregningsFordeler.byggBeregningsmotorKontekst(
            periode = BEREGNINGS_PERIODE,
            oppdrag = listOf(oppdrag),
            trekk = trekk,
            input = input,
            skatteKontekst = SkatteKontekst(skattekort = null, allokeringer = emptyMap()),
            relaterteBeregninger = relaterteBeregninger,
            overføringsdato = overfoeringsdato,
            beregnet = input.beregnet,
        )
    val beregnet = Ytelsesmotor().beregnYtelse(kontekst)
    val resultat = Trekkmotor().calculateOevrigeTrekk(beregnet)

    return resultat.segmenter
        .flatMap { it.scratchpad.values }
        .flatMap { it.beregninger.values.flatten() }
        .filter { it.type == DelberegningType.TREKK }
}

private fun tidligereUtbetaling(
    trekkvedtakId: TrekkvedtakId? = null,
    oppdragId: OppdragId = OppdragId(1),
) = Beregning(
    periode = DatoIntervall("2025-12-01", "2025-12-31"),
    konto = "",
    overfoeres = LocalDate.of(2026, 3, 1),
    fagomraade = FagomraadeKode.AAP,
    gjelder = AktoerId(1),
    beregnet = LocalDateTime.parse("2026-03-01T00:00:00"),
    delberegning =
        listOf(
            Delberegning(
                oppdragId = oppdragId,
                datoIntervall = DatoIntervall("2025-12-01", "2025-12-31"),
                linjer =
                    listOf(Delberegningslinje(type = DelberegningType.YTELSE, dagsbeloep = emptyList(), beloep = BigDecimal("1000"))) +
                        listOfNotNull(
                            trekkvedtakId?.let {
                                Delberegningslinje(type = DelberegningType.TREKK, dagsbeloep = emptyList(), beloep = BigDecimal("-100"), trekkvedtakId = it)
                            },
                        ),
            ),
        ),
)

private fun halveringsforklaring(linje: Delberegningslinje): TekstEkstraInfo? =
    linje.ekstra
        .filterIsInstance<TekstEkstraInfo>()
        .singleOrNull { it.grunn == TREKK_MAANED_HALVERING }
