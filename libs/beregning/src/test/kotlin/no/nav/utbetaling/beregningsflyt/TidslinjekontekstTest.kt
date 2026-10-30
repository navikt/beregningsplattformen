package no.nav.utbetaling.beregningsflyt

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs

import no.nav.sokos.dare.poc.KlassekodeTestdata
import no.nav.sokos.dare.poc.aktoer.AktoerId
import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.beregning.dataklasser.BeregningsStatus
import no.nav.sokos.dare.poc.beregning.dataklasser.BeregningsStatusflagg
import no.nav.sokos.dare.poc.beregning.fordeler.BeregningsScratchpad
import no.nav.sokos.dare.poc.beregning.fordeler.SaldoBoetter
import no.nav.sokos.dare.poc.beregning.fordeler.SatsBoetteId
import no.nav.sokos.dare.poc.beregning.fordeler.SatsBoetter
import no.nav.sokos.dare.poc.beregning.fordeler.SkatteKontekst
import no.nav.sokos.dare.poc.oppdrag.OppdragId
import no.nav.sokos.dare.poc.oppdrag.OppdragslinjekjedeId
import no.nav.sokos.dare.poc.oppdrag.YtelseKlasseKode
import no.nav.sokos.dare.poc.oppdrag.YtelseKlasseKodeRef
import no.nav.sokos.dare.poc.trekk.FagomraadeKode
import no.nav.sokos.dare.poc.trekk.TrekkperiodeId
import no.nav.sokos.dare.poc.trekk.TrekkvedtakId
import no.nav.utbetaling.beregning.oppdrag.Fagområde
import no.nav.utbetaling.beregning.oppdrag.FagsystemId
import no.nav.utbetaling.beregning.oppdrag.Opplysning
import no.nav.utbetaling.beregning.oppdrag.Opplysninger
import no.nav.utbetaling.beregning.oppdrag.Personidentifikator
import no.nav.utbetaling.beregning.oppdrag.SaksbehandlerId
import no.nav.utbetaling.beregning.oppdrag.Sats
import no.nav.utbetaling.beregning.oppdrag.Status
import no.nav.utbetaling.beregning.oppdrag.UtbetalesTil
import no.nav.utbetaling.beregning.oppdrag.YtelseKlassekode
import no.nav.utbetaling.beregning.oppdrag.YtelseSatstype
import no.nav.utbetaling.beregning.tid.DefaultTidslinjesamling
import no.nav.utbetaling.beregning.tid.Periode.LukketPeriode
import no.nav.utbetaling.beregning.tid.Tidslinje
import no.nav.utbetaling.beregning.tid.Tidslinjesamling
import no.nav.utbetaling.beregningsflyt.identitet.Beregningsidentitet
import no.nav.utbetaling.beregningsflyt.identitet.Klassekodeoppslag
import no.nav.utbetaling.beregningsflyt.identitet.Oppdragsnokkel
import no.nav.utbetaling.beregningsflyt.kontekst.BeregningsSegment
import no.nav.utbetaling.beregningsflyt.kontekst.BeregningsmotorKontekst
import no.nav.utbetaling.beregningsflyt.kontekst.OppdragDelperiode

