package no.nav.sokos.dare.poc.beregning.fordeler

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

import no.nav.sokos.dare.poc.TestUtil.oppdragFraFil
import no.nav.sokos.dare.poc.aktoer.AktoerId
import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.oppdrag.Oppdrag
import no.nav.sokos.dare.poc.testFordelingInput
import no.nav.sokos.dare.poc.trekk.FagomraadeKode

class BeregningsgrunnlagTest :
    FunSpec({
        val periode = DatoIntervall("2026-03-06", "2026-03-10")
        val beregnet = LocalDateTime.parse("2026-10-19T12:00:00")

        fun delperiode(oppdrag: Oppdrag = oppdragFraFil("aapApiTest/simple.xml")): OppdragDelperiode {
            val input = testFordelingInput(konto = "konto123", beregnet = beregnet, utbetalesTil = AktoerId(2))
            return OppdragDelperiode(
                oppdrag = oppdrag,
                periode = periode,
                konto = input.konto,
                gjelder = input.gjelder,
                beregnet = input.beregnet,
                utbetalesTil = input.utbetalesTil,
                status = input.status,
                trekk = emptyList(),
                ekstraInfo = input.ekstraInfo,
                overfoeringsdato = input.overfoeringsdato,
                tidligereTrukneTrekkvedtakIMaaneden = emptySet(),
            )
        }

        fun kontekst(segment: BeregningsSegment) =
            BeregningsmotorKontekst(
                periode = periode,
                segmenter = listOf(segment),
                skatteKontekst = SkatteKontekst(skattekort = null, allokeringer = emptyMap()),
                satsBoetter = SatsBoetter(emptyMap()),
                saldoBoetter = SaldoBoetter(emptyMap()),
                overføringsdato = LocalDate.parse("2026-10-20"),
                beregnet = beregnet,
            )

        test("legacy delperiode exposes metadata through the shared contract") {
            val legacy = delperiode()
            val grunnlag: Delperiodegrunnlag = legacy

            grunnlag.oppdragId shouldBe legacy.oppdrag.id
            grunnlag.fagomraade shouldBe FagomraadeKode.AAP
            grunnlag.periode shouldBe legacy.periode
            grunnlag.konto shouldBe legacy.konto
            grunnlag.gjelder shouldBe legacy.gjelder
            grunnlag.beregnet shouldBe legacy.beregnet
            grunnlag.utbetalesTil shouldBe legacy.utbetalesTil
            grunnlag.status shouldBe legacy.status
            grunnlag.trekk shouldBe legacy.trekk
            (grunnlag.ekstraInfo === legacy.ekstraInfo) shouldBe true
            grunnlag.overfoeringsdato shouldBe legacy.overfoeringsdato
            grunnlag.tidligereTrukneTrekkvedtakIMaaneden shouldBe legacy.tidligereTrukneTrekkvedtakIMaaneden
            grunnlag.relaterteBeregninger shouldBe legacy.relaterteBeregninger
        }

        test("shared daily amounts and deduction base match legacy methods on weekdays weekends and a gap") {
            val original = oppdragFraFil("aapApiTest/simple.xml")
            val linje = original.alleOppdragslinjer.single()
            val oppdrag =
                original.medOppdragslinjer(
                    listOf(
                        linje.copy(datoVedtakFom = periode.fom, datoVedtakTom = periode.fom, vedtakssats = BigDecimal("1200")),
                        linje.copy(
                            linjeId = 2,
                            delytelseId = "etter-gap",
                            datoVedtakFom = periode.tom!!,
                            datoVedtakTom = periode.tom,
                            vedtakssats = BigDecimal("1200"),
                        ),
                    ),
                )
            val grunnlag: Delperiodegrunnlag = delperiode(oppdrag)

            listOf(
                "2026-03-06" to BigDecimal("974"),
                "2026-03-07" to BigDecimal.ZERO,
                "2026-03-08" to BigDecimal.ZERO,
                "2026-03-09" to BigDecimal.ZERO,
                "2026-03-10" to BigDecimal("974"),
            ).forEach { (datoTekst, forventet) ->
                val dato = LocalDate.parse(datoTekst)
                grunnlag.forDag(dato) shouldBe oppdrag.forDag(dato)
                grunnlag.forDag(dato) shouldBe forventet
                grunnlag.vedtakSatsForDag(dato) shouldBe oppdrag.vedtakSatsForDag(dato)
                grunnlag.vedtakSatsForDag(dato) shouldBe if (forventet.signum() == 0) BigDecimal.ZERO else BigDecimal("1200")
                val dag = DatoIntervall(dato, dato)
                grunnlag.trekkgrunnlag(dag) shouldBe oppdrag.trekkgrunnlag(dag)
                grunnlag.trekkgrunnlag(dag) shouldBe forventet
            }
            grunnlag.trekkgrunnlag(periode) shouldBe oppdrag.trekkgrunnlag(periode)
            grunnlag.trekkgrunnlag(periode) shouldBe BigDecimal("1948")
        }

        test("legacy copy retains oppdrag and requires an ID only when reading the contract ID") {
            val legacy = delperiode()
            val utenId = legacy.copy(oppdrag = legacy.oppdrag.copy(id = null))
            val grunnlag: Delperiodegrunnlag = utenId

            utenId.oppdrag.id shouldBe null
            grunnlag.fagomraade shouldBe legacy.oppdrag.fagomraade
            shouldThrow<IllegalArgumentException> { grunnlag.oppdragId }
        }

        test("legacy segment and context expose covariant lists and shared mutable state") {
            val legacyDelperiode = delperiode()
            val legacySegment = BeregningsSegment(delperioder = listOf(legacyDelperiode))
            val legacyKontekst = kontekst(legacySegment)
            val grunnlag: Beregningsgrunnlag = legacyKontekst
            val segment: Segmentgrunnlag = grunnlag.segmenter.single()
            val del: Delperiodegrunnlag = segment.delperioder.single()

            (del === legacyDelperiode) shouldBe true
            segment.linjeGroupKey shouldBe legacySegment.linjeGroupKey
            (segment.ekstraInfo === legacySegment.ekstraInfo) shouldBe true
            (segment.beregningsStatusflagg === legacySegment.beregningsStatusflagg) shouldBe true
            segment.avventStatus shouldBe legacySegment.avventStatus
            segment.avventOverfoeres shouldBe legacySegment.avventOverfoeres
            (segment.scratchpad === legacySegment.scratchpad) shouldBe true
            val scratchpad = BeregningsScratchpad()
            segment.scratchpad[del.oppdragId] = scratchpad
            legacySegment.scratchpad[legacyDelperiode.oppdrag.id] shouldBe scratchpad
            grunnlag.periode shouldBe legacyKontekst.periode
            (grunnlag.skatteKontekst === legacyKontekst.skatteKontekst) shouldBe true
            (grunnlag.satsBoetter === legacyKontekst.satsBoetter) shouldBe true
            (grunnlag.saldoBoetter === legacyKontekst.saldoBoetter) shouldBe true
            grunnlag.beregnet shouldBe legacyKontekst.beregnet
            grunnlag.restFrikortBeloep shouldBe null
            grunnlag.restFrikortBeloep = BigDecimal("1000")
            legacyKontekst.restFrikortBeloep shouldBe BigDecimal("1000")

            legacySegment
                .copy()
                .delperioder
                .single()
                .oppdrag shouldBe legacyDelperiode.oppdrag
            legacyKontekst.copy().beregninger() shouldBe legacyKontekst.beregninger()
        }

        test("shared active date retains transfer date and calculation date fallback") {
            val legacy = kontekst(BeregningsSegment(delperioder = listOf(delperiode())))
            val grunnlag: Beregningsgrunnlag = legacy
            val utenOverfoeringsdato: Beregningsgrunnlag = legacy.copy(overføringsdato = null)

            grunnlag.aktivDato() shouldBe legacy.aktivDato()
            grunnlag.aktivDato() shouldBe LocalDate.parse("2026-10-20")
            utenOverfoeringsdato.aktivDato() shouldBe beregnet.toLocalDate()
        }

        test("default calculation traversal retains legacy segment and subperiod encounter order without sorting") {
            val siste = delperiode().copy(periode = DatoIntervall("2026-03-10", "2026-03-10"))
            val foerste = delperiode().copy(periode = DatoIntervall("2026-03-06", "2026-03-06"))
            val midten = delperiode().copy(periode = DatoIntervall("2026-03-09", "2026-03-09"))
            val a = BeregningsSegment(delperioder = listOf(siste, foerste))
            val b = BeregningsSegment(delperioder = listOf(midten))
            val grunnlag: Beregningsgrunnlag = kontekst(a).copy(segmenter = listOf(a, b))

            val rekkefolge = grunnlag.beregningsrekkefolge()
            rekkefolge shouldBe listOf(a to siste, a to foerste, b to midten)
            (rekkefolge.first().first === a) shouldBe true
            (rekkefolge.first().second === siste) shouldBe true
        }
    })
