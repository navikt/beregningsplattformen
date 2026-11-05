package no.nav.utbetaling.beregningsflyt

import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import io.kotest.assertions.fail
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import no.nav.sokos.dare.poc.aktoer.AktoerId
import no.nav.sokos.dare.poc.beregning.EkstraInfo
import no.nav.sokos.dare.poc.beregning.dto.BeregningsmotorKontekstPresentasjon
import no.nav.sokos.dare.poc.beregning.fordeler.SkatteKontekst
import no.nav.utbetaling.beregning.oppdrag.Oppdrag
import no.nav.utbetaling.beregning.tid.Periode.LukketPeriode


internal val JsonIgnoreUnknown = Json {
    ignoreUnknownKeys = true
}

class BeregningsflytTest: FunSpec({

    val beregningsflyt = Beregningsflyt()

    val (funker, funkerIkke) = Scenario.alle.partition {
        it.navn in setOf(
            "GrunnleggendeTest/oppdrag_med_kjedet_ytelse_med_overlapp",
            "GrunnleggendeTest/oppdrag_med_kjedet_ytelse_uten_overlapp",
            "GrunnleggendeTest/oppdrag_med_komplisert_kjeding",
            "GrunnleggendeTest/oppdrag_med_to_linjer_uten_overlapp_eller_hull",
            "GrunnleggendeTest/oppdrag_uten_noe_annet",
            "SatsRelatertTest/utbdato_trekk_utenfor_overfoeringsdato_paavirker_ikke",
            "SkattProsentkortTest/prosentkort_beregner_skatt_med_prosentsats",
            "SkattAarsskifteTest/januar_beregning_ikke_fjoraars_skattekort",
            "SkattAarsskifteTest/januar_med_fjoraars_skattekort",
            "SkattAarsskifteTest/aarsskifte_frikort",
            "SkattAarsskifteTest/aarsskifte_med_fjoraars_januar_februar",
            "SkattAarsskifteTest/aarsskifte_prosentkort",
            "SkattAarsskifteTest/januar_februar_split_med_fjoraars"
        )
    }

    fun kjørScenaro(scenario: Scenario) {
        val skatteKontekst = JsonIgnoreUnknown.decodeFromString<SkatteKontekst>(scenario.skatteKontekst)
        val beregningsperiode = LukketPeriode(
            fom = LocalDate.parse(scenario.kontekst.periode.fom),
            tom = LocalDate.parse(scenario.kontekst.periode.tom),
        )
        when (val oppdrag = Oppdrag.fraJson(scenario.oppdrag)) {
            is Oppdrag.Utbetaling -> {
                val kontextOgBeregninger = beregningsflyt.beregn(
                    beregningsperiode = beregningsperiode,
                    oppdragstidslinje = oppdrag.oppdragstidslinje,
                    skatteKontekst = skatteKontekst,
                    beregnet = LocalDateTime.parse(scenario.kontekst.beregnet),
                    restFrikortBeloep = scenario.kontekst.ledigFrikortBeloep?.toBigDecimal()
                )

                val beregningsmotorKontekstPresentasjon = BeregningsmotorKontekstPresentasjon(kontextOgBeregninger.first, kontextOgBeregninger.second )
                val modifisertberegningskontekstpresentasjon = beregningsmotorKontekstPresentasjon.copy(beregninger = beregningsmotorKontekstPresentasjon.beregninger.map { beregning ->
                    beregning.copy(
                        gjelder = AktoerId(scenario.kontekst.gjelder.toLong()),
                        overfoeres = LocalDate.parse(scenario.kontekst.overfoeringsdato),
                        utbetalesTil = scenario.kontekst.utbetalesTil?.let { AktoerId(it.toLong()) }
                    )
                })
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

    context("Tester som funker") {
        withData(nameFn = { it.navn }, funker) { scenario ->
            kjørScenaro(scenario)
        }
    }

    xcontext("Tester som ikke funker") {
        withData(nameFn = { it.navn }, funkerIkke) { scenario ->
            kjørScenaro(scenario)
        }
    }

    test("avviser ukjente typer for motorens EkstraInfo") {
        shouldThrow<SerializationException> {
            JsonIgnoreUnknown.decodeFromString<EkstraInfo>(
                """{"type":"UkjentEkstraInfo","grunn":"SKATT_TILLEGGSOPPLYSNING"}""",
            )
        }
    }
})
