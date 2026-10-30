package no.nav.utbetaling.beregningsflyt

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

import no.nav.sokos.dare.poc.beregning.fordeler.BeregningsmotorKontekst as LegacyKontekst
import no.nav.sokos.dare.poc.aktoer.AktoerId
import no.nav.sokos.dare.poc.beregning.BeregningId
import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.beregning.DelberegningType
import no.nav.sokos.dare.poc.beregning.EkstraInfo
import no.nav.sokos.dare.poc.beregning.TekstEkstraInfo
import no.nav.sokos.dare.poc.beregning.dataklasser.BeregningsStatus
import no.nav.sokos.dare.poc.beregning.dataklasser.Satstype
import no.nav.sokos.dare.poc.beregning.dto.BeregningsPresentasjonV2
import no.nav.sokos.dare.poc.beregning.dto.BeregningsmotorKontekstPresentasjon
import no.nav.sokos.dare.poc.beregning.dto.Capacity
import no.nav.sokos.dare.poc.beregning.dto.DelBeregningsPresentasjonV2
import no.nav.sokos.dare.poc.beregning.dto.Linje
import no.nav.sokos.dare.poc.beregning.dto.SaldoBoettePresentasjon
import no.nav.sokos.dare.poc.beregning.dto.SatsBoettePresentasjon
import no.nav.sokos.dare.poc.beregning.fordeler.Beregningsgrunnlag
import no.nav.sokos.dare.poc.beregning.fordeler.SaldoBoetter
import no.nav.sokos.dare.poc.beregning.fordeler.SatsBoetteId
import no.nav.sokos.dare.poc.beregning.fordeler.SatsBoetter
import no.nav.sokos.dare.poc.beregning.fordeler.SkatteKontekst
import no.nav.sokos.dare.poc.beregning.integrasjon.BeregningsmotorKontekstKomparator
import no.nav.sokos.dare.poc.beregning.integrasjon.IntegrasjonsScenario
import no.nav.sokos.dare.poc.oppdrag.OppdragId
import no.nav.sokos.dare.poc.oppdrag.OppdragslinjekjedeId
import no.nav.sokos.dare.poc.oppdrag.YtelseKlasseKode
import no.nav.sokos.dare.poc.oppdrag.YtelseKlasseKodeRef
import no.nav.sokos.dare.poc.trekk.FagomraadeKode
import no.nav.sokos.dare.poc.trekk.TrekkperiodeId
import no.nav.sokos.dare.poc.trekk.TrekkvedtakId
import no.nav.utbetaling.beregning.oppdrag.Opplysning
import no.nav.utbetaling.beregning.oppdrag.Personidentifikator
import no.nav.utbetaling.beregning.oppdrag.SaksbehandlerId
import no.nav.utbetaling.beregning.oppdrag.UtbetalesTil
import no.nav.utbetaling.beregning.tid.DefaultTidslinjesamling
import no.nav.utbetaling.beregning.tid.Periode.LukketPeriode
import no.nav.utbetaling.beregningsflyt.beregning.Ytelsesmotor
import no.nav.utbetaling.beregningsflyt.identitet.Beregningsidentitet
import no.nav.utbetaling.beregningsflyt.identitet.Klassekodeoppslag
import no.nav.utbetaling.beregningsflyt.identitet.Oppdragsnokkel
import no.nav.utbetaling.beregningsflyt.kontekst.BeregningsSegment
import no.nav.utbetaling.beregningsflyt.kontekst.BeregningsmotorKontekst
import no.nav.utbetaling.beregningsflyt.kontekst.OppdragDelperiode

