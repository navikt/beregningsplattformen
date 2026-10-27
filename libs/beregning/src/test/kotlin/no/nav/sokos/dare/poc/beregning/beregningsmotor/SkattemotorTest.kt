package no.nav.sokos.dare.poc.beregning.beregningsmotor

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

import no.nav.sokos.dare.poc.TestObjects
import no.nav.sokos.dare.poc.TestUtil.oppdragFraFil
import no.nav.sokos.dare.poc.aktoer.AktoerId
import no.nav.sokos.dare.poc.beregning.Beregning
import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.beregning.Delberegning
import no.nav.sokos.dare.poc.beregning.DelberegningType
import no.nav.sokos.dare.poc.beregning.Delberegningslinje
import no.nav.sokos.dare.poc.beregning.EkstraInfo
import no.nav.sokos.dare.poc.beregning.LinjeKlassekode
import no.nav.sokos.dare.poc.beregning.TekstEkstraInfo
import no.nav.sokos.dare.poc.beregning.dataklasser.Satstype
import no.nav.sokos.dare.poc.beregning.fordeler.BeregningsFordeler
import no.nav.sokos.dare.poc.beregning.fordeler.BeregningsScratchpad
import no.nav.sokos.dare.poc.beregning.fordeler.SkatteKontekst
import no.nav.sokos.dare.poc.oppdrag.OppdragId
import no.nav.sokos.dare.poc.oppdrag.YtelseKlasseKode
import no.nav.sokos.dare.poc.oppdrag.YtelseKlasseKodeRef
import no.nav.sokos.dare.poc.skattekort.domain.Forskuddstrekk
import no.nav.sokos.dare.poc.skattekort.domain.ForskuddstrekkKode
import no.nav.sokos.dare.poc.skattekort.domain.ForskuddstrekkType
import no.nav.sokos.dare.poc.skattekort.domain.Skattekort
import no.nav.sokos.dare.poc.skattekort.domain.Tilleggsopplysning
import no.nav.sokos.dare.poc.skattetrekk.SkatteTabell
import no.nav.sokos.dare.poc.testFordelingInput
import no.nav.sokos.dare.poc.trekk.Faggruppe
import no.nav.sokos.dare.poc.trekk.FagomraadeKode
import no.nav.sokos.dare.poc.trekk.TrekkAlternativ
import no.nav.sokos.dare.poc.trekk.Trekkvedtak