class TidslinjekontekstTest :
    FunSpec({
        test("weekdays sum independent source chains and decision rates without deduplicating source line IDs") {
            val delperiode = delperiode(kilde = kilde(opplysninger("100", "120"), opplysninger("200", "230")))

            delperiode.forDag(dag(5)) shouldBe BigDecimal("300")
            delperiode.vedtakSatsForDag(dag(5)) shouldBe BigDecimal("350")
            delperiode.trekkgrunnlag(intervall) shouldBe BigDecimal("1500")
        }

        test("weekends and missing decision rates are zero") {
            val delperiode = delperiode()
            delperiode.forDag(dag(10)) shouldBe BigDecimal.ZERO
            delperiode.vedtakSatsForDag(dag(10)) shouldBe BigDecimal.ZERO
            delperiode.vedtakSatsForDag(dag(5)) shouldBe BigDecimal.ZERO
        }

        test("deduction base excludes classifications not marked grunnlagTrekk but daily amount does not") {
            val delperiode =
                delperiode(
                    kilde = kilde(opplysninger("100"), opplysninger("200").copy(ytelseKlassekode = opplysning(YtelseKlassekode("EXCLUDED")))),
                )
            delperiode.forDag(dag(5)) shouldBe BigDecimal("300")
            delperiode.trekkgrunnlag(DatoIntervall(dag(1), dag(31))) shouldBe BigDecimal("500")
        }

        test("view clips both ends and does not change the source") {
            val kilde = kilde(opplysninger("100"))
            val delperiode = delperiode(kilde)
            delperiode.oppdrag[dag(4)] shouldBe emptyMap()
            delperiode.oppdrag[dag(12)] shouldBe emptyMap()
            delperiode.forDag(dag(2)) shouldBe BigDecimal.ZERO
            delperiode.forDag(dag(12)) shouldBe BigDecimal.ZERO
            kilde[dag(2)].size shouldBe 1
            kilde[dag(12)].size shouldBe 1
            shouldThrow<IllegalArgumentException> { delperiode.trekkgrunnlag(DatoIntervall(dag(5))) }
        }

        test("unsupported rate and status fail before execution with key and bounded interval diagnostics") {
            val ugyldigeSatser =
                YtelseSatstype.entries.filter { it != YtelseSatstype.DAG }.map {
                    opplysninger("100").copy(sats = opplysning(Sats(BigDecimal("100"), it.kode)))
                }
            val ugyldigeStatuser =
                listOf(
                    opplysninger("100").copy(status = opplysning(Status.Hvilende)),
                    opplysninger("100").copy(status = opplysning(Status.Sperret)),
                    opplysninger("100").copy(status = opplysning(Status.Opphørt)),
                )
            (ugyldigeSatser + ugyldigeStatuser).forEach { unsupported ->
                val feil = shouldThrow<IllegalArgumentException> { delperiode(kilde(unsupported)) }
                feil.message.orEmpty() shouldContain nokkel.toString()
                feil.message.orEmpty() shouldContain intervall.toString()
            }
        }

        test("unsupported source values outside the bounded calculation scope are irrelevant") {
            val ugyldig = opplysninger("900").copy(sats = opplysning(Sats(BigDecimal("900"), YtelseSatstype.MND.kode)), gjelderId = null)
            val tidslinje =
                Tidslinje
                    .Builder<Opplysninger>()
                    .periode(LukketPeriode(dag(1), dag(4)), ugyldig)
                    .periode(LukketPeriode(dag(5), dag(11)), opplysninger("100"))
                    .periode(LukketPeriode(dag(12), dag(31)), ugyldig)
                    .build()
            val delperiode = delperiode(DefaultTidslinjesamling(mapOf(UUID.randomUUID() to tidslinje)))
            delperiode.forDag(dag(5)) shouldBe BigDecimal("100")
        }

        test("known PENAPET in AAP resolves lookup flags without an additional area restriction") {
            val kode = YtelseKlassekode("PENAPET")
            val klassifisering = KlassekodeTestdata.klassekodeLookup.getValue(YtelseKlasseKodeRef(kode.kode))
            (FagomraadeKode.AAP in klassifisering.fagomraader) shouldBe false
            listOf(klassifisering, klassifisering.copy(grunnlagTrekk = false, grunnlagSkatt = false)).forEach { oppslag ->
                val delperiode =
                    delperiode(
                        kilde = kilde(opplysninger("100").copy(ytelseKlassekode = opplysning(kode))),
                        klassekoder = Klassekodeoppslag { oppslag },
                    )
                delperiode.klassekoder.finn(kode).grunnlagSkatt shouldBe oppslag.grunnlagSkatt
                delperiode.forDag(dag(5)) shouldBe BigDecimal("100")
                delperiode.trekkgrunnlag(intervall) shouldBe if (oppslag.grunnlagTrekk) BigDecimal("500") else BigDecimal.ZERO
            }
        }

        test("unknown or mismatched resolved class codes are rejected eagerly") {
            shouldThrow<IllegalArgumentException> {
                delperiode(kilde(opplysninger("100").copy(ytelseKlassekode = opplysning(YtelseKlassekode("UNKNOWN")))))
            }
            shouldThrow<IllegalArgumentException> {
                delperiode(klassekoder = Klassekodeoppslag { klassekode.copy(kode = YtelseKlasseKodeRef("PENAPET")) })
            }
        }

        test("validation failures retain the logical key and bounded interval") {
            val ugyldige =
                listOf(
                    opplysninger("100").copy(fagsystemId = FagsystemId("other-system")),
                    opplysninger("100").copy(gjelderId = null),
                    opplysninger("100").copy(gjelderId = opplysning(Personidentifikator("other-person"))),
                    opplysninger("100").copy(utbetalesTil = null),
                )
            ugyldige.forEach { verdi ->
                val feil = shouldThrow<IllegalArgumentException> { delperiode(kilde(verdi)) }
                feil.message.orEmpty() shouldContain nokkel.toString()
                feil.message.orEmpty() shouldContain intervall.toString()
            }
            val feil = shouldThrow<IllegalArgumentException> { delperiode(oppdragId = OppdragId(999)) }
            feil.message.orEmpty() shouldContain nokkel.toString()
            feil.message.orEmpty() shouldContain intervall.toString()
        }

        test("missing identity and classification lookups retain context and the original cause") {
            val mangler = NoSuchElementException("Missing lookup entry")
            val oppslag =
                listOf(
                    { delperiode(klassekoder = Klassekodeoppslag { throw mangler }) },
                    {
                        delperiode(
                            identitet =
                                object : Beregningsidentitet by identitet {
                                    override fun oppdragId(nokkel: Oppdragsnokkel): OppdragId = throw mangler
                                },
                        )
                    },
                    {
                        delperiode(
                            identitet =
                                object : Beregningsidentitet by identitet {
                                    override fun gjelderId(personidentifikator: Personidentifikator): AktoerId = throw mangler
                                },
                        )
                    },
                    {
                        delperiode(
                            identitet =
                                object : Beregningsidentitet by identitet {
                                    override fun mottakerId(utbetalesTil: UtbetalesTil): AktoerId = throw mangler
                                },
                        )
                    },
                )
            oppslag.forEach { bygg ->
                val feil = shouldThrow<IllegalArgumentException> { bygg() }
                feil.message.orEmpty() shouldContain nokkel.toString()
                feil.message.orEmpty() shouldContain intervall.toString()
                feil.cause shouldBeSameInstanceAs mangler
            }
        }

        test("unexpected adapter runtime failures propagate unchanged") {
            listOf(RuntimeException("Adapter failure"), IllegalStateException("Adapter bug"), IllegalArgumentException("Adapter bug")).forEach { uventet ->
                val oppslag =
                    listOf(
                        { delperiode(klassekoder = Klassekodeoppslag { throw uventet }) },
                        {
                            delperiode(
                                identitet =
                                    object : Beregningsidentitet by identitet {
                                        override fun oppdragId(nokkel: Oppdragsnokkel): OppdragId = throw uventet
                                    },
                            )
                        },
                        {
                            delperiode(
                                identitet =
                                    object : Beregningsidentitet by identitet {
                                        override fun gjelderId(personidentifikator: Personidentifikator): AktoerId = throw uventet
                                    },
                            )
                        },
                        {
                            delperiode(
                                identitet =
                                    object : Beregningsidentitet by identitet {
                                        override fun mottakerId(utbetalesTil: UtbetalesTil): AktoerId = throw uventet
                                    },
                            )
                        },
                    )
                oppslag.forEach { bygg ->
                    shouldThrow<RuntimeException> { bygg() } shouldBeSameInstanceAs uventet
                }
            }
        }

        test("resolved actors must agree across all values and with declared context without parsing person identifiers") {
            val konflikt = opplysninger("100").copy(gjelderId = opplysning(Personidentifikator("other-person")))
            shouldThrow<IllegalArgumentException> { delperiode(kilde(opplysninger("100"), konflikt)) }
            shouldThrow<IllegalArgumentException> { delperiode(kilde(opplysninger("100").copy(gjelderId = null))) }
            shouldThrow<IllegalArgumentException> {
                delperiode(kilde(opplysninger("100").copy(utbetalesTil = opplysning(UtbetalesTil.Person(Personidentifikator("other-person"))))))
            }
            shouldThrow<IllegalArgumentException> { delperiode(kilde(opplysninger("100").copy(utbetalesTil = null))) }
            delperiode().gjelder shouldBe AktoerId(42)
            delperiode().utbetalesTil shouldBe AktoerId(43)
        }

        test("each timeline value must match the declared logical key and explicit numeric identity") {
            shouldThrow<IllegalArgumentException> {
                delperiode(kilde(opplysninger("100").copy(fagsystemId = FagsystemId("other-system"))))
            }
            shouldThrow<IllegalArgumentException> {
                delperiode(kilde(opplysninger("100").copy(fagområde = Fagområde("DP"))))
            }
            shouldThrow<IllegalArgumentException> { delperiode(oppdragId = OppdragId(999)) }
            shouldThrow<IllegalArgumentException> { delperiode(fagomraade = FagomraadeKode.DP) }
        }

        test("different logical keys sharing a numeric ID cannot produce one output segment") {
            val annenNokkel = nokkel.copy(fagsystemId = FagsystemId("other-system"))
            val andre = delperiode(kilde(opplysninger("100").copy(fagsystemId = annenNokkel.fagsystemId)), annenNokkel)
            shouldThrow<IllegalArgumentException> {
                kontekst(listOf(BeregningsSegment(delperioder = listOf(delperiode(), andre)))).beregninger()
            }
        }

        test("each construction owns fresh mutable state even when initial buckets are reused") {
            val satsId = SatsBoetteId(TrekkperiodeId(1), "2026-01")
            val saldoId = TrekkvedtakId(1)
            val sats = SatsBoetter(mapOf(satsId to BigDecimal("100")))
            val saldo = SaldoBoetter(mapOf(saldoId to BigDecimal("200")))
            val første = kontekst(listOf(BeregningsSegment(delperioder = listOf(delperiode()))), sats, saldo)
            val andre = kontekst(listOf(BeregningsSegment(delperioder = listOf(delperiode()))), sats, saldo)
            første.satsBoetter.satser shouldNotBeSameInstanceAs andre.satsBoetter.satser
            første.saldoBoetter.saldoer shouldNotBeSameInstanceAs andre.saldoBoetter.saldoer
            første.satsBoetter.brukSats(satsId, BigDecimal("10"))
            første.saldoBoetter.brukSaldo(saldoId, BigDecimal("20"))
            andre.satsBoetter.restSats(satsId) shouldBe BigDecimal("100")
            andre.saldoBoetter.finnRestSaldo(saldoId) shouldBe BigDecimal("200")
            sats.restSats(satsId) shouldBe BigDecimal("100")
            saldo.finnRestSaldo(saldoId) shouldBe BigDecimal("200")
            første.segmenter.first().scratchpad[OppdragId(7)] = BeregningsScratchpad()
            andre.segmenter.first().scratchpad shouldBe emptyMap()
            første.restFrikortBeloep = BigDecimal.TEN
            andre.restFrikortBeloep shouldBe null
            første.segmenter.first().ekstraInfo shouldNotBeSameInstanceAs andre.segmenter.first().ekstraInfo
            første.segmenter.first().beregningsStatusflagg shouldNotBeSameInstanceAs andre.segmenter.first().beregningsStatusflagg
            første.segmenter
                .first()
                .delperioder
                .first()
                .ekstraInfo shouldNotBeSameInstanceAs
                andre.segmenter
                    .first()
                    .delperioder
                    .first()
                    .ekstraInfo
        }

        test("output construction retains scalar metadata and scratchpad delberegninger") {
            val segment =
                BeregningsSegment(
                    delperioder = listOf(delperiode()),
                    beregningsStatusflagg = mutableSetOf(BeregningsStatusflagg.DATOSTYRT),
                )
            segment.scratchpad[OppdragId(7)] = BeregningsScratchpad(beregninger = mutableMapOf(intervall to mutableListOf()))
            val kontekst = kontekst(listOf(segment, BeregningsSegment(delperioder = emptyList())))
            val beregning = kontekst.beregninger().single()
            beregning.periode shouldBe intervall
            beregning.fagomraade shouldBe FagomraadeKode.AAP
            beregning.konto shouldBe "123"
            beregning.gjelder shouldBe AktoerId(42)
            beregning.utbetalesTil shouldBe AktoerId(43)
            beregning.beregnet shouldBe beregnet
            beregning.status shouldBe BeregningsStatus.BEREGNET
            beregning.statusflagg shouldBe listOf(BeregningsStatusflagg.DATOSTYRT)
            beregning.overfoeres shouldBe dag(15)
            beregning.delberegning.single().oppdragId shouldBe OppdragId(7)
            beregning.delberegning.single().datoIntervall shouldBe intervall
            kontekst.aktivDato() shouldBe dag(20)
            kontekst(emptyList(), overføringsdato = null).aktivDato() shouldBe beregnet.toLocalDate()
        }

        test("avvent uses its transfer date including null without changing result recipient") {
            listOf(null, dag(25)).forEach { overfoeres ->
                val segment =
                    BeregningsSegment(
                        delperioder = listOf(delperiode()),
                        avventStatus = BeregningsStatus.AVVENT,
                        avventOverfoeres = overfoeres,
                    )
                val beregning = kontekst(listOf(segment)).beregninger().single()
                beregning.status shouldBe BeregningsStatus.AVVENT
                beregning.overfoeres shouldBe overfoeres
                beregning.utbetalesTil shouldBe AktoerId(43)
            }
        }

        test("output rejects scratchpad entries belonging to another numeric identity") {
            val segment = BeregningsSegment(delperioder = listOf(delperiode()))
            segment.scratchpad[OppdragId(999)] = BeregningsScratchpad(beregninger = mutableMapOf(intervall to mutableListOf()))
            val feil = shouldThrow<IllegalArgumentException> { kontekst(listOf(segment)).beregninger() }
            feil.message.orEmpty() shouldContain nokkel.toString()
            feil.message.orEmpty() shouldContain intervall.toString()
        }
    })

