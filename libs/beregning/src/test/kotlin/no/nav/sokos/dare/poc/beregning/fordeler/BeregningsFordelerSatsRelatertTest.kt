package no.nav.sokos.dare.poc.beregning.fordeler

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

import no.nav.sokos.dare.poc.TestObjects
import no.nav.sokos.dare.poc.TestUtil.oppdragFraFil
import no.nav.sokos.dare.poc.aktoer.AktoerId
import no.nav.sokos.dare.poc.beregning.Beregning
import no.nav.sokos.dare.poc.beregning.BeregningId
import no.nav.sokos.dare.poc.beregning.Dagbeloep
import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.beregning.Delberegning
import no.nav.sokos.dare.poc.beregning.DelberegningType
import no.nav.sokos.dare.poc.beregning.Delberegningslinje
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Beregningsmotor
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Skattemotor
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Trekkmotor
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Ytelsesmotor
import no.nav.sokos.dare.poc.beregning.dataklasser.BeregningsStatus
import no.nav.sokos.dare.poc.beregning.ekstern.BeregningsKilde
import no.nav.sokos.dare.poc.testFordelingInput
import no.nav.sokos.dare.poc.trekk.FagomraadeKode
import no.nav.sokos.dare.poc.trekk.TrekkAlternativ
import no.nav.sokos.dare.poc.trekk.TrekkvedtakId

private val fordeler = BeregningsFordeler(Beregningsmotor(Ytelsesmotor(), Trekkmotor(), Skattemotor()))

private fun relatertBeregningMedTrekk(
    trekkvedtakId: TrekkvedtakId,
    periode: DatoIntervall,
    beloep: BigDecimal,
): Beregning =
    Beregning(
        id = BeregningId(999),
        periode = periode,
        konto = "",
        overfoeres = LocalDate.now(),
        fagomraade = FagomraadeKode.AAP,
        gjelder = AktoerId(1),
        beregnet = LocalDateTime.now(),
        status = BeregningsStatus.BEREGNET,
        kilde = BeregningsKilde.POC,
        delberegning =
            listOf(
                Delberegning(
                    linjer =
                        listOf(
                            Delberegningslinje(
                                type = DelberegningType.TREKK,
                                dagsbeloep = emptyList(),
                                beloep = beloep,
                                trekkvedtakId = trekkvedtakId,
                            ),
                        ),
                    datoIntervall = periode,
                ),
            ),
    )

private fun relatertBeregningMedDagsbeloepTrekk(
    trekkvedtakId: TrekkvedtakId,
    periode: DatoIntervall,
    dagsbeloep: List<Dagbeloep>,
): Beregning =
    Beregning(
        id = BeregningId(999),
        periode = periode,
        konto = "",
        overfoeres = LocalDate.now(),
        fagomraade = FagomraadeKode.AAP,
        gjelder = AktoerId(1),
        beregnet = LocalDateTime.now(),
        status = BeregningsStatus.BEREGNET,
        kilde = BeregningsKilde.POC,
        delberegning =
            listOf(
                Delberegning(
                    linjer =
                        listOf(
                            Delberegningslinje(
                                type = DelberegningType.TREKK,
                                dagsbeloep = dagsbeloep,
                                beloep = BigDecimal.ZERO,
                                trekkvedtakId = trekkvedtakId,
                            ),
                        ),
                    datoIntervall = periode,
                ),
            ),
    )