class SkattemotorTest :
    FunSpec({

        val fordeler = BeregningsFordeler(Beregningsmotor(Ytelsesmotor(), Trekkmotor(), Skattemotor()))
        val ytelsesmotor = Ytelsesmotor()
        val trekkmotor = Trekkmotor()
        val skattemotor = Skattemotor()

        fun oppdragMedId(
            id: Long = 1,
            file: String = "simple.xml",
        ) = oppdragFraFil("testdata/SkattemotorTest/$file").copy(id = OppdragId(id))

        fun assertSkattelinjerHarKonto(
            skattelinjer: List<Delberegningslinje>,
            klassekode: String,
            konto: String,
        ) {
            skattelinjer.isNotEmpty() shouldBe true
            skattelinjer.forEach { skattelinje ->
                skattelinje.kodeKlasse shouldBe LinjeKlassekode.Skatt(klassekode)
                skattelinje.reskontroKonto shouldBe konto
            }
        }

        val prosentkortLoenn =
            listOf(
                Forskuddstrekk(
                    typeForskuddsTrekk = ForskuddstrekkType.PROSENTKORT,
                    trekkKode = ForskuddstrekkKode.LOENN_FRA_NAV,
                    prosentsats = BigDecimal("30"),
                ),
            )

        fun beregnSkattelinjerMedTilleggsopplysning(
            tilleggsopplysning: Set<Tilleggsopplysning>,
            forskuddsTrekk: List<Forskuddstrekk> = prosentkortLoenn,
            skattemotorInstans: Skattemotor = skattemotor,
        ): List<Delberegningslinje> {
            val periode = DatoIntervall("2026-03-02", "2026-03-06")
            val skattekort =
                Skattekort(
                    aktoerId = AktoerId(1),
                    inntektsaar = 2026,
                    kilde = "TEST",
                    identifikator = 1,
                    utstedtDato = LocalDate.of(2026, 1, 1),
                    tilleggsopplysning = tilleggsopplysning,
                    forskuddsTrekk = forskuddsTrekk,
                )
            val kontekst =
                fordeler.byggBeregningsmotorKontekst(
                    periode = periode,
                    oppdrag = listOf(oppdragMedId()),
                    trekk = emptyList(),
                    input = testFordelingInput(),
                    skatteKontekst = SkatteKontekst(skattekort = skattekort, allokeringer = emptyMap()),
                    relaterteBeregninger = emptyList(),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )
            val resultat = skattemotorInstans.beregnSkatteTrekk(ytelsesmotor.beregnYtelse(kontekst))

            return resultat.segmenter
                .flatMap { it.scratchpad.values }
                .flatMap { it.beregninger.values }
                .flatten()
                .filter { it.type == DelberegningType.SKATT }
        }

        test("beregnSkatteTrekk uten skattekort oppretter SKATT delberegningslinje med default prosentsats") {
            val beregningsPeriode = DatoIntervall("2026-03-02", "2026-03-06")
            val beregningsOppdrag = listOf(oppdragMedId())
            val beregningsTrekk = emptyList<Trekkvedtak>()
            val input = testFordelingInput()

            val kontekst =
                fordeler.byggBeregningsmotorKontekst(
                    periode = beregningsPeriode,
                    oppdrag = beregningsOppdrag,
                    trekk = beregningsTrekk,
                    input = input,
                    skatteKontekst = SkatteKontekst(skattekort = null, allokeringer = emptyMap()),
                    relaterteBeregninger = emptyList(),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )
            val medYtelse = ytelsesmotor.beregnYtelse(kontekst)

            val kapasitetFoerSkatt =
                medYtelse.segmenter
                    .first()
                    .scratchpad[OppdragId(1)]!!
                    .restKapasitet.values
                    .sumOf { it }

            val result = skattemotor.beregnSkatteTrekk(medYtelse)

            val segment = result.segmenter.first()
            val scratchpad = segment.scratchpad[OppdragId(1)]!!
            val beregninger = scratchpad.beregninger[beregningsPeriode]!!

            val skattLinjer = beregninger.filter { it.type == DelberegningType.SKATT }
            skattLinjer shouldHaveSize 1
            assertSkattelinjerHarKonto(skattLinjer, "FSKTSKAT", "0510000")

            val skattLinje = skattLinjer.first()
            skattLinje.satsType shouldBe Satstype.PROSENT
            skattLinje.sats shouldBe BigDecimal(50)

            val forventetBeloep = -(kapasitetFoerSkatt * BigDecimal(50) / BigDecimal(100)).setScale(0, RoundingMode.HALF_UP)
            skattLinje.beloep shouldBe forventetBeloep
        }

        test("beregnSkatteTrekk uten skattekort legger til TekstekstraInfo med MANGLENDE_SKATTEKORT") {
            val beregningsPeriode = DatoIntervall("2026-03-02", "2026-03-06")
            val oppdrag = oppdragMedId()

            val beregningsOppdrag = listOf(oppdragMedId())

            val beregningsTrekk = emptyList<Trekkvedtak>()
            val input = testFordelingInput()

            val kontekst =
                fordeler.byggBeregningsmotorKontekst(
                    periode = beregningsPeriode,
                    oppdrag = beregningsOppdrag,
                    trekk = beregningsTrekk,
                    input = input,
                    skatteKontekst = SkatteKontekst(skattekort = null, allokeringer = emptyMap()),
                    relaterteBeregninger = emptyList(),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )
            val medYtelse = ytelsesmotor.beregnYtelse(kontekst)
            val result = skattemotor.beregnSkatteTrekk(medYtelse)

            val segment = result.segmenter.first()
            val scratchpad = segment.scratchpad[OppdragId(1)]!!
            val skattelinjer = scratchpad.beregninger[beregningsPeriode]!!.filter { it.type == DelberegningType.SKATT }
            assertSkattelinjerHarKonto(skattelinjer, "FSKTSKAT", "0510000")
            val skattLinje = skattelinjer.first()

            skattLinje.ekstra shouldHaveSize 1
            val ekstraInfo = skattLinje.ekstra.first()
            ekstraInfo.shouldBeInstanceOf<TekstEkstraInfo>()
            ekstraInfo.grunn shouldBe EkstraInfo.Reasons.SKATT_MANGLENDE_SKATTEKORT
            ekstraInfo.toString() shouldContain "bruker fast skattesats 50 prosent"
        }

        test("beregnSkatteTrekk uten skattekort reduserer gjeldendeKapasitet med skattebeloep") {
            val beregningsPeriode = DatoIntervall("2026-03-02", "2026-03-06")
            val beregningsOppdrag = listOf(oppdragMedId())
            val beregningsTrekk = emptyList<Trekkvedtak>()
            val input = testFordelingInput()

            val kontekst =
                fordeler.byggBeregningsmotorKontekst(
                    periode = beregningsPeriode,
                    oppdrag = beregningsOppdrag,
                    trekk = beregningsTrekk,
                    input = input,
                    skatteKontekst = SkatteKontekst(skattekort = null, allokeringer = emptyMap()),
                    relaterteBeregninger = emptyList(),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )
            val medYtelse = ytelsesmotor.beregnYtelse(kontekst)

            val segment = medYtelse.segmenter.first()
            val scratchpad = segment.scratchpad[OppdragId(1)]!!
            val kapasitetFoerSkatt = scratchpad.restKapasitet.values.sumOf { it }

            val result = skattemotor.beregnSkatteTrekk(medYtelse)

            val scratchpadEtter = result.segmenter.first().scratchpad[OppdragId(1)]!!
            val kapasitetEtterSkatt = scratchpadEtter.restKapasitet.values.sumOf { it }

            // gjeldendeKapasitet reduseres med skattebeloepet
            val skattLinje = scratchpadEtter.beregninger[beregningsPeriode]!!.first { it.type == DelberegningType.SKATT }
            val forventetKapasitet = (kapasitetFoerSkatt + skattLinje.beloep).coerceAtLeast(BigDecimal.ZERO)
            kapasitetEtterSkatt shouldBe forventetKapasitet
        }

        test("beregnSkatteTrekk uten skattekort bruker fjoraars skattekort i januar") {
            val periode = DatoIntervall("2026-01-05", "2026-01-09")
            val oppdrag = listOf(oppdragMedId(file = "oppdrag_med_ytelse_i_januar-februar_2026.xml"))
            val input = testFordelingInput()

            val fjoraarsProsentkort =
                Skattekort(
                    aktoerId = AktoerId(1),
                    inntektsaar = 2025,
                    kilde = "TEST",
                    identifikator = 1,
                    utstedtDato = LocalDate.of(2025, 1, 1),
                    forskuddsTrekk =
                        listOf(
                            Forskuddstrekk(
                                typeForskuddsTrekk = ForskuddstrekkType.PROSENTKORT,
                                trekkKode = ForskuddstrekkKode.LOENN_FRA_NAV,
                                prosentsats = BigDecimal(30),
                            ),
                        ),
                )

            val kontekst =
                fordeler.byggBeregningsmotorKontekst(
                    periode = periode,
                    oppdrag = oppdrag,
                    trekk = emptyList<Trekkvedtak>(),
                    input = input,
                    skatteKontekst =
                        SkatteKontekst(
                            skattekort = fjoraarsProsentkort,
                            allokeringer = emptyMap(),
                        ),
                    relaterteBeregninger = emptyList(),
                    overføringsdato = LocalDateTime.parse("2026-01-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-01-19T00:00:00"),
                )
            val medYtelse = ytelsesmotor.beregnYtelse(kontekst)
            val result = skattemotor.beregnSkatteTrekk(medYtelse)

            val scratchpad = result.segmenter.first().scratchpad[OppdragId(1)]!!
            val skattLinje =
                scratchpad.beregninger.values
                    .flatten()
                    .first { it.type == DelberegningType.SKATT }

            skattLinje.sats shouldBe BigDecimal(30)
        }

        test("beregnSkatteTrekk uten skattekort bruker fast skattesats etter januar") {
            val periode = DatoIntervall("2026-02-02", "2026-02-06")
            val oppdrag = listOf(oppdragMedId(file = "oppdrag_med_ytelse_i_januar-februar_2026.xml"))
            val input = testFordelingInput()

            val fjoraarsProsentkort =
                Skattekort(
                    aktoerId = AktoerId(1),
                    inntektsaar = 2025,
                    kilde = "TEST",
                    identifikator = 1,
                    utstedtDato = LocalDate.of(2025, 1, 1),
                    forskuddsTrekk =
                        listOf(
                            Forskuddstrekk(
                                typeForskuddsTrekk = ForskuddstrekkType.PROSENTKORT,
                                trekkKode = ForskuddstrekkKode.LOENN_FRA_NAV,
                                prosentsats = BigDecimal(30),
                            ),
                        ),
                )

            val kontekst =
                fordeler.byggBeregningsmotorKontekst(
                    periode = periode,
                    oppdrag = oppdrag,
                    trekk = emptyList<Trekkvedtak>(),
                    input = input,
                    skatteKontekst =
                        SkatteKontekst(
                            skattekort = fjoraarsProsentkort,
                            allokeringer = emptyMap(),
                        ),
                    relaterteBeregninger = emptyList(),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )
            val medYtelse = ytelsesmotor.beregnYtelse(kontekst)
            val result = skattemotor.beregnSkatteTrekk(medYtelse)

            val scratchpad = result.segmenter.first().scratchpad[OppdragId(1)]!!
            val skattLinje =
                scratchpad.beregninger.values
                    .flatten()
                    .first { it.type == DelberegningType.SKATT }

            skattLinje.sats shouldBe BigDecimal(50)
            val manglerInfo =
                skattLinje.ekstra
                    .filterIsInstance<TekstEkstraInfo>()
                    .first { it.grunn == EkstraInfo.Reasons.SKATT_MANGLENDE_SKATTEKORT }
            manglerInfo.toString() shouldContain "fast skattesats"
            manglerInfo.toString() shouldContain "50"
        }

        test("beregnSkatteTrekk med prosentkort beregner skatt med angitt prosentsats") {
            val beregningsPeriode = DatoIntervall("2026-03-02", "2026-03-06")
            val beregningsOppdrag = listOf(oppdragMedId())
            val beregningsTrekk = emptyList<Trekkvedtak>()
            val input = testFordelingInput()

            val prosentsats = BigDecimal("33.5")
            val skattekort =
                Skattekort(
                    aktoerId = AktoerId(1),
                    inntektsaar = 2026,
                    kilde = "TEST",
                    identifikator = 1,
                    utstedtDato = LocalDate.of(2026, 1, 1),
                    forskuddsTrekk =
                        listOf(
                            Forskuddstrekk(
                                typeForskuddsTrekk = ForskuddstrekkType.PROSENTKORT,
                                trekkKode = ForskuddstrekkKode.LOENN_FRA_NAV,
                                prosentsats = prosentsats,
                            ),
                        ),
                )

            val kontekst =
                fordeler.byggBeregningsmotorKontekst(
                    periode = beregningsPeriode,
                    oppdrag = beregningsOppdrag,
                    trekk = beregningsTrekk,
                    input = input,
                    skatteKontekst = SkatteKontekst(skattekort = skattekort, allokeringer = emptyMap()),
                    relaterteBeregninger = emptyList(),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )
            val medYtelse = ytelsesmotor.beregnYtelse(kontekst)

            val kapasitetFoerSkatt =
                medYtelse.segmenter
                    .first()
                    .scratchpad[OppdragId(1)]!!
                    .restKapasitet.values
                    .sumOf { it }

            val result = skattemotor.beregnSkatteTrekk(medYtelse)

            val segment = result.segmenter.first()
            val scratchpad = segment.scratchpad[OppdragId(1)]!!
            val beregninger = scratchpad.beregninger[beregningsPeriode]!!

            val skattLinjer = beregninger.filter { it.type == DelberegningType.SKATT }
            skattLinjer shouldHaveSize 1
            assertSkattelinjerHarKonto(skattLinjer, "FSKTSKAT", "0510000")

            val skattLinje = skattLinjer.first()
            skattLinje.satsType shouldBe Satstype.PROSENT
            skattLinje.sats shouldBe prosentsats

            val forventetBeloep = -(kapasitetFoerSkatt * prosentsats / BigDecimal(100)).setScale(0, RoundingMode.HALF_UP)
            skattLinje.beloep shouldBe forventetBeloep
        }

        test("beregnSkatteTrekk med prosentkort legger til SKATT_TREKK_PROSENT ekstraInfo") {
            val beregningsPeriode = DatoIntervall("2026-03-02", "2026-03-06")
            val beregningsOppdrag = listOf(oppdragMedId())
            val beregningsTrekk = emptyList<Trekkvedtak>()
            val input = testFordelingInput()

            val prosentsats = BigDecimal("33.5")
            val skattekort =
                Skattekort(
                    aktoerId = AktoerId(1),
                    inntektsaar = 2026,
                    kilde = "TEST",
                    identifikator = 1,
                    utstedtDato = LocalDate.of(2026, 1, 1),
                    forskuddsTrekk =
                        listOf(
                            Forskuddstrekk(
                                typeForskuddsTrekk = ForskuddstrekkType.PROSENTKORT,
                                trekkKode = ForskuddstrekkKode.LOENN_FRA_NAV,
                                prosentsats = prosentsats,
                            ),
                        ),
                )

            val kontekst =
                fordeler.byggBeregningsmotorKontekst(
                    periode = beregningsPeriode,
                    oppdrag = beregningsOppdrag,
                    trekk = beregningsTrekk,
                    input = input,
                    skatteKontekst = SkatteKontekst(skattekort = skattekort, allokeringer = emptyMap()),
                    relaterteBeregninger = emptyList(),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )
            val medYtelse = ytelsesmotor.beregnYtelse(kontekst)
            val result = skattemotor.beregnSkatteTrekk(medYtelse)

            val segment = result.segmenter.first()
            val scratchpad = segment.scratchpad[OppdragId(1)]!!
            val skattLinje = scratchpad.beregninger[beregningsPeriode]!!.first { it.type == DelberegningType.SKATT }

            skattLinje.ekstra shouldHaveSize 1
            val ekstraInfo = skattLinje.ekstra.first()
            ekstraInfo.shouldBeInstanceOf<TekstEkstraInfo>()
            ekstraInfo.grunn shouldBe EkstraInfo.Reasons.SKATT_TREKK_PROSENT
        }

        test("beregnSkatteTrekk med prosentkort reduserer restKapasitet") {
            val beregningsPeriode = DatoIntervall("2026-03-02", "2026-03-06")
            val beregningsOppdrag = listOf(oppdragMedId())
            val beregningsTrekk = emptyList<Trekkvedtak>()
            val input = testFordelingInput()

            val prosentsats = BigDecimal("33.5")
            val skattekort =
                Skattekort(
                    aktoerId = AktoerId(1),
                    inntektsaar = 2026,
                    kilde = "TEST",
                    identifikator = 1,
                    utstedtDato = LocalDate.of(2026, 1, 1),
                    forskuddsTrekk =
                        listOf(
                            Forskuddstrekk(
                                typeForskuddsTrekk = ForskuddstrekkType.PROSENTKORT,
                                trekkKode = ForskuddstrekkKode.LOENN_FRA_NAV,
                                prosentsats = prosentsats,
                            ),
                        ),
                )

            val kontekst =
                fordeler.byggBeregningsmotorKontekst(
                    periode = beregningsPeriode,
                    oppdrag = beregningsOppdrag,
                    trekk = beregningsTrekk,
                    input = input,
                    skatteKontekst = SkatteKontekst(skattekort = skattekort, allokeringer = emptyMap()),
                    relaterteBeregninger = emptyList(),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )
            val medYtelse = ytelsesmotor.beregnYtelse(kontekst)

            val kapasitetFoerSkatt =
                medYtelse.segmenter
                    .first()
                    .scratchpad[OppdragId(1)]!!
                    .restKapasitet.values
                    .sumOf { it }

            val result = skattemotor.beregnSkatteTrekk(medYtelse)

            val scratchpadEtter = result.segmenter.first().scratchpad[OppdragId(1)]!!
            val kapasitetEtterSkatt = scratchpadEtter.restKapasitet.values.sumOf { it }

            val skattLinje = scratchpadEtter.beregninger[beregningsPeriode]!!.first { it.type == DelberegningType.SKATT }
            val forventetKapasitet = (kapasitetFoerSkatt + skattLinje.beloep).coerceAtLeast(BigDecimal.ZERO)
            kapasitetEtterSkatt shouldBe forventetKapasitet
        }

        test("beregnSkatteTrekk med prosentkort bruker fast skattesats for delperiode i forrige år") {
            val periode = DatoIntervall("2025-12-29", "2026-01-02")
            val oppdrag = listOf(oppdragMedId(file = "oppdrag_med_ytelse_fra_2025_til_2026.xml"))
            val input = testFordelingInput()

            val skattekort =
                Skattekort(
                    aktoerId = AktoerId(1),
                    inntektsaar = 2026,
                    kilde = "TEST",
                    identifikator = 1,
                    utstedtDato = LocalDate.of(2026, 1, 1),
                    tilleggsopplysning = setOf(Tilleggsopplysning.KILDESKATT_PAA_LOENN),
                    forskuddsTrekk =
                        listOf(
                            Forskuddstrekk(
                                typeForskuddsTrekk = ForskuddstrekkType.PROSENTKORT,
                                trekkKode = ForskuddstrekkKode.LOENN_FRA_NAV,
                                prosentsats = BigDecimal("33.5"),
                            ),
                        ),
                )

            val kontekst =
                fordeler.byggBeregningsmotorKontekst(
                    periode = periode,
                    oppdrag = oppdrag,
                    trekk = emptyList<Trekkvedtak>(),
                    input = input,
                    skatteKontekst = SkatteKontekst(skattekort = skattekort, allokeringer = emptyMap()),
                    relaterteBeregninger = emptyList(),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )
            val medYtelse = ytelsesmotor.beregnYtelse(kontekst)
            val result = skattemotor.beregnSkatteTrekk(medYtelse)

            val allScratchpads =
                result.segmenter.flatMap { segment ->
                    segment.scratchpad.values.flatMap { sp ->
                        sp.beregninger.values.flatten()
                    }
                }
            val skattLinjer = allScratchpads.filter { it.type == DelberegningType.SKATT }

            val decemberSkatt = skattLinjer.first { it.sats == BigDecimal(44) }
            decemberSkatt.kodeKlasse shouldBe LinjeKlassekode.Skatt("KSKTSKAT")
            decemberSkatt.reskontroKonto shouldBe "0512435"
            val forrigeAarInfo =
                decemberSkatt.ekstra
                    .filterIsInstance<TekstEkstraInfo>()
                    .first { it.grunn == EkstraInfo.Reasons.SKATTESATS_FORRIGE_AAR }
            forrigeAarInfo.toString() shouldContain "forrige år"

            val januarSkatt = skattLinjer.first { it.sats == BigDecimal("33.5") }
            januarSkatt.ekstra
                .filterIsInstance<TekstEkstraInfo>()
                .any { it.grunn == EkstraInfo.Reasons.SKATT_TREKK_PROSENT } shouldBe true
        }

        fun frikortSkattekort(
            trekkKode: ForskuddstrekkKode = ForskuddstrekkKode.LOENN_FRA_NAV,
            frikortBelop: Int = 100,
            tilleggsopplysning: Set<Tilleggsopplysning> = emptySet(),
        ) = Skattekort(
            aktoerId = AktoerId(1),
            inntektsaar = 2026,
            kilde = "TEST",
            identifikator = 1,
            utstedtDato = LocalDate.of(2026, 1, 1),
            forskuddsTrekk =
                listOf(
                    Forskuddstrekk(
                        typeForskuddsTrekk = ForskuddstrekkType.FRIKORT,
                        trekkKode = trekkKode,
                        frikortBelop = frikortBelop,
                    ),
                ),
            tilleggsopplysning = tilleggsopplysning,
        )

        fun beregnFrikortMedOverskudd(
            skattekort: Skattekort,
            fagomraade: FagomraadeKode = FagomraadeKode.AAP,
        ): BeregningsScratchpad {
            val oppdrag =
                oppdragMedId().let {
                    it.copy(fagomraade = fagomraade)
                }

            val periode = DatoIntervall("2026-03-02", "2026-03-06")
            val input = testFordelingInput()

            val kontekst =
                fordeler.byggBeregningsmotorKontekst(
                    periode = periode,
                    oppdrag = listOf(oppdrag),
                    trekk = emptyList<Trekkvedtak>(),
                    input = input,
                    skatteKontekst = SkatteKontekst(skattekort = skattekort, allokeringer = emptyMap()),
                    relaterteBeregninger = emptyList(),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )
            kontekst.restFrikortBeloep = BigDecimal(skattekort.forskuddsTrekk.first().frikortBelop!!)

            val medYtelse = ytelsesmotor.beregnYtelse(kontekst)
            val result = skattemotor.beregnSkatteTrekk(medYtelse)

            return result.segmenter.first().scratchpad[OppdragId(1)]!!
        }

        test("frikort should create frikortbruk line that is not booking") {
            val skattekort = frikortSkattekort(frikortBelop = 100)
            val scratchpad = beregnFrikortMedOverskudd(skattekort)

            val frikortLinje =
                scratchpad.beregninger.values
                    .flatten()
                    .first { it.type == DelberegningType.FRIKORT_BRUK }

            frikortLinje.beloep shouldBe BigDecimal(100)
            frikortLinje.bokforende shouldBe false
        }

        test("frikort full dekning should create frikortbruk line with zero skatt") {
            val skattekort = frikortSkattekort(frikortBelop = 100000)
            val scratchpad = beregnFrikortMedOverskudd(skattekort)

            val skattLinje =
                scratchpad.beregninger.values
                    .flatten()
                    .first { it.type == DelberegningType.SKATT }
            skattLinje.kodeKlasse shouldBe LinjeKlassekode.Skatt("FSKTSKAT")
            skattLinje.reskontroKonto shouldBe "0510000"
            val frikortLinje =
                scratchpad.beregninger.values
                    .flatten()
                    .first { it.type == DelberegningType.FRIKORT_BRUK }

            skattLinje.beloep shouldBe BigDecimal.ZERO
            (frikortLinje.beloep > BigDecimal.ZERO) shouldBe true
            frikortLinje.kodeKlasse shouldBe null
            frikortLinje.reskontroKonto shouldBe null
        }

        test("frikort overskudd should use default skattesats 50% for LOENN uten tilleggsopplysning") {
            val skattekort = frikortSkattekort()
            val scratchpad = beregnFrikortMedOverskudd(skattekort)
            val skattLinje =
                scratchpad.beregninger.values
                    .flatten()
                    .first { it.type == DelberegningType.SKATT }
            skattLinje.kodeKlasse shouldBe LinjeKlassekode.Skatt("FSKTSKAT")
            skattLinje.reskontroKonto shouldBe "0510000"

            skattLinje.sats shouldBe BigDecimal(50)

            val skattesatsInfo =
                skattLinje.ekstra
                    .filterIsInstance<TekstEkstraInfo>() shouldHaveSingleElement { it.grunn == EkstraInfo.Reasons.FRIKORT_MED_BELOEPSGRENSE_BRUKT_RESTSKATT }
            skattesatsInfo.toString() shouldContain "50"
        }

        test("beregnSkatteTrekk uten skattekort med skattereduserende trekk trekker fra trekkbeloep i grunnlag") {
            val periode = DatoIntervall("2026-03-02", "2026-03-06")
            val trekkvedtak =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.LOPM,
                    type = TestObjects.TrekkType.YTELSESPERIODE_SKATTEREDUSERENDE,
                    perioder = listOf(periode),
                    sats = BigDecimal("200"),
                )

            val beregningsPeriode = periode
            val beregningsOppdrag = listOf(oppdragMedId())
            val beregningsTrekk = listOf(trekkvedtak)
            val input = testFordelingInput()

            val kontekst =
                fordeler.byggBeregningsmotorKontekst(
                    periode = beregningsPeriode,
                    oppdrag = beregningsOppdrag,
                    trekk = beregningsTrekk,
                    input = input,
                    skatteKontekst = SkatteKontekst(skattekort = null, allokeringer = emptyMap()),
                    relaterteBeregninger = emptyList(),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )
            val medYtelse = ytelsesmotor.beregnYtelse(kontekst)
            val medSkattereduserendeTrekk = trekkmotor.calculateSkattereduserendeTrekk(medYtelse)

            // gjeldendeKapasitet etter skattereduserende trekk er skatteGrunnlaget
            val kapasitetEtterTrekk =
                medSkattereduserendeTrekk.segmenter
                    .first()
                    .scratchpad[OppdragId(1)]!!
                    .restKapasitet.values
                    .sumOf { it }

            val result = skattemotor.beregnSkatteTrekk(medSkattereduserendeTrekk)

            val segment = result.segmenter.first()
            val scratchpad = segment.scratchpad[OppdragId(1)]!!
            val skattLinje = scratchpad.beregninger[periode]!!.first { it.type == DelberegningType.SKATT }

            val forventetBeloep = -(kapasitetEtterTrekk * BigDecimal(50) / BigDecimal(100)).setScale(0, RoundingMode.HALF_UP)
            skattLinje.beloep shouldBe forventetBeloep
        }

        test("beregnSkatteTrekk med tabellkort og etterbetaling beregner prosent-skatt med SKATT_ETTERBETALING") {
            val periode = DatoIntervall("2026-03-02", "2026-03-06")

            val relatertBeregning =
                Beregning(
                    periode = periode,
                    konto = "",
                    overfoeres = LocalDate.of(2026, 3, 1),
                    fagomraade = FagomraadeKode.AAP,
                    gjelder = AktoerId(1),
                    beregnet = LocalDateTime.of(2026, 3, 1, 12, 0),
                    delberegning =
                        listOf(
                            Delberegning(
                                oppdragId = OppdragId(1),
                                linjer =
                                    listOf(
                                        Delberegningslinje(
                                            type = DelberegningType.YTELSE,
                                            dagsbeloep = emptyList(),
                                            beloep = BigDecimal("4870"),
                                            kodeKlasse =
                                                LinjeKlassekode.Ytelse(
                                                    YtelseKlasseKode(
                                                        kode = YtelseKlasseKodeRef("AAPUAA"),
                                                        beskrivelse = "Test",
                                                        grunnlagSkatt = false,
                                                        grunnlagTrekk = false,
                                                        fagomraader = emptyList(),
                                                    ),
                                                ),
                                        ),
                                    ),
                                datoIntervall = periode,
                            ),
                        ),
                )

            val beregningsPeriode = periode
            val beregningsOppdrag = listOf(oppdragMedId())
            val beregningsTrekk = emptyList<Trekkvedtak>()
            val input = testFordelingInput()

            val prosentsats = BigDecimal("30")
            val skattekort =
                Skattekort(
                    aktoerId = AktoerId(1),
                    inntektsaar = 2026,
                    kilde = "TEST",
                    identifikator = 1,
                    utstedtDato = LocalDate.of(2026, 1, 1),
                    forskuddsTrekk =
                        listOf(
                            Forskuddstrekk(
                                typeForskuddsTrekk = ForskuddstrekkType.TABELLKORT,
                                trekkKode = ForskuddstrekkKode.LOENN_FRA_NAV,
                                trekktabell = "8010",
                                prosentsats = prosentsats,
                            ),
                        ),
                )

            val kontekst =
                fordeler.byggBeregningsmotorKontekst(
                    periode = beregningsPeriode,
                    oppdrag = beregningsOppdrag,
                    trekk = beregningsTrekk,
                    input = input,
                    skatteKontekst =
                        SkatteKontekst(
                            skattekort = skattekort,
                            allokeringer = mapOf(ForskuddstrekkKode.LOENN_FRA_NAV to Faggruppe.ARBYT),
                        ),
                    relaterteBeregninger = listOf(relatertBeregning),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )
            val medYtelse = ytelsesmotor.beregnYtelse(kontekst)
            val result = skattemotor.beregnSkatteTrekk(medYtelse)

            val segment = result.segmenter.first()
            val scratchpad = segment.scratchpad[OppdragId(1)]!!
            val skattLinjer = scratchpad.beregninger[periode]!!.filter { it.type == DelberegningType.SKATT }
            skattLinjer shouldHaveSize 1
            assertSkattelinjerHarKonto(skattLinjer, "FSKTSKAT", "0510000")

            val skattLinje = skattLinjer.first()
            skattLinje.satsType shouldBe Satstype.PROSENT
            skattLinje.sats shouldBe prosentsats

            val ekstraInfo = skattLinje.ekstra.first()
            ekstraInfo.shouldBeInstanceOf<TekstEkstraInfo>()
            ekstraInfo.grunn shouldBe EkstraInfo.Reasons.SKATT_ETTERBETALING
        }

        test("beregnSkatteTrekk med tabellkort og biarbeidsgiver beregner prosent-skatt med SKATT_BIARBEIDSGIVER") {
            val periode = DatoIntervall("2026-03-02", "2026-03-06")

            val beregningsPeriode = periode
            val beregningsOppdrag = listOf(oppdragMedId())
            val beregningsTrekk = emptyList<Trekkvedtak>()
            val input = testFordelingInput()

            val prosentsats = BigDecimal("30")
            val skattekort =
                Skattekort(
                    aktoerId = AktoerId(1),
                    inntektsaar = 2026,
                    kilde = "TEST",
                    identifikator = 1,
                    utstedtDato = LocalDate.of(2026, 1, 1),
                    forskuddsTrekk =
                        listOf(
                            Forskuddstrekk(
                                typeForskuddsTrekk = ForskuddstrekkType.TABELLKORT,
                                trekkKode = ForskuddstrekkKode.LOENN_FRA_NAV,
                                trekktabell = "8010",
                                prosentsats = prosentsats,
                            ),
                        ),
                )

            // Allokert til KORTTID, men oppdrag er ARBYT → biarbeidsgiver
            val kontekst =
                fordeler.byggBeregningsmotorKontekst(
                    periode = beregningsPeriode,
                    oppdrag = beregningsOppdrag,
                    trekk = beregningsTrekk,
                    input = input,
                    skatteKontekst =
                        SkatteKontekst(
                            skattekort = skattekort,
                            allokeringer = mapOf(ForskuddstrekkKode.LOENN_FRA_NAV to Faggruppe.KORTTID),
                        ),
                    relaterteBeregninger = emptyList(),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )
            val medYtelse = ytelsesmotor.beregnYtelse(kontekst)

            val kapasitetFoerSkatt =
                medYtelse.segmenter
                    .first()
                    .scratchpad[OppdragId(1)]!!
                    .restKapasitet.values
                    .sumOf { it }

            val result = skattemotor.beregnSkatteTrekk(medYtelse)

            val segment = result.segmenter.first()
            val scratchpad = segment.scratchpad[OppdragId(1)]!!
            val skattLinjer = scratchpad.beregninger[periode]!!.filter { it.type == DelberegningType.SKATT }
            skattLinjer shouldHaveSize 1
            assertSkattelinjerHarKonto(skattLinjer, "FSKTSKAT", "0510000")

            val skattLinje = skattLinjer.first()
            skattLinje.satsType shouldBe Satstype.PROSENT
            skattLinje.sats shouldBe prosentsats

            val forventetBeloep2 = -(kapasitetFoerSkatt * prosentsats / BigDecimal(100)).setScale(0, RoundingMode.HALF_UP)
            skattLinje.beloep shouldBe forventetBeloep2

            val ekstraInfo = skattLinje.ekstra.first()
            ekstraInfo.shouldBeInstanceOf<TekstEkstraInfo>()
            ekstraInfo.grunn shouldBe EkstraInfo.Reasons.SKATT_BIARBEIDSGIVER
        }

        test("beregnSkatteTrekk med tabellkort hovedarbeidsgiver beregner tabelltrekk med korrekt fordeling") {
            val periode = DatoIntervall("2026-03-02", "2026-03-06")

            val beregningsPeriode = periode
            val beregningsOppdrag = listOf(oppdragMedId())
            val beregningsTrekk = emptyList<Trekkvedtak>()
            val input = testFordelingInput()

            val tabellNummer = "8010"
            val skattekort =
                Skattekort(
                    aktoerId = AktoerId(1),
                    inntektsaar = 2026,
                    kilde = "TEST",
                    identifikator = 1,
                    utstedtDato = LocalDate.of(2026, 1, 1),
                    forskuddsTrekk =
                        listOf(
                            Forskuddstrekk(
                                typeForskuddsTrekk = ForskuddstrekkType.TABELLKORT,
                                trekkKode = ForskuddstrekkKode.LOENN_FRA_NAV,
                                trekktabell = tabellNummer,
                                prosentsats = BigDecimal("30"),
                            ),
                        ),
                )

            // Allokert til ARBYT, oppdrag er ARBYT → hovedarbeidsgiver
            val kontekst =
                fordeler.byggBeregningsmotorKontekst(
                    periode = beregningsPeriode,
                    oppdrag = beregningsOppdrag,
                    trekk = beregningsTrekk,
                    input = input,
                    skatteKontekst =
                        SkatteKontekst(
                            skattekort = skattekort,
                            allokeringer = mapOf(ForskuddstrekkKode.LOENN_FRA_NAV to Faggruppe.ARBYT),
                        ),
                    relaterteBeregninger = emptyList(),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )
            val medYtelse = ytelsesmotor.beregnYtelse(kontekst)

            val scratchpadFoer = medYtelse.segmenter.first().scratchpad[OppdragId(1)]!!
            val skatteGrunnlag = periode.hverdager.sumOf { scratchpadFoer.restKapasitet[it] ?: BigDecimal.ZERO }
            val dager = periode.hverdager.size

            val result = skattemotor.beregnSkatteTrekk(medYtelse)

            val segment = result.segmenter.first()
            val scratchpad = segment.scratchpad[OppdragId(1)]!!
            val skattLinjer = scratchpad.beregninger[periode]!!.filter { it.type == DelberegningType.SKATT }
            skattLinjer shouldHaveSize 1
            assertSkattelinjerHarKonto(skattLinjer, "FSKTSKAT", "0510000")

            val skattLinje = skattLinjer.first()
            skattLinje.satsType shouldBe Satstype.TABELL

            // Verify amount matches SkatteTabell calculation
            val forventetTotalSkatt = SkatteTabell.beregnTabellTrekk(tabellNummer, dager, skatteGrunnlag).trekkbeloep
            skattLinje.beloep shouldBe -forventetTotalSkatt

            val ekstraInfo = skattLinje.ekstra.first()
            ekstraInfo.shouldBeInstanceOf<TekstEkstraInfo>()
            ekstraInfo.grunn shouldBe EkstraInfo.Reasons.SKATT_TABELL_HOVEDARBEIDSGIVER
        }

        test("beregnSkatteTrekk velger reskontrokonto fra skattekortets tilleggsopplysninger") {
            listOf(
                emptySet<Tilleggsopplysning>() to ("FSKTSKAT" to "0510000"),
                setOf(Tilleggsopplysning.OPPHOLD_I_TILTAKSSONE) to ("FSKTSKAT" to "0510000"),
                setOf(Tilleggsopplysning.KILDESKATT_PENSJONIST) to ("KSKTSKAT" to "0512435"),
                setOf(Tilleggsopplysning.KILDESKATT_PAA_LOENN) to ("KSKTSKAT" to "0512435"),
                setOf(Tilleggsopplysning.OPPHOLD_PAA_SVALBARD) to ("SSKTSKAT" to "0512100"),
            ).forEach { (tilleggsopplysning, forventet) ->
                val skattelinjer = beregnSkattelinjerMedTilleggsopplysning(tilleggsopplysning)
                assertSkattelinjerHarKonto(skattelinjer, forventet.first, forventet.second)
            }
        }

        test("beregnSkatteTrekk skal bruke tilleggsopplysning til konto når skattekortet mangler relevant forskuddstrekk") {
            val skattelinjer = beregnSkattelinjerMedTilleggsopplysning(setOf(Tilleggsopplysning.OPPHOLD_PAA_SVALBARD), forskuddsTrekk = emptyList())

            assertSkattelinjerHarKonto(skattelinjer, "SSKTSKAT", "0512100")
        }

        test("beregnSkatteTrekk skal velge konto uavhengig per kontekst i samme Skattemotor") {
            val delt = Skattemotor()

            val svalbard = beregnSkattelinjerMedTilleggsopplysning(setOf(Tilleggsopplysning.OPPHOLD_PAA_SVALBARD), skattemotorInstans = delt)
            val ordinaer = beregnSkattelinjerMedTilleggsopplysning(emptySet(), skattemotorInstans = delt)

            assertSkattelinjerHarKonto(svalbard, "SSKTSKAT", "0512100")
            assertSkattelinjerHarKonto(ordinaer, "FSKTSKAT", "0510000")
        }

        test("beregnSkatteTrekk med tabellkort hovedarbeidsgiver legger til partiinformasjon for verdiberegnet dagberegning") {
            val periode = DatoIntervall("2026-03-02", "2026-03-13")
            val grunnOppdrag = oppdragMedId()
            val grunnLinje = grunnOppdrag.alleOppdragslinjer.first()

            val beregningsOppdrag =
                listOf(
                    grunnOppdrag.medOppdragslinjer(
                        listOf(
                            grunnLinje.copy(
                                linjeId = 1,
                                delytelseId = "delytelse-1",
                                sats = BigDecimal("900"),
                                vedtakssats = BigDecimal("1200"),
                                datoVedtakFom = LocalDate.of(2026, 3, 2),
                                datoVedtakTom = LocalDate.of(2026, 3, 6),
                            ),
                            grunnLinje.copy(
                                linjeId = 2,
                                delytelseId = "delytelse-2",
                                sats = BigDecimal("1100"),
                                vedtakssats = BigDecimal("1500"),
                                datoVedtakFom = LocalDate.of(2026, 3, 9),
                                datoVedtakTom = LocalDate.of(2026, 3, 13),
                            ),
                        ),
                    ),
                )

            val skattekort =
                Skattekort(
                    aktoerId = AktoerId(1),
                    inntektsaar = 2026,
                    kilde = "TEST",
                    identifikator = 1,
                    utstedtDato = LocalDate.of(2026, 1, 1),
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

            val kontekst =
                fordeler.byggBeregningsmotorKontekst(
                    periode = periode,
                    oppdrag = beregningsOppdrag,
                    trekk = emptyList(),
                    input = testFordelingInput(),
                    skatteKontekst =
                        SkatteKontekst(
                            skattekort = skattekort,
                            allokeringer = mapOf(ForskuddstrekkKode.LOENN_FRA_NAV to Faggruppe.ARBYT),
                        ),
                    relaterteBeregninger = emptyList(),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )
            val medYtelse = ytelsesmotor.beregnYtelse(kontekst)
            val result = skattemotor.beregnSkatteTrekk(medYtelse)

            val scratchpad = result.segmenter.first().scratchpad[OppdragId(1)]!!
            val partiinformasjon = scratchpad.ekstraInfo.filterIsInstance<TekstEkstraInfo>()

            partiinformasjon shouldHaveSize 2
            partiinformasjon.map { it.verdier["vedtakssats"]!!.verdi }.toSet() shouldBe setOf("1200", "1500")
            partiinformasjon.map { it.verdier["bruttoYtelse"]!!.verdi }.toSet() shouldBe setOf("4500", "5500")
        }

        test("beregnSkatteTrekk med tabellkort fordeler korrekt mellom etterbetaling og tabelltrekk perioder") {
            val etterbetalingPeriode = DatoIntervall("2026-03-02", "2026-03-06")
            val normalPeriode = DatoIntervall("2026-03-09", "2026-03-13")

            val relatertBeregning =
                Beregning(
                    periode = etterbetalingPeriode,
                    konto = "",
                    overfoeres = LocalDate.of(2026, 3, 1),
                    fagomraade = FagomraadeKode.AAP,
                    gjelder = AktoerId(1),
                    beregnet = LocalDateTime.of(2026, 3, 1, 12, 0),
                    delberegning =
                        listOf(
                            Delberegning(
                                oppdragId = OppdragId(1),
                                linjer =
                                    listOf(
                                        Delberegningslinje(
                                            type = DelberegningType.YTELSE,
                                            dagsbeloep = emptyList(),
                                            beloep = BigDecimal("4870"),
                                            kodeKlasse =
                                                LinjeKlassekode.Ytelse(
                                                    YtelseKlasseKode(
                                                        kode = YtelseKlasseKodeRef("AAPUAA"),
                                                        beskrivelse = "Test",
                                                        grunnlagSkatt = false,
                                                        grunnlagTrekk = false,
                                                        fagomraader = emptyList(),
                                                    ),
                                                ),
                                        ),
                                    ),
                                datoIntervall = etterbetalingPeriode,
                            ),
                        ),
                )

            val beregningsPeriode = DatoIntervall("2026-03-02", "2026-03-13")
            val beregningsOppdrag = listOf(oppdragMedId())
            val beregningsTrekk = emptyList<Trekkvedtak>()
            val input = testFordelingInput()

            val prosentsats = BigDecimal("30")
            val tabellNummer = "8010"
            val skattekort =
                Skattekort(
                    aktoerId = AktoerId(1),
                    inntektsaar = 2026,
                    kilde = "TEST",
                    identifikator = 1,
                    utstedtDato = LocalDate.of(2026, 1, 1),
                    forskuddsTrekk =
                        listOf(
                            Forskuddstrekk(
                                typeForskuddsTrekk = ForskuddstrekkType.TABELLKORT,
                                trekkKode = ForskuddstrekkKode.LOENN_FRA_NAV,
                                trekktabell = tabellNummer,
                                prosentsats = prosentsats,
                            ),
                        ),
                )

            val kontekst =
                fordeler.byggBeregningsmotorKontekst(
                    periode = beregningsPeriode,
                    oppdrag = beregningsOppdrag,
                    trekk = beregningsTrekk,
                    input = input,
                    skatteKontekst =
                        SkatteKontekst(
                            skattekort = skattekort,
                            allokeringer = mapOf(ForskuddstrekkKode.LOENN_FRA_NAV to Faggruppe.ARBYT),
                        ),
                    relaterteBeregninger = listOf(relatertBeregning),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )
            val medYtelse = ytelsesmotor.beregnYtelse(kontekst)

            val result = skattemotor.beregnSkatteTrekk(medYtelse)

            val segment = result.segmenter.first()
            val scratchpad = segment.scratchpad[OppdragId(1)]!!

            // Find skatt lines per period, grouped by reason
            val skattPerPeriode =
                scratchpad.beregninger
                    .filter { (_, linjer) -> linjer.any { it.type == DelberegningType.SKATT } }
                    .mapValues { (_, linjer) -> linjer.filter { it.type == DelberegningType.SKATT } }

            val etterbetalingEntry =
                skattPerPeriode.entries.first { (_, linjer) ->
                    linjer.any { linje ->
                        linje.ekstra.any { it is TekstEkstraInfo && it.grunn == EkstraInfo.Reasons.SKATT_ETTERBETALING }
                    }
                }
            val tabellEntry =
                skattPerPeriode.entries.first { (_, linjer) ->
                    linjer.any { linje ->
                        linje.ekstra.any { it is TekstEkstraInfo && it.grunn == EkstraInfo.Reasons.SKATT_TABELL_HOVEDARBEIDSGIVER }
                    }
                }

            // Etterbetaling period should have prosent-skatt
            val etterbetalingSkattLinje = etterbetalingEntry.value.first()
            etterbetalingSkattLinje.satsType shouldBe Satstype.PROSENT
            etterbetalingSkattLinje.sats shouldBe prosentsats
            etterbetalingSkattLinje.beloep shouldBe BigDecimal.ZERO

            // Tabelltrekk period should use TABELL satstype
            val tabellSkattLinje = tabellEntry.value.first()
            tabellSkattLinje.satsType shouldBe Satstype.TABELL

            // Verify tabelltrekk used correct aggregated values via ekstraInfo
            val tabellEkstraInfo = tabellSkattLinje.ekstra.first() as TekstEkstraInfo
            tabellEkstraInfo.grunn shouldBe EkstraInfo.Reasons.SKATT_TABELL_HOVEDARBEIDSGIVER

            // Verify the tabelltrekk was calculated with correct values from ekstraInfo
            val trekkgrunnlag = BigDecimal(tabellEkstraInfo.verdier["trekkgrunnlag"]!!.verdi)
            val dager = tabellEkstraInfo.verdier["dager"]!!.verdi.toInt()
            val totalSkatt = BigDecimal(tabellEkstraInfo.verdier["totalSkatt"]!!.verdi)

            // Verify the tabelltrekk was calculated with those exact values
            val forventetBeregning = SkatteTabell.beregnTabellTrekk(tabellNummer, dager, trekkgrunnlag)
            totalSkatt shouldBe forventetBeregning.trekkbeloep
            tabellSkattLinje.beloep shouldBe -totalSkatt
        }
    })
