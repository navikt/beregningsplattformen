package no.nav.utbetaling.beregningsflyt

import java.time.LocalDate
import java.time.LocalDateTime

import kotlinx.serialization.json.Json

import io.kotest.assertions.fail
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData

import no.nav.sokos.dare.poc.aktoer.AktoerId
import no.nav.sokos.dare.poc.beregning.Beregning
import no.nav.sokos.dare.poc.beregning.dto.BeregningsmotorKontekstPresentasjon
import no.nav.sokos.dare.poc.beregning.fordeler.SkatteKontekst
import no.nav.sokos.dare.poc.trekk.Trekkvedtak
import no.nav.utbetaling.beregning.oppdrag.Oppdrag
import no.nav.utbetaling.beregning.tid.Periode.LukketPeriode

internal val JsonIgnoreUnknown =
    Json {
        ignoreUnknownKeys = true
    }

class BeregningsflytTest :
    FunSpec({

        val beregningsflyt = Beregningsflyt()

        fun kjørScenario(scenario: Scenario) {
            val skatteKontekst = JsonIgnoreUnknown.decodeFromString<SkatteKontekst>(scenario.skatteKontekst)
            val trekkvedtak = JsonIgnoreUnknown.decodeFromString<List<Trekkvedtak>>(scenario.trekk)
            val relaterteBeregninger =
                scenario.relaterteBeregninger?.let {
                    JsonIgnoreUnknown.decodeFromString<List<Beregning>>(it)
                } ?: emptyList()

            val beregningsperiode =
                LukketPeriode(
                    fom = LocalDate.parse(scenario.kontekst.periode.fom),
                    tom = LocalDate.parse(scenario.kontekst.periode.tom),
                )
            when (val oppdrag = Oppdrag.fraJson(scenario.oppdrag)) {
                is Oppdrag.Utbetaling -> {
                    val beregningsgrunnlag =
                        Beregningsgrunnlag(
                            beregningsperiode = beregningsperiode,
                            oppdragstidslinje = oppdrag.oppdragstidslinje,
                            skatteKontekst = skatteKontekst,
                            trekkVedtak = trekkvedtak,
                            relaterteBeregninger = relaterteBeregninger,
                            beregningstidspunkt =  LocalDateTime.parse(scenario.kontekst.beregnet),
                            tilgjengeligFrikortbeløp = scenario.kontekst.ledigFrikortBeloep?.toBigDecimal(),
                            overføres = LocalDate.parse(scenario.kontekst.overfoeringsdato),
                        )
                    val (beregningskontekst, beregninger) = beregningsflyt.beregn(beregningsgrunnlag)

                    val beregningsmotorKontekstPresentasjon = BeregningsmotorKontekstPresentasjon(beregningskontekst, beregninger)
                    val modifisertberegningskontekstpresentasjon =
                        beregningsmotorKontekstPresentasjon.copy(
                            beregninger =
                                beregningsmotorKontekstPresentasjon.beregninger.map { beregning ->
                                    beregning.copy(
                                        gjelder = AktoerId(scenario.kontekst.gjelder.toLong()),
                                        utbetalesTil = scenario.kontekst.utbetalesTil?.let { AktoerId(it.toLong()) },
                                    )
                                },
                        )
                    sammenliknBeregning(
                        scenario = scenario,
                        resultat = modifisertberegningskontekstpresentasjon,
                    )
                }
                is Oppdrag.Simulering -> fail("Simleringer støttes ikke i beregningsflyten enda")
                is Oppdrag.UgyldigOppdrag -> fail("Feil: ${oppdrag.feil.stackTraceToString()}")
                is Oppdrag.UgyldigKombinasjon -> fail("Feil: ${oppdrag.feil.stackTraceToString()}")
            }
        }

        context("Scenarier") {
            withData(nameFn = { it.navn }, Scenariolaster.lastAlle()) { scenario ->
                kjørScenario(scenario)
            }
        }
    })