class BeregningsFordelerSatsRelatertTest :
    FunSpec({

        test("dag-sats: relatert beregning som dekker én boette-dag trekkes fullt fra") {
            val vedtak =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.LOPD,
                    type = TestObjects.TrekkType.YTELSESPERIODE_SKATTEREDUSERENDE,
                    perioder = listOf(DatoIntervall("2026-03-02", "2026-03-05")),
                    sats = BigDecimal("200"),
                )

            val trekkvedtakIdForPeriode = TrekkvedtakId(vedtak.trekkperioder.first().trekkvedtakId)

            // Relatert beregning trakk 50 kr for én dag (2026-03-03), som er innenfor boette-perioden
            val relatert =
                relatertBeregningMedTrekk(
                    trekkvedtakId = trekkvedtakIdForPeriode,
                    periode = DatoIntervall("2026-03-03", "2026-03-03"),
                    beloep = BigDecimal("-50"),
                )

            val periode = DatoIntervall("2026-03-02", "2026-03-05")
            val oppdrag = listOf(oppdragFraFil("testdata/BeregningsFordelerSatsRelatertTest/enkel-ytelse-feb-til-mid-mar-2026.xml"))
            val trekk = listOf(vedtak)
            val input = testFordelingInput()

            val result =
                fordeler.byggBeregningsmotorKontekst(
                    periode = periode,
                    oppdrag = oppdrag,
                    trekk = trekk,
                    input = input,
                    skatteKontekst = SkatteKontekst(skattekort = null, allokeringer = emptyMap()),
                    relaterteBeregninger = listOf(relatert),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )

            val trekkperiodeId = vedtak.trekkperioder.first().id!!
            // Boette for 2026-03-03 should be reduced by 50
            result.satsBoetter.restSats(SatsBoetteId(trekkperiodeId, "2026-03-03")) shouldBe BigDecimal("150")
            // Other days unaffected
            result.satsBoetter.restSats(SatsBoetteId(trekkperiodeId, "2026-03-02")) shouldBe BigDecimal("200")
            result.satsBoetter.restSats(SatsBoetteId(trekkperiodeId, "2026-03-04")) shouldBe BigDecimal("200")
            result.satsBoetter.restSats(SatsBoetteId(trekkperiodeId, "2026-03-05")) shouldBe BigDecimal("200")
        }

        test("dag-sats: relatert beregning med bredere periode prorateres") {
            val vedtak =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.LOPD,
                    type = TestObjects.TrekkType.YTELSESPERIODE_SKATTEREDUSERENDE,
                    perioder = listOf(DatoIntervall("2026-03-02", "2026-03-06")),
                    sats = BigDecimal("200"),
                )

            val trekkvedtakIdForPeriode = TrekkvedtakId(vedtak.trekkperioder.first().trekkvedtakId)

            // Relatert beregning dekker 5 hverdager (man-fre), trakk 100 kr totalt
            // Boette for én dag skal bare få 100/5 = 20 trukket
            val relatert =
                relatertBeregningMedTrekk(
                    trekkvedtakId = trekkvedtakIdForPeriode,
                    periode = DatoIntervall("2026-03-02", "2026-03-06"),
                    beloep = BigDecimal("-100"),
                )

            val periode = DatoIntervall("2026-03-02", "2026-03-06")
            val oppdrag = listOf(oppdragFraFil("testdata/BeregningsFordelerSatsRelatertTest/enkel-ytelse-feb-til-mid-mar-2026.xml"))
            val trekk = listOf(vedtak)
            val input = testFordelingInput()

            val result =
                fordeler.byggBeregningsmotorKontekst(
                    periode = periode,
                    oppdrag = oppdrag,
                    trekk = trekk,
                    input = input,
                    skatteKontekst = SkatteKontekst(skattekort = null, allokeringer = emptyMap()),
                    relaterteBeregninger = listOf(relatert),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )

            val trekkperiodeId = vedtak.trekkperioder.first().id!!
            // 2026-03-02 to 2026-03-06 is mon-fri = 5 weekdays
            // Each day-boette overlaps 1 of 5 weekdays → 100 * 1/5 = 20 deducted per day
            result.satsBoetter.restSats(SatsBoetteId(trekkperiodeId, "2026-03-02")) shouldBe BigDecimal("180")
            result.satsBoetter.restSats(SatsBoetteId(trekkperiodeId, "2026-03-03")) shouldBe BigDecimal("180")
            result.satsBoetter.restSats(SatsBoetteId(trekkperiodeId, "2026-03-04")) shouldBe BigDecimal("180")
            result.satsBoetter.restSats(SatsBoetteId(trekkperiodeId, "2026-03-05")) shouldBe BigDecimal("180")
            result.satsBoetter.restSats(SatsBoetteId(trekkperiodeId, "2026-03-06")) shouldBe BigDecimal("180")
        }

        test("dag-sats: relatert beregning som ikke overlapper boette-dag påvirker ikke satsen") {
            val vedtak =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.LOPD,
                    type = TestObjects.TrekkType.YTELSESPERIODE_SKATTEREDUSERENDE,
                    perioder = listOf(DatoIntervall("2026-03-02", "2026-03-05")),
                    sats = BigDecimal("200"),
                )

            val trekkvedtakIdForPeriode = TrekkvedtakId(vedtak.trekkperioder.first().trekkvedtakId)

            // Relatert beregning er for en helt annen periode
            val relatert =
                relatertBeregningMedTrekk(
                    trekkvedtakId = trekkvedtakIdForPeriode,
                    periode = DatoIntervall("2026-04-01", "2026-04-03"),
                    beloep = BigDecimal("-100"),
                )

            val periode = DatoIntervall("2026-03-02", "2026-03-05")
            val oppdrag = listOf(oppdragFraFil("testdata/BeregningsFordelerSatsRelatertTest/enkel-ytelse-feb-til-mid-mar-2026.xml"))
            val trekk = listOf(vedtak)
            val input = testFordelingInput()

            val result =
                fordeler.byggBeregningsmotorKontekst(
                    periode = periode,
                    oppdrag = oppdrag,
                    trekk = trekk,
                    input = input,
                    skatteKontekst = SkatteKontekst(skattekort = null, allokeringer = emptyMap()),
                    relaterteBeregninger = listOf(relatert),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )

            val trekkperiodeId = vedtak.trekkperioder.first().id!!
            result.satsBoetter.restSats(SatsBoetteId(trekkperiodeId, "2026-03-02")) shouldBe BigDecimal("200")
            result.satsBoetter.restSats(SatsBoetteId(trekkperiodeId, "2026-03-03")) shouldBe BigDecimal("200")
            result.satsBoetter.restSats(SatsBoetteId(trekkperiodeId, "2026-03-04")) shouldBe BigDecimal("200")
            result.satsBoetter.restSats(SatsBoetteId(trekkperiodeId, "2026-03-05")) shouldBe BigDecimal("200")
        }

        test("måneds-sats: relatert beregning som dekker hele måneden trekkes fullt") {
            // Period must span multiple months to trigger monthly boette creation
            val vedtak =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.LOPM,
                    type = TestObjects.TrekkType.YTELSESPERIODE_SKATTEREDUSERENDE,
                    perioder = listOf(DatoIntervall("2026-02-01", "2026-03-31")),
                    sats = BigDecimal("1000"),
                )

            val trekkvedtakIdForPeriode = TrekkvedtakId(vedtak.trekkperioder.first().trekkvedtakId)

            // Relatert beregning dekker hele mars, trakk 300
            val relatert =
                relatertBeregningMedTrekk(
                    trekkvedtakId = trekkvedtakIdForPeriode,
                    periode = DatoIntervall("2026-03-01", "2026-03-31"),
                    beloep = BigDecimal("-300"),
                )

            val periode = DatoIntervall("2026-02-01", "2026-03-31")
            val oppdrag = listOf(oppdragFraFil("testdata/BeregningsFordelerSatsRelatertTest/enkel-ytelse-feb-til-mid-mar-2026.xml"))
            val trekk = listOf(vedtak)
            val input = testFordelingInput()

            val result =
                fordeler.byggBeregningsmotorKontekst(
                    periode = periode,
                    oppdrag = oppdrag,
                    trekk = trekk,
                    input = input,
                    skatteKontekst = SkatteKontekst(skattekort = null, allokeringer = emptyMap()),
                    relaterteBeregninger = listOf(relatert),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )

            val trekkperiodeId = vedtak.trekkperioder.first().id!!
            result.satsBoetter.restSats(SatsBoetteId(trekkperiodeId, "2026-03")) shouldBe BigDecimal("700")
        }

        test("måneds-sats: relatert beregning som delvis overlapper måneden prorateres") {
            // Period must span multiple months to trigger monthly boette creation
            val vedtak =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.LOPM,
                    type = TestObjects.TrekkType.YTELSESPERIODE_SKATTEREDUSERENDE,
                    perioder = listOf(DatoIntervall("2026-02-01", "2026-03-31")),
                    sats = BigDecimal("1000"),
                )

            val trekkvedtakIdForPeriode = TrekkvedtakId(vedtak.trekkperioder.first().trekkvedtakId)

            // Relatert beregning: delberegning spans Feb 1 - Mar 31 (larger than march boette)
            // march boette: Mar 1-31, hverdager = 22
            // delberegning hverdager: Feb 1-Mar 31 total = 20(feb) + 22(mar) = 42
            // 42 > 22, so proration applies
            // Overlap weekdays (mar portion of delberegning): 22
            // Prorated: 200 * 22/42 = 105 (rounded HALF_UP)
            // Rest: 1000 - 105 = 895
            val relatert =
                relatertBeregningMedTrekk(
                    trekkvedtakId = trekkvedtakIdForPeriode,
                    periode = DatoIntervall("2026-02-01", "2026-03-31"),
                    beloep = BigDecimal("-200"),
                )

            val periode = DatoIntervall("2026-02-01", "2026-03-31")
            val oppdrag = listOf(oppdragFraFil("testdata/BeregningsFordelerSatsRelatertTest/enkel-ytelse-feb-til-mid-mar-2026.xml"))
            val trekk = listOf(vedtak)
            val input = testFordelingInput()

            val result =
                fordeler.byggBeregningsmotorKontekst(
                    periode = periode,
                    oppdrag = oppdrag,
                    trekk = trekk,
                    input = input,
                    skatteKontekst = SkatteKontekst(skattekort = null, allokeringer = emptyMap()),
                    relaterteBeregninger = listOf(relatert),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )

            val trekkperiodeId = vedtak.trekkperioder.first().id!!
            // Feb 2026: 20 weekdays, Mar 2026: 22 weekdays, total = 42
            // Overlap with march boette = 22 weekdays
            // Lossless: 200/42 = 4 remainder 32. First 32 days get 5, rest get 4.
            // March days are indices 20-41: 12 days get 5, 10 days get 4 = 60+40 = 100
            // Rest: 1000 - 100 = 900
            result.satsBoetter.restSats(SatsBoetteId(trekkperiodeId, "2026-03")) shouldBe BigDecimal("900")
        }

        test("måneds-sats: flere relaterte beregninger summeres") {
            // Period must span multiple months to trigger monthly boette creation
            val vedtak =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.LOPM,
                    type = TestObjects.TrekkType.YTELSESPERIODE_SKATTEREDUSERENDE,
                    perioder = listOf(DatoIntervall("2026-02-01", "2026-03-31")),
                    sats = BigDecimal("1000"),
                )

            val trekkvedtakIdForPeriode = TrekkvedtakId(vedtak.trekkperioder.first().trekkvedtakId)

            // Both relaterte fit entirely within march
            val relatert1 =
                relatertBeregningMedTrekk(
                    trekkvedtakId = trekkvedtakIdForPeriode,
                    periode = DatoIntervall("2026-03-01", "2026-03-15"),
                    beloep = BigDecimal("-200"),
                )
            val relatert2 =
                relatertBeregningMedTrekk(
                    trekkvedtakId = trekkvedtakIdForPeriode,
                    periode = DatoIntervall("2026-03-16", "2026-03-31"),
                    beloep = BigDecimal("-150"),
                )

            val periode = DatoIntervall("2026-02-01", "2026-03-31")
            val oppdrag = listOf(oppdragFraFil("testdata/BeregningsFordelerSatsRelatertTest/enkel-ytelse-feb-til-mid-mar-2026.xml"))
            val trekk = listOf(vedtak)
            val input = testFordelingInput()

            val result =
                fordeler.byggBeregningsmotorKontekst(
                    periode = periode,
                    oppdrag = oppdrag,
                    trekk = trekk,
                    input = input,
                    skatteKontekst = SkatteKontekst(skattekort = null, allokeringer = emptyMap()),
                    relaterteBeregninger = listOf(relatert1, relatert2),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )

            val trekkperiodeId = vedtak.trekkperioder.first().id!!
            // Both delberegninger fit within the boette month → full amounts
            // 1000 - 200 - 150 = 650
            result.satsBoetter.restSats(SatsBoetteId(trekkperiodeId, "2026-03")) shouldBe BigDecimal("650")
        }

        test("relatert beregning med feil trekkvedtakId påvirker ikke satsen") {
            val vedtak =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.LOPD,
                    type = TestObjects.TrekkType.YTELSESPERIODE_SKATTEREDUSERENDE,
                    perioder = listOf(DatoIntervall("2026-03-02", "2026-03-05")),
                    sats = BigDecimal("200"),
                )

            val trekkvedtakIdForPeriode = TrekkvedtakId(vedtak.trekkperioder.first().trekkvedtakId)

            // Relatert beregning peker på et annet trekkvedtak
            val annetTrekkvedtakId = TrekkvedtakId(9999)
            val relatert =
                relatertBeregningMedTrekk(
                    trekkvedtakId = annetTrekkvedtakId,
                    periode = DatoIntervall("2026-03-02", "2026-03-05"),
                    beloep = BigDecimal("-100"),
                )

            val periode = DatoIntervall("2026-03-02", "2026-03-05")
            val oppdrag = listOf(oppdragFraFil("testdata/BeregningsFordelerSatsRelatertTest/enkel-ytelse-feb-til-mid-mar-2026.xml"))
            val trekk = listOf(vedtak)
            val input = testFordelingInput()

            val result =
                fordeler.byggBeregningsmotorKontekst(
                    periode = periode,
                    oppdrag = oppdrag,
                    trekk = trekk,
                    input = input,
                    skatteKontekst = SkatteKontekst(skattekort = null, allokeringer = emptyMap()),
                    relaterteBeregninger = listOf(relatert),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )

            val trekkperiodeId = vedtak.trekkperioder.first().id!!
            result.satsBoetter.restSats(SatsBoetteId(trekkperiodeId, "2026-03-02")) shouldBe BigDecimal("200")
            result.satsBoetter.restSats(SatsBoetteId(trekkperiodeId, "2026-03-03")) shouldBe BigDecimal("200")
            result.satsBoetter.restSats(SatsBoetteId(trekkperiodeId, "2026-03-04")) shouldBe BigDecimal("200")
            result.satsBoetter.restSats(SatsBoetteId(trekkperiodeId, "2026-03-05")) shouldBe BigDecimal("200")
        }

        test("relatert beregning med YTELSE-linje påvirker ikke satsen") {
            val vedtak =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.LOPD,
                    type = TestObjects.TrekkType.YTELSESPERIODE_SKATTEREDUSERENDE,
                    perioder = listOf(DatoIntervall("2026-03-02", "2026-03-05")),
                    sats = BigDecimal("200"),
                )

            val trekkvedtakIdForPeriode = TrekkvedtakId(vedtak.trekkperioder.first().trekkvedtakId)

            // Relatert beregning har en YTELSE-linje (ikke TREKK)
            val relatert =
                Beregning(
                    id = BeregningId(999),
                    periode = DatoIntervall("2026-03-02", "2026-03-05"),
                    konto = "",
                    overfoeres = LocalDate.now(),
                    fagomraade = FagomraadeKode.AAP,
                    gjelder = AktoerId(1),
                    beregnet = LocalDateTime.now(),
                    status = BeregningsStatus.BEREGNET,
                    kilde = BeregningsKilde.POC,
                    delberegning =
                        listOf(
                            Delberegning(
                                linjer =
                                    listOf(
                                        Delberegningslinje(
                                            type = DelberegningType.YTELSE,
                                            dagsbeloep = emptyList(),
                                            beloep = BigDecimal("-500"),
                                            trekkvedtakId = trekkvedtakIdForPeriode,
                                        ),
                                    ),
                                datoIntervall = DatoIntervall("2026-03-02", "2026-03-05"),
                            ),
                        ),
                )

            val periode = DatoIntervall("2026-03-02", "2026-03-05")
            val oppdrag = listOf(oppdragFraFil("testdata/BeregningsFordelerSatsRelatertTest/enkel-ytelse-feb-til-mid-mar-2026.xml"))
            val trekk = listOf(vedtak)
            val input = testFordelingInput()

            val result =
                fordeler.byggBeregningsmotorKontekst(
                    periode = periode,
                    oppdrag = oppdrag,
                    trekk = trekk,
                    input = input,
                    skatteKontekst = SkatteKontekst(skattekort = null, allokeringer = emptyMap()),
                    relaterteBeregninger = listOf(relatert),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )

            val trekkperiodeId = vedtak.trekkperioder.first().id!!
            result.satsBoetter.restSats(SatsBoetteId(trekkperiodeId, "2026-03-02")) shouldBe BigDecimal("200")
            result.satsBoetter.restSats(SatsBoetteId(trekkperiodeId, "2026-03-03")) shouldBe BigDecimal("200")
            result.satsBoetter.restSats(SatsBoetteId(trekkperiodeId, "2026-03-04")) shouldBe BigDecimal("200")
            result.satsBoetter.restSats(SatsBoetteId(trekkperiodeId, "2026-03-05")) shouldBe BigDecimal("200")
        }
        test("dag-sats: relatert beregning med dagsbeloep trekker riktig fra satsbøtte") {
            val vedtak =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.LOPD,
                    type = TestObjects.TrekkType.YTELSESPERIODE_SKATTEREDUSERENDE,
                    perioder = listOf(DatoIntervall("2026-03-02", "2026-03-05")),
                    sats = BigDecimal("200"),
                )

            val trekkvedtakIdForPeriode = TrekkvedtakId(vedtak.trekkperioder.first().trekkvedtakId)

            // Relatert beregning har dagsbeloep for én dag (2026-03-03), 50 kr trukket
            val relatert =
                relatertBeregningMedDagsbeloepTrekk(
                    trekkvedtakId = trekkvedtakIdForPeriode,
                    periode = DatoIntervall("2026-03-03", "2026-03-03"),
                    dagsbeloep = listOf(Dagbeloep(dato = LocalDate.of(2026, 3, 3), beloep = BigDecimal("-50"))),
                )

            val periode = DatoIntervall("2026-03-02", "2026-03-05")
            val oppdrag = listOf(oppdragFraFil("testdata/BeregningsFordelerSatsRelatertTest/enkel-ytelse-feb-til-mid-mar-2026.xml"))
            val trekk = listOf(vedtak)
            val input = testFordelingInput()

            val result =
                fordeler.byggBeregningsmotorKontekst(
                    periode = periode,
                    oppdrag = oppdrag,
                    trekk = trekk,
                    input = input,
                    skatteKontekst = SkatteKontekst(skattekort = null, allokeringer = emptyMap()),
                    relaterteBeregninger = listOf(relatert),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )

            val trekkperiodeId = vedtak.trekkperioder.first().id!!
            // Boette for 2026-03-03 should be reduced by 50
            result.satsBoetter.restSats(SatsBoetteId(trekkperiodeId, "2026-03-03")) shouldBe BigDecimal("150")
            // Other days unaffected
            result.satsBoetter.restSats(SatsBoetteId(trekkperiodeId, "2026-03-02")) shouldBe BigDecimal("200")
            result.satsBoetter.restSats(SatsBoetteId(trekkperiodeId, "2026-03-04")) shouldBe BigDecimal("200")
            result.satsBoetter.restSats(SatsBoetteId(trekkperiodeId, "2026-03-05")) shouldBe BigDecimal("200")
        }

        test("dag-sats: relatert beregning med dagsbeloep over flere dager trekker kun overlappende") {
            val vedtak =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.LOPD,
                    type = TestObjects.TrekkType.YTELSESPERIODE_SKATTEREDUSERENDE,
                    perioder = listOf(DatoIntervall("2026-03-03", "2026-03-05")),
                    sats = BigDecimal("200"),
                )

            val trekkvedtakIdForPeriode = TrekkvedtakId(vedtak.trekkperioder.first().trekkvedtakId)

            // Relatert beregning har dagsbeloep for 3 dager, men bare 2026-03-03 og 2026-03-04 overlapper med boetten
            val relatert =
                relatertBeregningMedDagsbeloepTrekk(
                    trekkvedtakId = trekkvedtakIdForPeriode,
                    periode = DatoIntervall("2026-03-02", "2026-03-04"),
                    dagsbeloep =
                        listOf(
                            Dagbeloep(dato = LocalDate.of(2026, 3, 2), beloep = BigDecimal("-30")),
                            Dagbeloep(dato = LocalDate.of(2026, 3, 3), beloep = BigDecimal("-40")),
                            Dagbeloep(dato = LocalDate.of(2026, 3, 4), beloep = BigDecimal("-60")),
                        ),
                )

            val periode = DatoIntervall("2026-03-03", "2026-03-05")
            val oppdrag = listOf(oppdragFraFil("testdata/BeregningsFordelerSatsRelatertTest/enkel-ytelse-feb-til-mid-mar-2026.xml"))
            val trekk = listOf(vedtak)
            val input = testFordelingInput()

            val result =
                fordeler.byggBeregningsmotorKontekst(
                    periode = periode,
                    oppdrag = oppdrag,
                    trekk = trekk,
                    input = input,
                    skatteKontekst = SkatteKontekst(skattekort = null, allokeringer = emptyMap()),
                    relaterteBeregninger = listOf(relatert),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )

            val trekkperiodeId = vedtak.trekkperioder.first().id!!
            // 2026-03-02 dagsbeloep (-30) is outside boette period, should not affect
            // 2026-03-03: 200 - 40 = 160
            result.satsBoetter.restSats(SatsBoetteId(trekkperiodeId, "2026-03-03")) shouldBe BigDecimal("160")
            // 2026-03-04: 200 - 60 = 140
            result.satsBoetter.restSats(SatsBoetteId(trekkperiodeId, "2026-03-04")) shouldBe BigDecimal("140")
            // 2026-03-05: unaffected
            result.satsBoetter.restSats(SatsBoetteId(trekkperiodeId, "2026-03-05")) shouldBe BigDecimal("200")
        }
    })
