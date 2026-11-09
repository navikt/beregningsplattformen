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

        val (funker, funkerIkke) =
            Scenario.alle.partition {
                it.navn in
                    setOf(
                        "GrunnleggendeTest/oppdrag_med_kjedet_ytelse_med_overlapp",
                        "GrunnleggendeTest/oppdrag_med_kjedet_ytelse_uten_overlapp",
                        "GrunnleggendeTest/oppdrag_med_komplisert_kjeding",
                        "GrunnleggendeTest/oppdrag_med_to_linjer_uten_overlapp_eller_hull",
                        "GrunnleggendeTest/oppdrag_uten_noe_annet",
                        "GrunnleggendeTest/oppdrag_med_et_saldotrekk_som_ikke_utfoeres_fullstendig",
                        "GrunnleggendeTest/oppdrag_med_et_saldotrekk_som_utfoeres_fullstendig",
                        "SatsRelatertTest/utbdato_trekk_utenfor_overfoeringsdato_paavirker_ikke",
                        "SatsRelatertTest/utbdato_dagssats_med_relatert_samme_overfoeringsdato",
                        "SatsRelatertTest/utbdato_dagssats_relatert_annen_overfoeringsdato_paavirker_ikke",
                        "SatsRelatertTest/utbdato_dagssats_med_satsboette",
                        "SatsRelatertTest/utbdato_maanedssats_halvering_foerste_utbetaling",
                        "SatsRelatertTest/utbdato_maanedssats_halvering_uten_tidligere_utbetaling",
                        "SatsRelatertTest/utbdato_maanedssats_ingen_halvering_utenfor_kandidatgruppe",
                        "SatsRelatertTest/utbdato_maanedssats_med_relatert_samme_overfoeringsdato",
                        "SatsRelatertTest/utbdato_maanedssats_relatert_annen_overfoeringsdato_paavirker_ikke",
                        "SatsRelatertTest/utbdato_maanedssats_med_satsboette",
                        "SatsRelatertTest/utbdato_prosenttrekk_ikke_begrenset_av_satsboette",
                        "SatsRelatertTest/ytelper_dagssats_med_relatert_som_dekker_en_dag",
                        "SatsRelatertTest/ytelper_dagssats_med_relatert_beloep_prorateres",
                        "SatsRelatertTest/ytelper_dagssats_med_relatert_som_spenner_flere_dager",
                        "SatsRelatertTest/ytelper_maanedssats_kort_periode_med_relatert_som_reduserer",
                        "SatsRelatertTest/ytelper_relatert_med_annet_trekkvedtak_paavirker_ikke",
                        "SatsRelatertTest/ytelper_dagssats_uten_relaterte",
                        "SatsRelatertTest/ytelper_prosenttrekk_ikke_begrenset_av_satsboette",
                        "SatsRelatertTest/ytelper_maanedssats_med_relatert_som_reduserer",
                        "SkattProsentkortTest/prosentkort_med_skattereduserende_trekk",
                        "SkattProsentkortTest/prosentkort_beregner_skatt_med_prosentsats",
                        "SkattFrikortTest/frikort_belopsgrense_uttoemmes",
                        "SkattFrikortTest/frikort_uten_beloepsgrense_trekker_ikke_skatt",
                        "SkattAarsskifteTest/januar_beregning_ikke_fjoraars_skattekort",
                        "SkattAarsskifteTest/januar_med_fjoraars_skattekort",
                        "SkattAarsskifteTest/aarsskifte_frikort",
                        "SkattAarsskifteTest/aarsskifte_med_fjoraars_januar_februar",
                        "SkattAarsskifteTest/aarsskifte_prosentkort",
                        "SkattAarsskifteTest/januar_februar_split_med_fjoraars",
                        "AvventTest/avrk_enkel",
                        "AvventTest/avav_enkel",
                        "AvventTest/avvent_delvis_overlapp",
                        "AvventTest/avvent_to_perioder",
                        "AvventTest/avvent_med_tabelltrekk",
                        "TidligereUtbetaltTest/enkel_tidligere_utbetalt",
                        "TidligereUtbetaltTest/tidligere_utbetalt_med_proratering",
                    )
            }

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
                            beregningstidspunkt = { LocalDateTime.parse(scenario.kontekst.beregnet) },
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

        xcontext("Test som ikke funker ") {
            kjørScenario(Scenariolaster.last("SatsRelatertTest/utbdato_maanedssats_med_satsboette"))
        }

        context("Tester som funker") {
            withData(nameFn = { it.navn }, funker) { scenario ->
                kjørScenario(scenario)
            }
        }

        xcontext("Tester som ikke funker") {
            withData(nameFn = { it.navn }, funkerIkke) { scenario ->
                kjørScenario(scenario)
            }
        }
    })
