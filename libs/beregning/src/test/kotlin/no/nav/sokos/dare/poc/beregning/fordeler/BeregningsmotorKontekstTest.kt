package no.nav.sokos.dare.poc.beregning.fordeler

import java.math.BigDecimal
import java.time.LocalDateTime

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

import no.nav.sokos.dare.poc.TestObjects
import no.nav.sokos.dare.poc.TestUtil.oppdragFraFil
import no.nav.sokos.dare.poc.aktoer.AktoerId
import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.beregning.DelberegningType
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Beregningsmotor
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Skattemotor
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Trekkmotor
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Ytelsesmotor
import no.nav.sokos.dare.poc.beregning.dataklasser.BeregningsStatus
import no.nav.sokos.dare.poc.oppdrag.OppdragId
import no.nav.sokos.dare.poc.testFordelingInput
import no.nav.sokos.dare.poc.trekk.FagomraadeKode
import no.nav.sokos.dare.poc.trekk.TrekkAlternativ
import no.nav.sokos.dare.poc.trekk.Trekkvedtak

class BeregningsmotorKontekstTest :
    FunSpec({
        val motor = Beregningsmotor(Ytelsesmotor(), Trekkmotor(), Skattemotor())
        val fordeler = BeregningsFordeler(motor)

        test("skal returnere én beregning med riktig periode og metadata for ett oppdrag") {
            val periode = DatoIntervall("2026-02-01", "2026-02-28")
            val oppdrag = listOf(oppdragFraFil("aapApiTest/enkel-ytelse-feb-til-mid-mar.xml"))
            val trekk = emptyList<Trekkvedtak>()
            val input = testFordelingInput(konto = "konto123")

            val kontekst =
                fordeler.byggBeregningsmotorKontekst(
                    periode = periode,
                    oppdrag = oppdrag,
                    trekk = trekk,
                    input = input,
                    skatteKontekst = SkatteKontekst(skattekort = null, allokeringer = emptyMap()),
                    relaterteBeregninger = emptyList(),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )
            val beregnet = motor.calculate(kontekst)

            val beregninger = beregnet.beregninger()
            beregninger shouldHaveSize 1

            val beregning = beregninger.first()
            beregning.periode shouldBe periode
            beregning.konto shouldBe "konto123"
            beregning.gjelder shouldBe AktoerId(1)
            beregning.fagomraade shouldBe FagomraadeKode.AAP
        }

        test("beregninger should propagate status and allow null overfoeres from grunnlag") {
            val periode = DatoIntervall("2026-02-01", "2026-02-28")
            val oppdrag = listOf(oppdragFraFil("aapApiTest/enkel-ytelse-feb-til-mid-mar.xml"))
            val trekk = emptyList<Trekkvedtak>()
            val input = testFordelingInput(status = BeregningsStatus.AVVENT, overfoeringsdato = null)

            val kontekst =
                fordeler.byggBeregningsmotorKontekst(
                    periode = periode,
                    oppdrag = oppdrag,
                    trekk = trekk,
                    input = input,
                    skatteKontekst = SkatteKontekst(skattekort = null, allokeringer = emptyMap()),
                    relaterteBeregninger = emptyList(),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )
            val beregnet = motor.calculate(kontekst)

            val beregning = beregnet.beregninger().single()
            beregning.status shouldBe BeregningsStatus.AVVENT
            beregning.overfoeres shouldBe null
        }

        test("beregninger oppretter Delberegning med riktig oppdragId og periode fra scratchpad") {
            val periode = DatoIntervall("2026-02-01", "2026-02-28")
            val oppdrag = listOf(oppdragFraFil("aapApiTest/enkel-ytelse-feb-til-mid-mar.xml"))
            val trekk = emptyList<Trekkvedtak>()
            val input = testFordelingInput()

            val kontekst =
                fordeler.byggBeregningsmotorKontekst(
                    periode = periode,
                    oppdrag = oppdrag,
                    trekk = trekk,
                    input = input,
                    skatteKontekst = SkatteKontekst(skattekort = null, allokeringer = emptyMap()),
                    relaterteBeregninger = emptyList(),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )
            val beregnet = motor.calculate(kontekst)

            val beregninger = beregnet.beregninger()
            beregninger shouldHaveSize 1

            val delberegninger = beregninger.first().delberegning
            delberegninger shouldHaveSize 1

            val del = delberegninger.first()
            del.oppdragId shouldBe OppdragId(0)
            del.datoIntervall shouldBe DatoIntervall("2026-02-01", "2026-02-28")
        }

        test("beregninger inneholder ytelseslinjer og skattelinjer i delberegning") {
            val periode = DatoIntervall("2026-02-01", "2026-02-28")
            val oppdrag = listOf(oppdragFraFil("aapApiTest/enkel-ytelse-feb-til-mid-mar.xml"))
            val trekk = emptyList<Trekkvedtak>()
            val input = testFordelingInput()

            val kontekst =
                fordeler.byggBeregningsmotorKontekst(
                    periode = periode,
                    oppdrag = oppdrag,
                    trekk = trekk,
                    input = input,
                    skatteKontekst = SkatteKontekst(skattekort = null, allokeringer = emptyMap()),
                    relaterteBeregninger = emptyList(),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )
            val beregnet = motor.calculate(kontekst)

            val linjer =
                beregnet
                    .beregninger()
                    .first()
                    .delberegning
                    .first()
                    .linjer
            linjer.any { it.type == DelberegningType.YTELSE } shouldBe true
            linjer.any { it.type == DelberegningType.SKATT } shouldBe true
        }

        test("beregninger med to måneder gir to delberegninger for samme oppdragId") {
            val vedtak =
                TestObjects.trekkVedtak(
                    trekkalt = TrekkAlternativ.LOPM,
                    type = TestObjects.TrekkType.YTELSESPERIODE_SKATTEREDUSERENDE,
                    perioder = listOf(DatoIntervall("2026-02-01", "2026-02-28"), DatoIntervall("2026-03-01", "2026-03-15")),
                    sats = BigDecimal("1000"),
                )

            val periode = DatoIntervall("2026-02-01", "2026-03-15")
            val oppdrag = listOf(oppdragFraFil("aapApiTest/enkel-ytelse-feb-til-mid-mar.xml"))
            val trekk = listOf(vedtak)
            val input = testFordelingInput()

            val kontekst =
                fordeler.byggBeregningsmotorKontekst(
                    periode = periode,
                    oppdrag = oppdrag,
                    trekk = trekk,
                    input = input,
                    skatteKontekst = SkatteKontekst(skattekort = null, allokeringer = emptyMap()),
                    relaterteBeregninger = emptyList(),
                    overføringsdato = LocalDateTime.parse("2026-10-19T00:00:00").toLocalDate(),
                    beregnet = LocalDateTime.parse("2026-10-19T00:00:00"),
                )
            val beregnet = motor.calculate(kontekst)

            val beregninger = beregnet.beregninger()
            beregninger shouldHaveSize 1

            val delberegninger = beregninger.first().delberegning
            delberegninger.size shouldBe 2

            delberegninger[0].datoIntervall shouldBe DatoIntervall("2026-02-01", "2026-02-28")
            delberegninger[0].oppdragId shouldBe OppdragId(0)
            delberegninger[1].datoIntervall shouldBe DatoIntervall("2026-03-01", "2026-03-15")
            delberegninger[1].oppdragId shouldBe OppdragId(0)
        }
    })
