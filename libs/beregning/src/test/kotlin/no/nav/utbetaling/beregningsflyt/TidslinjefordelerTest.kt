package no.nav.utbetaling.beregningsflyt

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

import no.nav.sokos.dare.poc.aktoer.AktoerId
import no.nav.sokos.dare.poc.beregning.Beregning
import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.beregning.Delberegning
import no.nav.sokos.dare.poc.beregning.DelberegningType
import no.nav.sokos.dare.poc.beregning.Delberegningslinje
import no.nav.sokos.dare.poc.beregning.EkstraInfo
import no.nav.sokos.dare.poc.beregning.TekstEkstraInfo
import no.nav.sokos.dare.poc.beregning.dataklasser.BeregningsStatus
import no.nav.sokos.dare.poc.beregning.dataklasser.BeregningsStatusflagg
import no.nav.sokos.dare.poc.beregning.fordeler.FordelingInput
import no.nav.sokos.dare.poc.beregning.fordeler.SatsBoetteId
import no.nav.sokos.dare.poc.beregning.fordeler.SkatteKontekst
import no.nav.sokos.dare.poc.oppdrag.OppdragId
import no.nav.sokos.dare.poc.oppdrag.OppdragslinjekjedeId
import no.nav.sokos.dare.poc.oppdrag.YtelseKlasseKode
import no.nav.sokos.dare.poc.oppdrag.YtelseKlasseKodeRef
import no.nav.sokos.dare.poc.trekk.FagomraadeKode
import no.nav.sokos.dare.poc.trekk.SatsType
import no.nav.sokos.dare.poc.trekk.TrekkFagomraade
import no.nav.sokos.dare.poc.trekk.TrekkStatus
import no.nav.sokos.dare.poc.trekk.Trekkberegningstype
import no.nav.sokos.dare.poc.trekk.Trekkperiode
import no.nav.sokos.dare.poc.trekk.TrekkperiodeId
import no.nav.sokos.dare.poc.trekk.Trekktype
import no.nav.sokos.dare.poc.trekk.Trekkvedtak
import no.nav.sokos.dare.poc.trekk.TrekkvedtakId
import no.nav.utbetaling.beregning.oppdrag.Avventingsårsak
import no.nav.utbetaling.beregning.oppdrag.Oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.Opplysning
import no.nav.utbetaling.beregning.oppdrag.Opplysninger
import no.nav.utbetaling.beregning.oppdrag.Part
import no.nav.utbetaling.beregning.oppdrag.Personidentifikator
import no.nav.utbetaling.beregning.oppdrag.SaksbehandlerId
import no.nav.utbetaling.beregning.oppdrag.Status
import no.nav.utbetaling.beregning.oppdrag.UtbetalesTil
import no.nav.utbetaling.beregning.oppdrag.VedtakId
import no.nav.utbetaling.beregning.tid.DefaultTidslinjesamling
import no.nav.utbetaling.beregning.tid.Periode.LukketPeriode
import no.nav.utbetaling.beregning.tid.Tidslinje
import no.nav.utbetaling.beregningsflyt.fordeler.BeregningsFordeler
import no.nav.utbetaling.beregningsflyt.fordeler.avventSegmentering
import no.nav.utbetaling.beregningsflyt.fordeler.delperioder
import no.nav.utbetaling.beregningsflyt.fordeler.grupperTidslinjer
import no.nav.utbetaling.beregningsflyt.identitet.Beregningsidentitet
import no.nav.utbetaling.beregningsflyt.identitet.Klassekodeoppslag
import no.nav.utbetaling.beregningsflyt.identitet.Oppdragsnokkel
import no.nav.utbetaling.beregningsflyt.kontekst.BeregningsSegment