class ResultatSammenligningTest :
    FunSpec({
        test("timeline context uses the shared presentation constructor and preserves UUID result associations") {
            val (kontekst, kjeder) = presentasjonskontekst()
            val beregninger = kontekst.beregninger()
            val grunnlag: Beregningsgrunnlag = kontekst
            val faktisk = BeregningsmotorKontekstPresentasjon(grunnlag = grunnlag, beregninger = beregninger)
            val forventet =
                resultat().copy(
                    beregninger = beregninger.map(::BeregningsPresentasjonV2),
                    saldoBoetter = listOf(SaldoBoettePresentasjon(8, restSaldo = BigDecimal("80"))),
                    satsBoetter = listOf(SatsBoettePresentasjon(9, "2026-03", restSats = BigDecimal("30"))),
                    initielleSaldoBoetter = listOf(SaldoBoettePresentasjon(8, restSaldo = BigDecimal("180"))),
                    initielleSatsBoetter = listOf(SatsBoettePresentasjon(9, "2026-03", restSats = BigDecimal("90"))),
                )
            BeregningsmotorKontekstKomparator.sammenlign(faktisk, forventet)
            ResultatSammenligning.sammenlign(faktisk, forventet)
            faktisk shouldBe forventet
            BeregningsmotorKontekstPresentasjon(beregninger, grunnlag) shouldBe faktisk
            BeregningsmotorKontekstPresentasjon(emptyList(), grunnlag) shouldBe faktisk.copy(beregninger = emptyList())
            val rader =
                faktisk.beregninger
                    .single()
                    .delberegninger
                    .single()
                    .rows
            rader.map { it.oppdragslinjekjedeId } shouldBe kjeder.values.toList()
            rader.map { it.columnValues.single() } shouldBe listOf(BigDecimal("100"), BigDecimal("40"))
            kjeder.keys.toList() shouldBe
                listOf(
                    UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"),
                    UUID.fromString("00000000-0000-0000-0000-000000000001"),
                )
        }

        test("legacy default null and explicit calculation overloads delegate without changing presentation") {
            val (kontekst, _) = presentasjonskontekst()
            val legacy =
                LegacyKontekst(
                    periode = kontekst.periode,
                    segmenter = emptyList(),
                    skatteKontekst = kontekst.skatteKontekst,
                    satsBoetter = kontekst.satsBoetter,
                    saldoBoetter = kontekst.saldoBoetter,
                    restFrikortBeloep = kontekst.restFrikortBeloep,
                    overføringsdato = null,
                    beregnet = kontekst.beregnet,
                )
            val standard = BeregningsmotorKontekstPresentasjon(grunnlag = legacy, beregninger = legacy.beregninger())
            val tom = standard.copy(beregninger = emptyList())
            val beregninger = kontekst.beregninger()
            beregninger.isNotEmpty() shouldBe true
            val eksplisitt = standard.copy(beregninger = beregninger.map(::BeregningsPresentasjonV2))
            BeregningsmotorKontekstPresentasjon(legacy) shouldBe standard
            BeregningsmotorKontekstPresentasjon(legacy, null) shouldBe standard
            BeregningsmotorKontekstPresentasjon(legacy, emptyList()) shouldBe tom
            BeregningsmotorKontekstPresentasjon(legacy, beregninger) shouldBe eksplisitt
            BeregningsmotorKontekstPresentasjon(kontekst = legacy) shouldBe standard
            BeregningsmotorKontekstPresentasjon(kontekst = legacy, beregninger = null) shouldBe standard
            BeregningsmotorKontekstPresentasjon(kontekst = legacy, beregninger = emptyList()) shouldBe tom
            BeregningsmotorKontekstPresentasjon(kontekst = legacy, beregninger = beregninger) shouldBe eksplisitt
            BeregningsmotorKontekstPresentasjon(beregninger = beregninger, kontekst = legacy) shouldBe eksplisitt
            BeregningsmotorKontekstPresentasjon(grunnlag = kontekst, beregninger = emptyList()).beregninger shouldBe emptyList()
        }

        test("identical presentations pass") {
            ResultatSammenligning.sammenlign(resultat(), resultat())
        }

        val radendringer: Map<String, (Linje) -> Linje> =
            linkedMapOf(
                "benefit class" to { it.copy(kodeKlasse = "AAPNEW") },
                "row label" to { it.copy(rowName = "Wrong benefit") },
                "daily capacity" to { it.copy(capacity = it.capacity!!.copy(dayCapacity = listOf(BigDecimal("101")))) },
                "lump capacity" to { it.copy(capacity = it.capacity!!.copy(lumpCapacity = BigDecimal.ONE)) },
                "missing populated capacity" to { it.copy(capacity = null) },
                "missing populated daily capacity" to { it.copy(capacity = it.capacity!!.copy(dayCapacity = null)) },
                "missing populated lump capacity" to { it.copy(capacity = it.capacity!!.copy(lumpCapacity = null)) },
                "money" to { it.copy(rowValue = BigDecimal.ONE) },
                "daily money" to { it.copy(columnValues = listOf(BigDecimal("101"))) },
                "non-tax account" to { it.copy(reskontroKonto = "wrong") },
                "missing populated chain" to { it.copy(oppdragslinjekjedeId = null) },
            )
        radendringer.forEach { (navn, endring) ->
            test("rejects changed $navn") {
                shouldThrow<AssertionError> {
                    ResultatSammenligning.sammenlign(resultat().endreFoersteRad(endring), resultat())
                }
            }
        }

        val boetteendringer: Map<String, (BeregningsmotorKontekstPresentasjon) -> BeregningsmotorKontekstPresentasjon> =
            linkedMapOf(
                "initial balance" to { it.copy(initielleSaldoBoetter = it.initielleSaldoBoetter.map { b -> b.copy(restSaldo = BigDecimal.ONE) }) },
                "initial balance identity" to { it.copy(initielleSaldoBoetter = it.initielleSaldoBoetter.map { b -> b.copy(trekkvedtakId = 999) }) },
                "initial rate" to { it.copy(initielleSatsBoetter = it.initielleSatsBoetter.map { b -> b.copy(restSats = BigDecimal.ONE) }) },
                "initial rate identity" to { it.copy(initielleSatsBoetter = it.initielleSatsBoetter.map { b -> b.copy(trekkperiodeId = 999) }) },
                "initial rate bucket" to { it.copy(initielleSatsBoetter = it.initielleSatsBoetter.map { b -> b.copy(boette = "wrong") }) },
                "missing initial buckets" to { it.copy(initielleSaldoBoetter = emptyList(), initielleSatsBoetter = emptyList()) },
                "original balance" to { it.copy(saldoBoetter = it.saldoBoetter.map { b -> b.copy(opprinneligSaldo = BigDecimal.ONE) }) },
                "original rate" to { it.copy(satsBoetter = it.satsBoetter.map { b -> b.copy(opprinneligSats = BigDecimal.ONE) }) },
                "initial original balance" to { it.copy(initielleSaldoBoetter = it.initielleSaldoBoetter.map { b -> b.copy(opprinneligSaldo = null) }) },
                "initial original rate" to { it.copy(initielleSatsBoetter = it.initielleSatsBoetter.map { b -> b.copy(opprinneligSats = null) }) },
            )
        boetteendringer.forEach { (navn, endring) ->
            test("rejects changed $navn") {
                shouldThrow<AssertionError> { ResultatSammenligning.sammenlign(endring(resultat()), resultat()) }
            }
        }

        test("rejects changed business assignment identity without normalizing it") {
            shouldThrow<AssertionError> {
                ResultatSammenligning.sammenlign(resultat().endreFoersteDel { it.copy(oppdragId = OppdragId(999)) }, resultat())
            }
        }

        test("rejects changed aggregate row scalar") {
            shouldThrow<AssertionError> {
                ResultatSammenligning.sammenlign(
                    resultat().endreFoersteDel { it.copy(sums = it.sums.copy(rowValue = BigDecimal.ONE)) },
                    resultat(),
                )
            }
        }

        test("rejects changed aggregate daily sum") {
            shouldThrow<AssertionError> {
                ResultatSammenligning.sammenlign(
                    resultat().endreFoersteDel { it.copy(sums = it.sums.copy(columnValues = listOf(BigDecimal.ONE))) },
                    resultat(),
                )
            }
        }

        test("rejects changed row totals") {
            shouldThrow<AssertionError> {
                ResultatSammenligning.sammenlign(resultat().endreFoersteDel { it.copy(sumColumn = listOf(BigDecimal.ONE, BigDecimal("40"))) }, resultat())
            }
        }

        test("rejects changed calculation extra information") {
            shouldThrow<AssertionError> {
                ResultatSammenligning.sammenlign(
                    resultat().copy(beregninger = resultat().beregninger.map { it.copy(ekstraInfo = setOf(TekstEkstraInfo(EkstraInfo.Reasons.STOPPNIVAA))) }),
                    resultat(),
                )
            }
        }

        test("accepts a consistent bijective numeric chain rename across calculations") {
            ResultatSammenligning.sammenlign(resultat().endreKjeder { if (it.id == 91L) 17L else 91L }, resultat())
        }

        test("rejects collapsing distinct chains") {
            shouldThrow<AssertionError> {
                ResultatSammenligning.sammenlign(resultat().endreKjeder { 500L }, resultat())
            }
        }

        test("rejects splitting a repeated chain across calculations") {
            val original = resultat()
            val endret = original.copy(beregninger = listOf(original.beregninger[0], original.beregninger[1].endreKjeder { it.id + 100 }))
            shouldThrow<AssertionError> { ResultatSammenligning.sammenlign(endret, original) }
        }

        test("rejects a changed chain cross reference in a later row") {
            val original = resultat()
            val endret =
                original.endreFoersteDel {
                    it.copy(rows = it.rows + it.rows.first().copy(oppdragslinjekjedeId = OppdragslinjekjedeId(17)))
                }
            val forventet = original.endreFoersteDel { it.copy(rows = it.rows + it.rows.first()) }
            shouldThrow<AssertionError> { ResultatSammenligning.sammenlign(endret, forventet) }
        }

        test("does not reorder rows to hide changed chain or business data") {
            shouldThrow<AssertionError> {
                ResultatSammenligning.sammenlign(resultat().endreFoersteDel { it.copy(rows = it.rows.reversed()) }, resultat())
            }
        }

        test("numeric scale differences pass for all amounts including capacities totals and original buckets") {
            val original = resultat()
            val endret =
                original.copy(
                    beregninger =
                        original.beregninger.map { b ->
                            b.copy(
                                beloepTilUtbetaling = b.beloepTilUtbetaling.setScale(2),
                                delberegninger =
                                    b.delberegninger.map { d ->
                                        d.copy(
                                            rows = d.rows.map { it.medSkala() },
                                            sums = d.sums.medSkala(),
                                            sumColumn = d.sumColumn.map { it.setScale(2) },
                                        )
                                    },
                            )
                        },
                    saldoBoetter = original.saldoBoetter.map { it.copy(restSaldo = it.restSaldo.setScale(2), opprinneligSaldo = it.opprinneligSaldo!!.setScale(2)) },
                    satsBoetter = original.satsBoetter.map { it.copy(restSats = it.restSats.setScale(2), opprinneligSats = it.opprinneligSats!!.setScale(2)) },
                    initielleSaldoBoetter = original.initielleSaldoBoetter.map { it.copy(restSaldo = it.restSaldo.setScale(2), opprinneligSaldo = it.opprinneligSaldo!!.setScale(2)) },
                    initielleSatsBoetter = original.initielleSatsBoetter.map { it.copy(restSats = it.restSats.setScale(2), opprinneligSats = it.opprinneligSats!!.setScale(2)) },
                    restFrikortBeloep = original.restFrikortBeloep!!.setScale(2),
                )
            ResultatSammenligning.sammenlign(endret, original)
        }

        val metadatautelatelser: Map<String, (BeregningsmotorKontekstPresentasjon) -> BeregningsmotorKontekstPresentasjon> =
            linkedMapOf(
                "calculation ID" to { it.copy(beregninger = it.beregninger.map { b -> b.copy(id = null) }) },
                "benefit class" to { it.endreFoersteRad { r -> r.copy(kodeKlasse = null) } },
                "capacity" to { it.endreFoersteRad { r -> r.copy(capacity = null) } },
                "daily capacity" to { it.endreFoersteRad { r -> r.copy(capacity = r.capacity!!.copy(dayCapacity = null)) } },
                "lump capacity" to { it.endreFoersteRad { r -> r.copy(capacity = r.capacity!!.copy(lumpCapacity = null)) } },
                "chain ID" to { it.endreFoersteRad { r -> r.copy(oppdragslinjekjedeId = null) } },
                "current original balance" to { it.copy(saldoBoetter = it.saldoBoetter.map { b -> b.copy(opprinneligSaldo = null) }) },
                "current original rate" to { it.copy(satsBoetter = it.satsBoetter.map { b -> b.copy(opprinneligSats = null) }) },
                "initial original balance" to { it.copy(initielleSaldoBoetter = it.initielleSaldoBoetter.map { b -> b.copy(opprinneligSaldo = null) }) },
                "initial original rate" to { it.copy(initielleSatsBoetter = it.initielleSatsBoetter.map { b -> b.copy(opprinneligSats = null) }) },
            )
        metadatautelatelser.forEach { (navn, utelat) ->
            test("rejects unexpected populated $navn when the current oracle expects null") {
                val faktisk = resultat().let { it.copy(beregninger = it.beregninger.map { b -> b.copy(id = BeregningId(123)) }) }
                val forventet = utelat(faktisk)
                ResultatSammenligning.sammenlign(forventet, forventet)
                shouldThrow<AssertionError> {
                    ResultatSammenligning.sammenlign(faktisk, forventet)
                }
                shouldThrow<AssertionError> {
                    ResultatSammenligning.sammenlign(forventet, faktisk)
                }
            }
        }

        test("all immutable original goldens compare to themselves") {
            IntegrasjonsScenario.alle().forEach {
                val forventet = it.les().forventet
                ResultatSammenligning.sammenlign(forventet, forventet)
            }
        }
    })