private val nokkel = Oppdragsnokkel(Fagområde("AAP"), FagsystemId("fagsystemId"))
private val intervall = DatoIntervall(dag(5), dag(11))
private val beregnet = LocalDateTime.of(2026, 1, 3, 10, 0)
private val klassekode = YtelseKlasseKode(YtelseKlasseKodeRef("AAPUAA"), "test", true, true, listOf(FagomraadeKode.AAP))
private val klassekoder =
    Klassekodeoppslag {
        when (it.kode) {
            "AAPUAA" -> klassekode
            "EXCLUDED" -> klassekode.copy(kode = YtelseKlasseKodeRef("EXCLUDED"), grunnlagTrekk = false)
            else -> throw NoSuchElementException("Unknown classification: $it")
        }
    }
private val identitet =
    object : Beregningsidentitet {
        override fun oppdragId(nokkel: Oppdragsnokkel) = OppdragId(7)

        override fun gjelderId(personidentifikator: Personidentifikator) =
            when (personidentifikator.identifikator) {
                "source-person" -> AktoerId(42)
                "other-person" -> AktoerId(99)
                else -> error("Unknown person")
            }

        override fun mottakerId(utbetalesTil: UtbetalesTil) =
            when (utbetalesTil) {
                UtbetalesTil.Person(Personidentifikator("source-recipient")) -> AktoerId(43)
                UtbetalesTil.Person(Personidentifikator("other-person")) -> AktoerId(99)
                else -> error("Unknown recipient")
            }

        override fun resultatKjedeId(kjedeId: UUID): OppdragslinjekjedeId = error("No result chains during context construction")
    }