class TidslinjefordelerTest :
    FunSpec({
        test("input recipient metadata stays nullable independently of effective source recipient grouping") {
            val kilde = lesTidslinje(json(linjer = linje("1", felt = ""","utbetalesTilId":"11111111111"""")))
            listOf(null, AktoerId(42)).forEach { mottaker ->
                val kontekst =
                    BeregningsFordeler(fordelerIdentitet, fordelerKlassekoder).byggBeregningsmotorKontekst(
                        fordelerInput.copy(utbetalesTil = mottaker),
                        mars,
                        kilde,
                        emptyList(),
                        SkatteKontekst(null, emptyMap()),
                        emptyList(),
                    )
                kontekst.segmenter
                    .single()
                    .linjeGroupKey
                    ?.utbetalesTilId shouldBe AktoerId(42)
                kontekst.segmenter
                    .single()
                    .delperioder
                    .single()
                    .utbetalesTil shouldBe mottaker
                kontekst.beregninger().single().utbetalesTil shouldBe mottaker
            }
        }

        test("partial avvent overlap retains every benefit and clips both bounds") {
            val kilde = lesTidslinje(json(avvent = avvent("AVAV", "2026-03-10", "2026-03-20", "2026-04-01")))
            val kontekst = fordel(kilde, periode("2026-03-05", "2026-03-25"))
            kontekst.segmenter.map { it.delperioder.map { dp -> dp.periode } } shouldBe
                listOf(listOf(periode("2026-03-05", "2026-03-09")), listOf(periode("2026-03-10", "2026-03-20")), listOf(periode("2026-03-21", "2026-03-25")))
            kontekst.segmenter.map { it.avventStatus } shouldBe listOf(null, BeregningsStatus.AVVENT, null)
            kontekst.segmenter[1].beregningsStatusflagg shouldBe setOf(BeregningsStatusflagg.MANUELL)
            kontekst.segmenter[1].avventOverfoeres shouldBe null
            kontekst.segmenter.flatMap { it.delperioder }.forEach { it.oppdrag.tidslinjer.keys shouldBe kilde.tidslinjer.keys }
            kontekst.segmenter[1]
                .delperioder
                .single()
                .forDag(LocalDate.parse("2026-03-10")) shouldBe BigDecimal("500")
            kontekst.beregninger()[1].overfoeres shouldBe null
        }

        test("equal assignment avvent repeated on two chains is deduplicated per interval") {
            val kilde = lesTidslinje(json(linjer = "${linje("1")},${linje("2")}", avvent = avvent("AVRK", "2026-03-10", "2026-03-20", "2026-04-02")))
            val kontekst = fordel(kilde)
            kontekst.segmenter.map { it.delperioder.single().periode } shouldBe
                listOf(periode("2026-03-01", "2026-03-09"), periode("2026-03-10", "2026-03-20"), periode("2026-03-21", "2026-03-31"))
            kontekst.segmenter[1].beregningsStatusflagg shouldBe setOf(BeregningsStatusflagg.DATOSTYRT)
            kontekst.segmenter[1].avventOverfoeres shouldBe LocalDate.parse("2026-04-02")
            kontekst.segmenter[1]
                .ekstraInfo
                .filterIsInstance<TekstEkstraInfo>()
                .count { it.grunn == EkstraInfo.Reasons.AVVENT } shouldBe 1
            kontekst.segmenter.forEach {
                it.delperioder
                    .single()
                    .oppdrag.tidslinjer.keys shouldBe kilde.tidslinjer.keys
            }
        }

        test("AVRK without transfer date remains date-controlled") {
            val kontekst = fordel(lesTidslinje(json(avvent = avvent("AVRK", "2026-03-01", "2026-03-31"))))
            kontekst.segmenter.single().beregningsStatusflagg shouldBe setOf(BeregningsStatusflagg.DATOSTYRT)
            kontekst.beregninger().single().overfoeres shouldBe null
        }

        test("conflicting simultaneous avvent on chains fails with logical key and interval") {
            val kilde = lesTidslinje(json(linjer = "${linje("1")},${linje("2")}", avvent = avvent("AVAV", "2026-03-10", "2026-03-20")))
            val andre = kilde.tidslinjer.keys.last()
            val konflikt =
                DefaultTidslinjesamling(
                    kilde.tidslinjer.mapValues { (id, tidslinje) ->
                        if (id != andre) {
                            tidslinje
                        } else {
                            tidslinje.mapVerdier {
                                it.copy(avventingsårsak = it.avventingsårsak?.let { verdi -> Opplysning(verdi.saksbehandlerId, Avventingsårsak.Refusjonskrav(null)) })
                            }
                        }
                    },
                )
            val feil = shouldThrow<IllegalArgumentException> { avventSegmentering(konflikt, mars) }
            feil.message.orEmpty() shouldContain "fagsystemId"
            feil.message.orEmpty() shouldContain "2026-03-10"
        }

        test("recipient and KID changes inside a source line are grouped by applicable values without replacing UUIDs") {
            val kilde =
                lesTidslinje(
                    json(
                        linjer =
                            linje(
                                "1",
                                felt = """,
                                "utbetalesTilId":"22222222222",
                                "datoUtbetalesTilIdFom":"2026-03-10",
                                "kid":"new-kid",
                                "datoKidFom":"2026-03-20"
                                """,
                            ),
                    ),
                )
            val kontekst = fordel(kilde)
            kontekst.segmenter.map { it.delperioder.single().periode } shouldBe
                listOf(periode("2026-03-01", "2026-03-09"), periode("2026-03-10", "2026-03-19"), periode("2026-03-20", "2026-03-31"))
            kontekst.segmenter.map { it.linjeGroupKey?.utbetalesTilId } shouldBe listOf(AktoerId(42), AktoerId(43), AktoerId(43))
            kontekst.segmenter.map { it.linjeGroupKey?.kid } shouldBe listOf(null, null, "new-kid")
            kontekst.segmenter.forEach {
                it.delperioder
                    .single()
                    .oppdrag.tidslinjer.keys shouldBe kilde.tidslinjer.keys
            }
            kontekst.segmenter.drop(1).forEach {
                it.ekstraInfo.filterIsInstance<TekstEkstraInfo>().any { info -> info.grunn == EkstraInfo.Reasons.OVERSTYRT_MOTTAKER } shouldBe true
            }
        }

        test("gaps are omitted and chain encounter order is preserved rather than sorted by UUID") {
            val kilde = lesTidslinje(json(linjer = "${linje("9", "2026-03-01", "2026-03-05")},${linje("2", "2026-03-20", "2026-03-31")}"))
            val kontekst = fordel(kilde, periode("2026-03-03", "2026-03-25"))
            kontekst.segmenter
                .single()
                .delperioder
                .map { it.periode } shouldBe listOf(periode("2026-03-03", "2026-03-05"), periode("2026-03-20", "2026-03-25"))
            kontekst.segmenter
                .single()
                .delperioder
                .map { it.oppdrag.tidslinjer.keys } shouldBe
                kilde.tidslinjer.keys.map { setOf(it) }
            val samtidig = lesTidslinje(json(linjer = "${linje("9")},${linje("2")}"))
            fordel(samtidig)
                .segmenter
                .single()
                .delperioder
                .single()
                .oppdrag.tidslinjer.keys
                .toList() shouldBe samtidig.tidslinjer.keys.toList()
        }

        test("open source end is clipped to a finite calculation interval without changing source") {
            val kilde = lesTidslinje(json(linjer = linje("1", tom = null)))
            fordel(kilde)
                .segmenter
                .single()
                .delperioder
                .single()
                .periode shouldBe mars
            kilde[LocalDate.parse("2027-03-01")].keys shouldBe kilde.tidslinjer.keys
            shouldThrow<IllegalArgumentException> { fordel(kilde, DatoIntervall("2026-03-01")) }
        }

        test("provenance and unit-only changes do not create calculation boundaries") {
            val kilde =
                lesTidslinje(
                    json(
                        oppdragsfelt = """,
                        "oppdragsEnhet120":[
                          {"typeEnhet":"BOS","enhet":"1234","datoEnhetFom":"2026-03-01"},
                          {"typeEnhet":"BOS","enhet":"5678","datoEnhetFom":"2026-03-15"}
                        ]
                        """,
                    ),
                )
            kilde.tidslinjer.values
                .single()
                .count() shouldBe 2
            fordel(kilde)
                .segmenter
                .single()
                .delperioder
                .map { it.periode } shouldBe listOf(mars)
        }

        test("dated debtor refund and application fields survive distribution without changing calculation policy") {
            val kilde =
                lesTidslinje(
                    json(
                        linjer =
                            linje(
                                "1",
                                felt = ""","skyldnerId":"12345678901","datoSkyldnerFom":"2026-03-10","typeSoknad":"NY",
                                "refusjonsinfo156":{"refunderesId":"12345678902","maksDato":"2026-03-31","datoFom":"2026-03-20"},
                                "vedtakssats157":{"vedtakssats":800}""",
                            ),
                    ),
                )
            val dp = fordel(kilde).segmenter.single().delperioder.single()
            dp.periode shouldBe mars
            listOf("2026-03-05", "2026-03-12", "2026-03-26").forEach { dato ->
                val dag = LocalDate.parse(dato)
                dp.oppdrag[dag] shouldBe kilde[dag]
                dp.forDag(dag) shouldBe BigDecimal("500")
                dp.vedtakSatsForDag(dag) shouldBe BigDecimal("800")
            }
            dp.oppdrag[LocalDate.parse("2026-03-05")].values.single().skyldner shouldBe null
            dp.oppdrag[LocalDate.parse("2026-03-12")].values.single().skyldner?.opplysning shouldBe
                Part.Person(Personidentifikator("12345678901"))
            dp.oppdrag[LocalDate.parse("2026-03-12")].values.single().refusjonsinfo shouldBe null
            dp.oppdrag[LocalDate.parse("2026-03-26")].values.single().refusjonsinfo?.opplysning?.refunderes shouldBe
                Part.Person(Personidentifikator("12345678902"))
        }

        test("related payments split only matching assignment benefits and are filtered by scalar identity") {
            val kilde = lesTidslinje(json())
            val betalt = tidligere(periode("2026-03-10", "2026-03-15"))
            val uvedkommende = tidligere(periode("2026-03-22", "2026-03-24"), OppdragId(999))
            val dp = fordel(kilde, relaterte = listOf(betalt, uvedkommende)).segmenter.single().delperioder
            dp.map { it.periode } shouldBe listOf(periode("2026-03-01", "2026-03-09"), periode("2026-03-10", "2026-03-15"), periode("2026-03-16", "2026-03-31"))
            dp.map { it.relaterteBeregninger } shouldBe listOf(emptyList(), listOf(betalt), emptyList())
        }

        test("logical assignment keys never collapse into unsupported multi-assignment result segments") {
            val kilde = lesTidslinje("[${json(system = "first")},${json(system = "second")}]")
            val kontekst = fordel(kilde)
            kontekst.segmenter.map {
                it.delperioder
                    .single()
                    .oppdragsnokkel.fagsystemId.id
            } shouldBe listOf("first", "second")
            kontekst.beregninger().size shouldBe 2
            kontekst.segmenter.forEach { segment ->
                segment.delperioder
                    .map { it.oppdragsnokkel }
                    .distinct()
                    .size shouldBe 1
            }
            shouldThrow<IllegalArgumentException> {
                BeregningsSegment(delperioder = kontekst.segmenter.flatMap { it.delperioder })
            }
        }

        test("grouping helper preserves a source chain across periodized KIDs and excludes out-of-scope values") {
            val kilde = lesTidslinje(json(linjer = linje("1", felt = ""","kid":"kid","datoKidFom":"2026-03-15"""")))
            val grupper = grupperTidslinjer(kilde, periode("2026-03-10", "2026-03-20"), fordelerIdentitet)
            grupper.map { it.linjeGroupKey.kid } shouldBe listOf(null, "kid")
            grupper.forEach { it.oppdrag.tidslinjer.keys shouldBe kilde.tidslinjer.keys }
            grupper[0].oppdrag[LocalDate.parse("2026-03-15")] shouldBe emptyMap()
            grupper[1].oppdrag[LocalDate.parse("2026-03-21")] shouldBe emptyMap()
        }

        test("monthly benefit-period deductions split at month edges with concrete bucket keys") {
            val kilde = lesTidslinje(json(linjer = linje("1", "2026-02-01", "2026-04-30")))
            val trekk = trekk(11, SatsType.M, Trekkberegningstype.YTELPER, periode("2026-02-01", "2026-04-30"))
            val kontekst = fordel(kilde, periode("2026-03-25", "2026-04-05"), trekk = listOf(trekk))
            val dp = kontekst.segmenter.single().delperioder
            dp.map { it.periode } shouldBe listOf(periode("2026-03-25", "2026-03-31"), periode("2026-04-01", "2026-04-05"))
            dp.map {
                it.trekk
                    .single()
                    .trekkperiode.boetter
            } shouldBe listOf(listOf("2026-03"), listOf("2026-04"))
            kontekst.satsBoetter.satser shouldBe mapOf(SatsBoetteId(TrekkperiodeId(11), "2026-03") to BigDecimal("100"), SatsBoetteId(TrekkperiodeId(11), "2026-04") to BigDecimal("100"))
        }

        test("transfer-date deductions use transfer month or day even when benefit period does not overlap") {
            val kilde = lesTidslinje(json())
            val maaned = trekk(11, SatsType.M, Trekkberegningstype.UTBDATO, periode("2026-04-15", "2026-04-20"), BigDecimal("200"))
            val daglig = trekk(12, SatsType.D, Trekkberegningstype.UTBDATO, periode("2026-04-15", "2026-04-20"))
            val prosent = trekk(13, SatsType.P, Trekkberegningstype.YTELPER, mars)
            val uvedkommende = trekk(14, SatsType.M, Trekkberegningstype.YTELPER, periode("2026-03-11", "2026-03-12"), fagomraade = FagomraadeKode.DP)
            val historikk =
                tidligere(periode("2026-02-01", "2026-02-10")).copy(
                    delberegning =
                        listOf(
                            Delberegning(
                                oppdragId = OppdragId(7),
                                datoIntervall = periode("2026-02-01", "2026-02-10"),
                                linjer = listOf(Delberegningslinje(type = DelberegningType.TREKK, dagsbeloep = emptyList(), beloep = BigDecimal("-30"), trekkvedtakId = TrekkvedtakId(11))),
                            ),
                        ),
                )
            val kontekst = fordel(kilde, relaterte = listOf(historikk), trekk = listOf(maaned, daglig, prosent, uvedkommende))
            val dp =
                kontekst.segmenter
                    .single()
                    .delperioder
                    .single()
            dp.periode shouldBe mars
            dp.trekk.map { it.id } shouldBe listOf(TrekkvedtakId(11), TrekkvedtakId(12), TrekkvedtakId(13))
            dp.trekk.map { it.trekkperiode.boetter } shouldBe listOf(listOf("2026-04"), listOf("2026-04-15"), emptyList())
            dp.tidligereTrukneTrekkvedtakIMaaneden shouldBe setOf(TrekkvedtakId(11))
            kontekst.satsBoetter.satser shouldBe mapOf(SatsBoetteId(TrekkperiodeId(11), "2026-04") to BigDecimal("70"), SatsBoetteId(TrekkperiodeId(12), "2026-04-15") to BigDecimal("100"))
            kontekst.saldoBoetter.saldoer shouldBe mapOf(TrekkvedtakId(11) to BigDecimal("170"))
        }

        test("daily benefit-period deductions list only weekdays inside selected bounded periods") {
            val trekk = trekk(11, SatsType.D, Trekkberegningstype.YTELPER, periode("2026-03-06", "2026-03-10"))
            val dp = fordel(lesTidslinje(json()), periode("2026-03-05", "2026-03-11"), trekk = listOf(trekk)).segmenter.single().delperioder
            dp.map { it.periode } shouldBe listOf(periode("2026-03-05", "2026-03-05"), periode("2026-03-06", "2026-03-10"), periode("2026-03-11", "2026-03-11"))
            dp.map { it.trekk.flatMap { tv -> tv.trekkperiode.boetter } } shouldBe listOf(emptyList(), listOf("2026-03-06", "2026-03-09", "2026-03-10"), emptyList())
        }

        test("tax and calendar-year boundaries are interval-based") {
            val kilde = lesTidslinje(json(linjer = linje("1", "2025-12-15", "2026-03-31")))
            val dp = fordel(kilde, periode("2025-12-15", "2026-02-15")).segmenter.single().delperioder
            dp.map { it.periode } shouldBe listOf(periode("2025-12-15", "2025-12-31"), periode("2026-01-01", "2026-01-31"), periode("2026-02-01", "2026-02-15"))
        }

        test("grouping ignores provenance-only changes but distributor retains real source line identity boundaries") {
            val opprinnelig = lesTidslinje(json()).tidslinjer.entries.single()
            val verdi = opprinnelig.value.first().second
            val provenance =
                DefaultTidslinjesamling(
                    mapOf(
                        opprinnelig.key to
                            Tidslinje
                                .Builder<Opplysninger>()
                                .periode(LukketPeriode(LocalDate.parse("2026-03-01"), LocalDate.parse("2026-03-14")), verdi)
                                .periode(
                                    LukketPeriode(LocalDate.parse("2026-03-15"), LocalDate.parse("2026-03-31")),
                                    verdi.copy(sats = Opplysning(SaksbehandlerId("Z888888"), verdi.sats.opplysning)),
                                ).build(),
                    ),
                )
            provenance.tidslinjer.values
                .single()
                .count() shouldBe 2
            val gruppe = grupperTidslinjer(provenance, mars, fordelerIdentitet).single()
            delperioder(gruppe.oppdrag, mars, fordelerIdentitet, OppdragId(7), emptyList(), fordelerInput.beregnet, emptyList()) shouldBe listOf(mars)
            val reelleLinjer = lesTidslinje(json(linjer = "${linje("1", tom = "2026-03-15")},${linje("2", fom = "2026-03-16", felt = ""","refDelytelseId":"1"""")}"))
            val dp = fordel(reelleLinjer).segmenter.single().delperioder
            dp.map { it.periode } shouldBe listOf(periode("2026-03-01", "2026-03-15"), periode("2026-03-16", "2026-03-31"))
            dp.map { it.oppdrag.tidslinjer.keys } shouldBe listOf(reelleLinjer.tidslinjer.keys, reelleLinjer.tidslinjer.keys)
        }

        test("implicit and explicit actor recipients share the effective recipient without losing either chain") {
            val kilde = lesTidslinje(json(linjer = "${linje("1")},${linje("2", felt = ""","utbetalesTilId":"11111111111"""")}"))
            val kontekst = fordel(kilde)
            kontekst.segmenter
                .single()
                .linjeGroupKey
                ?.utbetalesTilId shouldBe AktoerId(42)
            kontekst.segmenter
                .single()
                .delperioder
                .single()
                .oppdrag.tidslinjer.keys shouldBe kilde.tidslinjer.keys
            kontekst.segmenter
                .single()
                .delperioder
                .single()
                .forDag(LocalDate.parse("2026-03-02")) shouldBe BigDecimal("1000")
        }

        test("KID transition diagnostics match the legacy non-null previous KID rule") {
            val kilde =
                lesTidslinje(json(linjer = linje("1", felt = ""","kid":"first""""))) +
                    lesTidslinje(json(linjer = linje("1", fom = "2026-03-15", felt = ""","kid":"second"""")))
            val segmenter = fordel(kilde).segmenter
            segmenter.map { it.delperioder.single().periode } shouldBe listOf(periode("2026-03-01", "2026-03-14"), periode("2026-03-15", "2026-03-31"))
            segmenter.map { it.ekstraInfo.filterIsInstance<TekstEkstraInfo>().map { info -> info.grunn } } shouldBe listOf(emptyList(), listOf(EkstraInfo.Reasons.KID_ENDRET))
        }

        test("KID diagnostics follow source group encounter while final intervals are chronological and UUID order is unchanged") {
            val kilde =
                lesTidslinje(
                    json(
                        linjer =
                            listOf(
                                linje("9", fom = "2026-03-20", felt = ""","kid":"first""""),
                                linje("8", tom = "2026-03-19", felt = ""","kid":"second""""),
                                linje("7", fom = "2026-03-20", felt = ""","kid":"first""""),
                                linje("6", tom = "2026-03-19", felt = ""","kid":"second""""),
                            ).joinToString(","),
                    ),
                )
            val ider = kilde.tidslinjer.keys.toList()
            val segmenter = fordel(kilde).segmenter
            segmenter.map { it.delperioder.single().periode } shouldBe listOf(periode("2026-03-01", "2026-03-19"), periode("2026-03-20", "2026-03-31"))
            segmenter.map { it.linjeGroupKey?.kid } shouldBe listOf("second", "first")
            segmenter.map { it.ekstraInfo.filterIsInstance<TekstEkstraInfo>().map { info -> info.grunn } } shouldBe listOf(listOf(EkstraInfo.Reasons.KID_ENDRET), emptyList())
            segmenter.map {
                it.delperioder
                    .single()
                    .oppdrag.tidslinjer.keys
                    .toList()
            } shouldBe listOf(listOf(ider[1], ider[3]), listOf(ider[0], ider[2]))
            kilde.tidslinjer.keys.toList() shouldBe ider
        }

        test("decision ID alone creates a concrete calculation boundary") {
            val kilde = lesTidslinje(json()) + lesTidslinje(json(linjer = linje("1", fom = "2026-03-15", felt = ""","vedtakId":"new-decision"""")))
            val dp = fordel(kilde).segmenter.single().delperioder
            dp.map { it.periode } shouldBe listOf(periode("2026-03-01", "2026-03-14"), periode("2026-03-15", "2026-03-31"))
            dp.map {
                it.oppdrag[it.periode.fom]
                    .values
                    .single()
                    .vedtakId
                    ?.opplysning
            } shouldBe listOf(null, VedtakId("new-decision"))
            dp.forEach { it.oppdrag.tidslinjer.keys shouldBe kilde.tidslinjer.keys }
        }

        test("classification alone creates a concrete calculation boundary") {
            val kilde = lesTidslinje(json()) + lesTidslinje(json(linjer = linje("1", fom = "2026-03-15")).replace("AAPUAA", "AAPNEW"))
            val dp = fordel(kilde).segmenter.single().delperioder
            dp.map { it.periode } shouldBe listOf(periode("2026-03-01", "2026-03-14"), periode("2026-03-15", "2026-03-31"))
            dp.map {
                it.oppdrag[it.periode.fom]
                    .values
                    .single()
                    .ytelseKlassekode.opplysning.kode
            } shouldBe listOf("AAPUAA", "AAPNEW")
            dp.forEach { it.oppdrag.tidslinjer.keys shouldBe kilde.tidslinjer.keys }
        }

        test("status transitions create concrete boundaries and unsupported status in scope rejects distribution") {
            listOf("HVIL" to Status.Hvilende, "SPER" to Status.Sperret, "OPPH" to Status.Opphørt).forEach { (kode, status) ->
                val kilde = lesTidslinje(json(linjer = linje("1", felt = ""","kodeStatusLinje":"$kode","datoStatusFom":"2026-03-15"""")))
                delperioder(kilde, mars, fordelerIdentitet, OppdragId(7), emptyList(), fordelerInput.beregnet, emptyList()) shouldBe
                    listOf(periode("2026-03-01", "2026-03-14"), periode("2026-03-15", "2026-03-31"))
                val feil = shouldThrow<IllegalArgumentException> { fordel(kilde) }
                feil.message.orEmpty() shouldContain "Unsupported source status: $status"
                feil.message.orEmpty() shouldContain "2026-03-15"
                feil.message.orEmpty() shouldContain "fagsystemId"
                fordel(kilde, periode("2026-03-01", "2026-03-14"))
                    .segmenter
                    .single()
                    .delperioder
                    .map { it.periode } shouldBe
                    listOf(periode("2026-03-01", "2026-03-14"))
            }
        }

        test("rate and decision-rate changes retain real boundaries with the same source identity") {
            val kilde =
                lesTidslinje(json()) +
                    lesTidslinje(json(linjer = linje("1", fom = "2026-03-10", felt = ""","vedtakssats157":{"vedtakssats":800}"""))) +
                    lesTidslinje(json(linjer = linje("1", fom = "2026-03-20", felt = ""","vedtakssats157":{"vedtakssats":800}""")).replace("\"sats\":500", "\"sats\":700"))
            val dp = fordel(kilde).segmenter.single().delperioder
            dp.map { it.periode } shouldBe listOf(periode("2026-03-01", "2026-03-09"), periode("2026-03-10", "2026-03-19"), periode("2026-03-20", "2026-03-31"))
            dp.map { it.forDag(it.periode.fom) } shouldBe listOf(BigDecimal.ZERO, BigDecimal("500"), BigDecimal("700"))
            dp.map { it.vedtakSatsForDag(LocalDate.parse("2026-03-10")) } shouldBe listOf(BigDecimal.ZERO, BigDecimal("800"), BigDecimal.ZERO)
            dp.forEach { it.oppdrag.tidslinjer.keys shouldBe kilde.tidslinjer.keys }
        }

        test("grouping retains deliberately reverse-sorted UUID encounter order") {
            val opprinnelig = lesTidslinje(json()).tidslinjer.values.single()
            val forst = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff")
            val sist = UUID.fromString("00000000-0000-0000-0000-000000000001")
            val kilde = DefaultTidslinjesamling(linkedMapOf(forst to opprinnelig, sist to opprinnelig))
            grupperTidslinjer(kilde, mars, fordelerIdentitet)
                .single()
                .oppdrag.tidslinjer.keys
                .toList() shouldBe listOf(forst, sist)
        }
    })

private val mars = periode("2026-03-01", "2026-03-31")
private val fordelerInput = FordelingInput("123", AktoerId(42), LocalDateTime.of(2026, 3, 5, 10, 0), null, BeregningsStatus.BEREGNET, mutableListOf(), LocalDate.parse("2026-04-15"))
private val fordelerIdentitet =
    object : Beregningsidentitet {
        override fun oppdragId(nokkel: Oppdragsnokkel) = OppdragId(if (nokkel.fagsystemId.id == "second") 8 else 7)

        override fun gjelderId(personidentifikator: Personidentifikator): AktoerId {
            require(personidentifikator.identifikator == "11111111111")
            return AktoerId(42)
        }

        override fun mottakerId(utbetalesTil: UtbetalesTil): AktoerId {
            if (utbetalesTil == UtbetalesTil.Person(Personidentifikator("11111111111"))) return AktoerId(42)
            require(utbetalesTil == UtbetalesTil.Person(Personidentifikator("22222222222")))
            return AktoerId(43)
        }

        override fun resultatKjedeId(kjedeId: UUID): OppdragslinjekjedeId = error("No result mapping during distribution")
    }
private val fordelerKlassekoder = Klassekodeoppslag { YtelseKlasseKode(YtelseKlasseKodeRef(it.kode), "test", true, true, listOf(FagomraadeKode.AAP)) }

private fun periode(
    fom: String,
    tom: String,
) = DatoIntervall(fom, tom)

private fun fordel(
    kilde: Oppdragstidslinje,
    periode: DatoIntervall = mars,
    relaterte: List<Beregning> = emptyList(),
    trekk: List<Trekkvedtak> = emptyList(),
) = BeregningsFordeler(fordelerIdentitet, fordelerKlassekoder).byggBeregningsmotorKontekst(fordelerInput, periode, kilde, trekk, SkatteKontekst(null, emptyMap()), relaterte)

private fun trekk(
    id: Long,
    satsType: SatsType,
    beregningstype: Trekkberegningstype,
    periode: DatoIntervall,
    saldo: BigDecimal? = null,
    fagomraade: FagomraadeKode = FagomraadeKode.AAP,
): Trekkvedtak =
    Trekkvedtak(
        id = TrekkvedtakId(id),
        trekktypeKode = "test",
        belopSaldo = saldo,
        ansvarligEnhet = "1234",
        debitorId = 42,
        trekkstatus = TrekkStatus.LOPE,
        bruker = "test",
        tidspktReg = fordelerInput.beregnet,
        trekkperioder = listOf(Trekkperiode(TrekkperiodeId(id), id, periode.fom, periode.tom, BigDecimal("100"), satsType, bruker = "test", tidspktReg = fordelerInput.beregnet)),
        fagomraader = listOf(TrekkFagomraade(trekkvedtakId = id, fagomraadeKode = fagomraade, aktiv = true, tidspktReg = fordelerInput.beregnet)),
        trekktype = Trekktype("test", "test", "10", "test", "test", false, "test", "test", 1000000, "test", fordelerInput.beregnet, beregningstype),
    )

private fun tidligere(
    periode: DatoIntervall,
    id: OppdragId = OppdragId(7),
) = Beregning(
    periode = periode,
    konto = "123",
    overfoeres = fordelerInput.overfoeringsdato,
    fagomraade = FagomraadeKode.AAP,
    gjelder = AktoerId(42),
    beregnet = fordelerInput.beregnet,
    delberegning =
        listOf(
            Delberegning(oppdragId = id, linjer = listOf(Delberegningslinje(type = DelberegningType.YTELSE, dagsbeloep = emptyList(), beloep = BigDecimal("100"))), datoIntervall = periode),
        ),
)

private fun avvent(
    kode: String,
    fom: String,
    tom: String,
    overfores: String? = null,
) = """{"kodeArsak":"$kode","datoAvventFom":"$fom","datoAvventTom":"$tom","datoOverfores":${overfores?.let { "\"$it\"" } ?: "null"}}"""

private fun linje(
    id: String,
    fom: String = "2026-03-01",
    tom: String? = "2026-03-31",
    felt: String = "",
) = """
    {"kodeEndringLinje":"NY","delytelseId":"$id","kodeKlassifik":"AAPUAA",
     "datoVedtakFom":"$fom","datoVedtakTom":${tom?.let { "\"$it\"" } ?: "null"},
     "sats":500,"fradragTillegg":"T","typeSats":"DAG","saksbehId":"Z999999"$felt}
    """.trimIndent()

private fun json(
    linjer: String = linje("1"),
    avvent: String = "",
    system: String = "fagsystemId",
    oppdragsfelt: String = "",
) = """
    {"kodeAksjon":"1","kodeEndring":"NY","kodeFagomraade":"AAP","fagsystemId":"$system",
     "oppdragGjelderId":"11111111111","datoOppdragGjelderFom":"2000-01-01","saksbehId":"Z999999",
     "avvent118":[$avvent],"oppdragsLinje150":[$linjer]$oppdragsfelt}
    """.trimIndent()