private val sammenligningsperiode = DatoIntervall("2026-03-02", "2026-03-02")

private fun presentasjonskontekst(): Pair<BeregningsmotorKontekst, Map<UUID, OppdragslinjekjedeId>> {
    val kjeder =
        linkedMapOf(
            UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff") to OppdragslinjekjedeId(91),
            UUID.fromString("00000000-0000-0000-0000-000000000001") to OppdragslinjekjedeId(17),
        )
    val identitet =
        object : Beregningsidentitet {
            override fun oppdragId(nokkel: Oppdragsnokkel) = OppdragId(7)

            override fun gjelderId(personidentifikator: Personidentifikator) = AktoerId(42)

            override fun mottakerId(utbetalesTil: UtbetalesTil) = AktoerId(42)

            override fun resultatKjedeId(kjedeId: UUID) = kjeder.getValue(kjedeId)
        }
    val kilde =
        DefaultTidslinjesamling(
            kjeder.keys.zip(listOf("100", "40")).associate { (id, sats) ->
                id to
                    tidslinjeForTest(id, LukketPeriode(sammenligningsperiode.fom, sammenligningsperiode.tom!!) to BigDecimal(sats))
                        .tidslinjer
                        .getValue(id)
                        .mapVerdier {
                            it.copy(gjelderId = Opplysning(SaksbehandlerId("Z999999"), Personidentifikator("11111111111")))
                        }
            },
        )
    val dp =
        OppdragDelperiode(
            oppdrag = kilde,
            oppdragsnokkel =
                kilde.tidslinjer.values
                    .first()
                    .first()
                    .second
                    .let { Oppdragsnokkel(it.fagområde, it.fagsystemId) },
            oppdragId = OppdragId(7),
            fagomraade = FagomraadeKode.AAP,
            identitet = identitet,
            klassekoder = Klassekodeoppslag { YtelseKlasseKode(YtelseKlasseKodeRef(it.kode), "test", true, true, listOf(FagomraadeKode.AAP)) },
            periode = sammenligningsperiode,
            konto = "123",
            gjelder = AktoerId(42),
            beregnet = LocalDateTime.of(2026, 3, 10, 10, 0),
            utbetalesTil = AktoerId(42),
            status = BeregningsStatus.BEREGNET,
            trekk = emptyList(),
            overfoeringsdato = null,
            tidligereTrukneTrekkvedtakIMaaneden = emptySet(),
        )
    val kontekst =
        BeregningsmotorKontekst(
            periode = sammenligningsperiode,
            segmenter = listOf(BeregningsSegment(null, listOf(dp))),
            skatteKontekst = SkatteKontekst(null, emptyMap()),
            satsBoetter = SatsBoetter(mapOf(SatsBoetteId(TrekkperiodeId(9), "2026-03") to BigDecimal("90"))),
            saldoBoetter = SaldoBoetter(mapOf(TrekkvedtakId(8) to BigDecimal("180"))),
            restFrikortBeloep = BigDecimal("500"),
            overføringsdato = null,
            beregnet = dp.beregnet,
        )
    kontekst.satsBoetter.satser.replaceAll { _, _ -> BigDecimal("30") }
    kontekst.saldoBoetter.saldoer.replaceAll { _, _ -> BigDecimal("80") }
    Ytelsesmotor(identitet).beregnYtelse(kontekst)
    return kontekst to kjeder
}