private fun dag(dag: Int): LocalDate = LocalDate.of(2026, 1, dag)

private fun <T> opplysning(verdi: T): Opplysning<T> = Opplysning(SaksbehandlerId("Z999999"), verdi)

private fun opplysninger(
    sats: String,
    vedtakssats: String? = null,
): Opplysninger =
    tidslinjeForTest(UUID.randomUUID(), LukketPeriode(dag(1), dag(31)) to BigDecimal(sats), vedtakssats = vedtakssats?.toBigDecimal())[dag(1)]
        .values
        .single()
        .copy(
            gjelderId = opplysning(Personidentifikator("source-person")),
            utbetalesTil = opplysning(UtbetalesTil.Person(Personidentifikator("source-recipient"))),
        )

private fun kilde(vararg verdier: Opplysninger): Tidslinjesamling<UUID, Opplysninger> =
    DefaultTidslinjesamling(
        verdier.associate { UUID.randomUUID() to Tidslinje.Builder<Opplysninger>().periode(LukketPeriode(dag(1), dag(31)), it).build() },
    )

private fun delperiode(
    kilde: Tidslinjesamling<UUID, Opplysninger> = kilde(opplysninger("100")),
    oppdragsnokkel: Oppdragsnokkel = nokkel,
    oppdragId: OppdragId = OppdragId(7),
    fagomraade: FagomraadeKode = FagomraadeKode.AAP,
    klassekoder: Klassekodeoppslag = no.nav.utbetaling.beregningsflyt.klassekoder,
    identitet: Beregningsidentitet = no.nav.utbetaling.beregningsflyt.identitet,
): OppdragDelperiode =
    OppdragDelperiode(
        oppdrag = kilde,
        oppdragsnokkel = oppdragsnokkel,
        oppdragId = oppdragId,
        fagomraade = fagomraade,
        identitet = identitet,
        klassekoder = klassekoder,
        periode = intervall,
        konto = "123",
        gjelder = AktoerId(42),
        beregnet = beregnet,
        utbetalesTil = AktoerId(43),
        status = BeregningsStatus.BEREGNET,
        trekk = emptyList(),
        overfoeringsdato = dag(15),
        tidligereTrukneTrekkvedtakIMaaneden = emptySet(),
    )

private fun kontekst(
    segmenter: List<BeregningsSegment>,
    satsBoetter: SatsBoetter = SatsBoetter(emptyMap()),
    saldoBoetter: SaldoBoetter = SaldoBoetter(emptyMap()),
    overføringsdato: LocalDate? = dag(20),
): BeregningsmotorKontekst =
    BeregningsmotorKontekst(
        periode = intervall,
        segmenter = segmenter,
        skatteKontekst = SkatteKontekst(null, emptyMap()),
        satsBoetter = satsBoetter,
        saldoBoetter = saldoBoetter,
        overføringsdato = overføringsdato,
        beregnet = beregnet,
    )
