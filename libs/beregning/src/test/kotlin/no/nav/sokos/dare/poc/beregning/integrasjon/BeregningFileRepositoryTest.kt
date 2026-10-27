package no.nav.sokos.dare.poc.beregning.integrasjon

import java.math.BigDecimal
import java.time.LocalDateTime

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

import no.nav.sokos.dare.poc.TestObjects
import no.nav.sokos.dare.poc.TestUtil.oppdragFraFil
import no.nav.sokos.dare.poc.aktoer.AktoerId
import no.nav.sokos.dare.poc.beregning.Beregning
import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Beregningsmotor
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Skattemotor
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Trekkmotor
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Ytelsesmotor
import no.nav.sokos.dare.poc.beregning.dataklasser.BeregningsStatus
import no.nav.sokos.dare.poc.beregning.dataklasser.BeregningsStatusflagg
import no.nav.sokos.dare.poc.beregning.dto.BeregningsPresentasjonV2
import no.nav.sokos.dare.poc.beregning.fordeler.BeregningsFordeler
import no.nav.sokos.dare.poc.beregning.fordeler.SkatteKontekst
import no.nav.sokos.dare.poc.testFordelingInput
import no.nav.sokos.dare.poc.trekk.TrekkAlternativ

class BeregningFileRepositoryTest :
    FunSpec({
        val motor = Beregningsmotor(Ytelsesmotor(), Trekkmotor(), Skattemotor())
        val fordeler = BeregningsFordeler(motor)
        val testFilnavn = "beregning-round-trip-test.json"

        afterSpec {
            // File(IntegrationTestConstants.outputDir, testFilnavn).delete()
        }

        test("presentasjoner should allow null overfoeres") {
            val oppdrag = oppdragFraFil("aapApiTest/simple.xml")
            val beregning =
                Beregning(
                    periode = DatoIntervall("2026-02-01", "2026-02-28"),
                    konto = "konto123",
                    overfoeres = null,
                    fagomraade = oppdrag.fagomraade,
                    gjelder = AktoerId(1),
                    beregnet = LocalDateTime.parse("2026-02-01T10:00:00"),
                    delberegning = emptyList(),
                )

            BeregningsPresentasjonV2(beregning).overfoeres shouldBe null
        }

        test("serialiserer og deserialiserer List<Beregning> uten tap av data") {
            val vedtak =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.LOPM,
                    type = TestObjects.TrekkType.YTELSESPERIODE_SKATTEREDUSERENDE,
                    perioder = listOf(DatoIntervall("2026-02-01", "2026-02-28"), DatoIntervall("2026-03-01", "2026-03-15")),
                    sats = BigDecimal("1000"),
                )

            val beregningsPeriode = DatoIntervall("2026-02-01", "2026-03-15")
            val beregningsOppdrag = listOf(oppdragFraFil("aapApiTest/simple.xml"))
            val beregningsTrekk = listOf(vedtak)
            val input = testFordelingInput(konto = "konto123")

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
            val beregnet = motor.calculate(kontekst)
            val original =
                beregnet.beregninger().map {
                    it.copy(
                        status = BeregningsStatus.AVVENT,
                        statusflagg = listOf(BeregningsStatusflagg.MANUELL),
                        overfoeres = null,
                    )
                }

            BeregningFileRepository.lagre(original, testFilnavn)
            val deserialisert = BeregningFileRepository.les(testFilnavn)

            deserialisert shouldHaveSize original.size

            val ob = original.first()
            val db = deserialisert.first()
            db.periode shouldBe ob.periode
            db.konto shouldBe ob.konto
            db.fagomraade shouldBe ob.fagomraade
            db.gjelder shouldBe ob.gjelder
            db.overfoeres shouldBe ob.overfoeres
            db.beregnet shouldBe ob.beregnet
            db.status shouldBe ob.status
            db.statusflagg shouldBe ob.statusflagg

            db.delberegning shouldHaveSize ob.delberegning.size
            db.delberegning.forEachIndexed { i, del ->
                val orig = ob.delberegning[i]
                del.oppdragId shouldBe orig.oppdragId
                del.datoIntervall shouldBe orig.datoIntervall
                del.linjer shouldHaveSize orig.linjer.size
                del.linjer.forEachIndexed { j, linje ->
                    linje.type shouldBe orig.linjer[j].type
                    linje.beloep.compareTo(orig.linjer[j].beloep) shouldBe 0
                    linje.dagsbeloep.size shouldBe orig.linjer[j].dagsbeloep.size
                }
            }
        }
    })