private fun resultat(): BeregningsmotorKontekstPresentasjon {
    val rad =
        Linje(
            type = DelberegningType.YTELSE,
            rowName = "Ytelse YtelseKlasseKodeRef(kode=AAPUAA)",
            columnValues = listOf(BigDecimal("100")),
            rowValue = BigDecimal.ZERO,
            ekstra = emptySet(),
            capacity = Capacity(listOf(BigDecimal("100")), BigDecimal.ZERO),
            satstype = Satstype.DAG,
            sats = BigDecimal("100"),
            oppdragslinjekjedeId = OppdragslinjekjedeId(91),
            kodeKlasse = "AAPUAA",
        )
    val del =
        DelBeregningsPresentasjonV2(
            oppdragId = OppdragId(7),
            columns = sammenligningsperiode.dager,
            rows = listOf(rad, rad.copy(columnValues = listOf(BigDecimal("40")), sats = BigDecimal("40"), oppdragslinjekjedeId = OppdragslinjekjedeId(17))),
            datoIntervall = sammenligningsperiode,
            sums = Linje(DelberegningType.SUM, "Sum", listOf(BigDecimal("140")), BigDecimal.ZERO, emptySet()),
            sumColumn = listOf(BigDecimal("100"), BigDecimal("40")),
        )
    val beregning =
        BeregningsPresentasjonV2(
            periode = sammenligningsperiode,
            fagomraade = FagomraadeKode.AAP,
            gjelder = AktoerId(42),
            beregnet = LocalDateTime.of(2026, 3, 10, 10, 0),
            delberegninger = listOf(del),
            beloepTilUtbetaling = BigDecimal("140"),
        )
    return BeregningsmotorKontekstPresentasjon(
        periode = sammenligningsperiode,
        beregninger = listOf(beregning, beregning),
        saldoBoetter = listOf(SaldoBoettePresentasjon(8, BigDecimal("200"), BigDecimal("80"))),
        satsBoetter = listOf(SatsBoettePresentasjon(9, "2026-03", BigDecimal("100"), BigDecimal("30"))),
        initielleSaldoBoetter = listOf(SaldoBoettePresentasjon(8, BigDecimal("200"), BigDecimal("180"))),
        initielleSatsBoetter = listOf(SatsBoettePresentasjon(9, "2026-03", BigDecimal("100"), BigDecimal("90"))),
        restFrikortBeloep = BigDecimal("500"),
    )
}

private fun BeregningsmotorKontekstPresentasjon.endreFoersteDel(endring: (DelBeregningsPresentasjonV2) -> DelBeregningsPresentasjonV2) =
    copy(beregninger = beregninger.mapIndexed { i, b -> if (i == 0) b.copy(delberegninger = b.delberegninger.mapIndexed { j, d -> if (j == 0) endring(d) else d }) else b })

private fun BeregningsmotorKontekstPresentasjon.endreFoersteRad(endring: (Linje) -> Linje) = endreFoersteDel { d -> d.copy(rows = d.rows.mapIndexed { i, r -> if (i == 0) endring(r) else r }) }

private fun BeregningsmotorKontekstPresentasjon.endreKjeder(endring: (OppdragslinjekjedeId) -> Long) = copy(beregninger = beregninger.map { it.endreKjeder(endring) })

private fun BeregningsPresentasjonV2.endreKjeder(endring: (OppdragslinjekjedeId) -> Long) =
    copy(
        delberegninger =
            delberegninger.map { d ->
                d.copy(rows = d.rows.map { r -> r.copy(oppdragslinjekjedeId = r.oppdragslinjekjedeId?.let { OppdragslinjekjedeId(endring(it)) }) })
            },
    )

private fun Linje.medSkala() =
    copy(
        columnValues = columnValues.map { it.setScale(2) },
        rowValue = rowValue.setScale(2),
        sats = sats?.setScale(2),
        capacity = capacity?.let { c -> c.copy(dayCapacity = c.dayCapacity?.map { it.setScale(2) }, lumpCapacity = c.lumpCapacity?.setScale(2)) },
    )
