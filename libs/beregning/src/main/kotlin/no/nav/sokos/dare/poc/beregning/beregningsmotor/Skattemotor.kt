package no.nav.sokos.dare.poc.beregning.beregningsmotor

import java.math.BigDecimal
import java.math.BigDecimal.ZERO
import java.math.RoundingMode
import java.time.Month.DECEMBER
import java.time.Month.JANUARY

import kotlin.collections.forEach

import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.beregning.DelberegningType
import no.nav.sokos.dare.poc.beregning.Delberegningslinje
import no.nav.sokos.dare.poc.beregning.EkstraInfo
import no.nav.sokos.dare.poc.beregning.EkstraInfoDsl.verdi
import no.nav.sokos.dare.poc.beregning.EkstraInfoValue
import no.nav.sokos.dare.poc.beregning.LinjeKlassekode
import no.nav.sokos.dare.poc.beregning.TekstEkstraInfo
import no.nav.sokos.dare.poc.beregning.dataklasser.BeregningsStatusflagg
import no.nav.sokos.dare.poc.beregning.dataklasser.Satstype
import no.nav.sokos.dare.poc.beregning.fordeler.BeregningsScratchpad
import no.nav.sokos.dare.poc.beregning.fordeler.Beregningsgrunnlag
import no.nav.sokos.dare.poc.beregning.fordeler.BeregningsmotorKontekst
import no.nav.sokos.dare.poc.beregning.fordeler.Delperiodegrunnlag
import no.nav.sokos.dare.poc.beregning.fordeler.Segmentgrunnlag
import no.nav.sokos.dare.poc.skattekort.domain.Forskuddstrekk
import no.nav.sokos.dare.poc.skattekort.domain.ForskuddstrekkKode
import no.nav.sokos.dare.poc.skattekort.domain.ForskuddstrekkType
import no.nav.sokos.dare.poc.skattekort.domain.Skattekort
import no.nav.sokos.dare.poc.skattekort.domain.Tilleggsopplysning
import no.nav.sokos.dare.poc.skattetrekk.SkatteKonto
import no.nav.sokos.dare.poc.skattetrekk.SkatteTabell

// TODO: Legg til ekstrainfo-informasjon ved skatteberegning (grunnlag, sats, type frikort, etc.)
class Skattemotor {
    fun beregnSkatteTrekk(kontekst: BeregningsmotorKontekst): BeregningsmotorKontekst = beregnSkatteTrekkFelles(kontekst)

    fun <K : Beregningsgrunnlag> beregnSkatteTrekkFelles(kontekst: K): K {
        val skattekort = kontekst.skatteKontekst.skattekort
        if (skattekort == null) {
            return beregnMedDefaultSkatt(kontekst)
        }
        return beregnMedSkattekort(kontekst, skattekort)
    }

    private fun <K : Beregningsgrunnlag> beregnMedSkattekort(
        kontekst: K,
        skattekort: Skattekort,
    ): K {
        kontekst.beregningsrekkefolge().forEach { (segment, delperiode) ->
            val trekkKode =
                delperiode.fagomraade
                    .faggruppe
                    .forskuddsTrekkKode
            // TODO: if-setningen under fanger at vi faar lov til aa bruke aarets skattekort i desember aaret foer, men ikke i november. Bør bli mer lesbart.
            val delperiodeErIForegåendeÅr = delperiode.periode.fom.year < kontekst.beregnet.year
            if (delperiodeErIForegåendeÅr && (kontekst.beregnet.month > JANUARY || delperiode.periode.fom.month < DECEMBER)) {
                beregnForrigeAarsSkatt(kontekst, delperiode, segment, trekkKode)
                return@forEach
            }

            val forskuddstrekk: Forskuddstrekk? = skattekort.erGyldigForBeregningsDato(kontekst.beregnet)?.forskuddsTrekk?.find { it.trekkKode == trekkKode }
            if (forskuddstrekk == null) {
                beregnDelperiodeMedDefaultskatt(segment, delperiode, skattekort)
                return@forEach
            }

            when (forskuddstrekk.typeForskuddsTrekk) {
                ForskuddstrekkType.FRIKORT -> {
                    if (forskuddstrekk.frikortBelop == null) {
                        beregnFrikortUtenBeloepsgrense(kontekst, delperiode, segment)
                    } else {
                        beregnFrikortMedBeloepsgrense(kontekst, delperiode, segment)
                    }
                }

                ForskuddstrekkType.PROSENTKORT -> {
                    beregnProsenttrekk(delperiode, segment, forskuddstrekk, skattekort)
                }

                ForskuddstrekkType.TABELLKORT -> {
                    // Handled in second pass below
                }
            }
        }

        beregnTabellkort(kontekst, skattekort)

        return kontekst
    }

    private fun beregnFrikortUtenBeloepsgrense(
        kontekst: Beregningsgrunnlag,
        delperiode: Delperiodegrunnlag,
        segment: Segmentgrunnlag,
    ) {
        val scratchpad = scratchpadFor(segment, delperiode)
        val skatteGrunnlag =
            scratchpad.kapasitetStyring
                .current()
                .values
                .sumOf { it }

        leggTilSkattLinje(
            scratchpad = scratchpad,
            periode = delperiode.periode,
            beloep = ZERO,
            satstype = Satstype.PROSENT,
            sats = ZERO,
            skattekort = kontekst.skatteKontekst.skattekort,
            ekstraInfo =
                setOf(
                    TekstEkstraInfo(
                        grunn = EkstraInfo.Reasons.FRIKORT_UTEN_BELOPSGRENSE_BRUKT,
                        verdier =
                            mapOf(
                                "grunnlag" verdi skatteGrunnlag,
                                "dekketAvFrikort" verdi skatteGrunnlag,
                            ),
                    ),
                ),
        )

        leggTilFrikortBrukLinje(
            scratchpad = scratchpad,
            periode = delperiode.periode,
            forbruktFrikort = skatteGrunnlag,
            ekstraInfo = emptySet(),
        )
    }

    private fun beregnFrikortMedBeloepsgrense(
        kontekst: Beregningsgrunnlag,
        delperiode: Delperiodegrunnlag,
        segment: Segmentgrunnlag,
    ) {
        val restFrikort = requireNotNull(kontekst.restFrikortBeloep) { "Rest frikortbeløp mangler" }
        val scratchpad = scratchpadFor(segment, delperiode)

        val skatteGrunnlag = delperiode.periode.hverdager.sumOf { scratchpad.kapasitetStyring.current()[it] ?: ZERO }

        if (skatteGrunnlag <= ZERO) return

        val dekketAvFrikort = skatteGrunnlag.min(restFrikort)
        val overskudd = skatteGrunnlag - dekketAvFrikort

        kontekst.restFrikortBeloep = restFrikort - dekketAvFrikort

        if (overskudd <= ZERO) {
            leggTilSkattLinje(
                scratchpad = scratchpad,
                periode = delperiode.periode,
                beloep = ZERO,
                satstype = Satstype.PROSENT,
                sats = ZERO,
                skattekort = kontekst.skatteKontekst.skattekort,
                ekstraInfo =
                    setOf(
                        TekstEkstraInfo(
                            grunn = EkstraInfo.Reasons.FRIKORT_MED_BELOEPSGRENSE_BRUKT,
                            verdier =
                                mapOf(
                                    "grunnlag" verdi skatteGrunnlag,
                                    "dekketAvFrikort" verdi dekketAvFrikort,
                                ),
                        ),
                    ),
            )
            leggTilFrikortBrukLinje(
                scratchpad = scratchpad,
                periode = delperiode.periode,
                forbruktFrikort = dekketAvFrikort,
                ekstraInfo = emptySet(),
            )
            return
        }

        val skattesats = delperiode.fagomraade.faggruppe.standardSkatteprosent
        val skattBeloep = beregnProsentBeloep(overskudd, skattesats)

        leggTilSkattLinje(
            scratchpad = scratchpad,
            periode = delperiode.periode,
            beloep = skattBeloep,
            satstype = Satstype.PROSENT,
            sats = skattesats,
            skattekort = kontekst.skatteKontekst.skattekort,
            ekstraInfo =
                setOfNotNull(
                    TekstEkstraInfo(
                        grunn = EkstraInfo.Reasons.FRIKORT_MED_BELOEPSGRENSE_BRUKT_RESTSKATT,
                        verdier =
                            mapOf(
                                "grunnlag" verdi skatteGrunnlag,
                                "dekketAvFrikort" verdi dekketAvFrikort,
                                "overskudd" verdi overskudd,
                                "prosentSats" verdi skattesats,
                            ),
                    ),
                ),
        )
        leggTilFrikortBrukLinje(
            scratchpad = scratchpad,
            periode = delperiode.periode,
            forbruktFrikort = dekketAvFrikort,
            ekstraInfo = emptySet(),
        )
    }

    private fun beregnForrigeAarsSkatt(
        kontekst: Beregningsgrunnlag,
        delperiode: Delperiodegrunnlag,
        segment: Segmentgrunnlag,
        trekkKode: ForskuddstrekkKode,
    ) {
        val scratchpad = scratchpadFor(segment, delperiode)
        val overskuddSkatt = utledSkattesatsForOverskudd(kontekst, trekkKode, delperiode.fagomraade.faggruppe.standardSkatteprosent)
        val skattesats = overskuddSkatt.sats
        val skatteGrunnlag = delperiode.periode.hverdager.sumOf { scratchpad.kapasitetStyring.current()[it] ?: ZERO }
        val skattBeloep = beregnProsentBeloep(skatteGrunnlag, skattesats)
        val aar = delperiode.periode.fom.year

        leggTilSkattLinje(
            scratchpad = scratchpad,
            periode = delperiode.periode,
            beloep = skattBeloep,
            satstype = Satstype.PROSENT,
            sats = skattesats,
            skattekort = kontekst.skatteKontekst.skattekort,
            ekstraInfo =
                setOfNotNull(
                    TekstEkstraInfo(
                        grunn = EkstraInfo.Reasons.SKATTESATS_FORRIGE_AAR,
                        verdier =
                            mapOf(
                                "aar" verdi aar,
                                "prosentSats" verdi skattesats,
                            ),
                    ),
                    overskuddSkatt.ekstraInfo,
                ),
        )
    }

    private fun beregnProsenttrekk(
        delperiode: Delperiodegrunnlag,
        segment: Segmentgrunnlag,
        forskuddstrekk: Forskuddstrekk,
        skattekort: Skattekort,
    ) {
        val prosentsats = requireNotNull(forskuddstrekk.prosentsats) { "Prosentkort mangler prosentsats" }
        val scratchpad = scratchpadFor(segment, delperiode)

        val skatteGrunnlag = delperiode.periode.hverdager.sumOf { scratchpad.kapasitetStyring.current()[it] ?: ZERO }
        val skattBeloep = beregnProsentBeloep(skatteGrunnlag, prosentsats)

        leggTilSkattLinje(
            scratchpad = scratchpad,
            periode = delperiode.periode,
            beloep = skattBeloep,
            satstype = Satstype.PROSENT,
            sats = prosentsats,
            skattekort = skattekort,
            ekstraInfo =
                setOf(
                    TekstEkstraInfo(
                        grunn = EkstraInfo.Reasons.SKATT_TREKK_PROSENT,
                        verdier =
                            mapOf(
                                "inntektsAar" verdi skattekort.inntektsaar,
                                "grunnlag" verdi skatteGrunnlag,
                                "utstedtDato" verdi skattekort.utstedtDato,
                                "prosentSats" verdi prosentsats,
                                "skattekortIdentifikator" verdi skattekort.identifikator,
                            ),
                    ),
                ),
        )
    }

    private fun beregnTabellkort(
        kontekst: Beregningsgrunnlag,
        skattekort: Skattekort,
    ) {
        val tabellkortPerioder = klassifiserTabellkortPerioder(kontekst, skattekort)
        if (tabellkortPerioder.isEmpty()) return
        fordelTabelltrekk(tabellkortPerioder, skattekort)
    }

    private fun klassifiserTabellkortPerioder(
        kontekst: Beregningsgrunnlag,
        skattekort: Skattekort,
    ): List<TabellkortPeriode> {
        val tabellkortPerioder = mutableListOf<TabellkortPeriode>()

        kontekst.segmenter.forEach { segment ->
            segment.delperioder.forEach { delperiode ->
                if (delperiode.periode.fom.year < skattekort.inntektsaar) return@forEach

                val forskuddstrekk = finnForskuddstrekk(skattekort, delperiode) ?: return@forEach
                if (forskuddstrekk.typeForskuddsTrekk != ForskuddstrekkType.TABELLKORT) return@forEach

                val scratchpad = scratchpadFor(segment, delperiode)
                val skatteGrunnlag = delperiode.periode.hverdager.sumOf { scratchpad.kapasitetStyring.current()[it] ?: ZERO }

                // Etterbetaling → prosent-skatt
                if (scratchpad.erEtterbetaling[delperiode.periode] == true) {
                    val prosentsats = requireNotNull(forskuddstrekk.prosentsats) { "Tabellkort mangler prosentsats for etterbetaling" }
                    beregnProsentSkattForTabellkort(
                        scratchpad = scratchpad,
                        delperiode = delperiode,
                        skatteGrunnlag = skatteGrunnlag,
                        prosentsats = prosentsats,
                        grunn = EkstraInfo.Reasons.SKATT_ETTERBETALING,
                        skattekort = skattekort,
                        verdier =
                            mapOf(
                                "grunnlag" verdi skatteGrunnlag,
                                "prosentSats" verdi prosentsats,
                            ),
                    )
                    return@forEach
                }

                // Biarbeidsgiver → prosent-skatt
                val trekkKode =
                    delperiode.fagomraade
                        .faggruppe
                        .forskuddsTrekkKode
                val allokertFaggruppe = kontekst.skatteKontekst.allokeringer[trekkKode]
                val oppdragFaggruppe =
                    delperiode.fagomraade
                        .faggruppe
                if (allokertFaggruppe != oppdragFaggruppe) {
                    val prosentsats = requireNotNull(forskuddstrekk.prosentsats) { "Tabellkort mangler prosentsats for biarbeidsgiver" }
                    beregnProsentSkattForTabellkort(
                        scratchpad = scratchpad,
                        delperiode = delperiode,
                        skatteGrunnlag = skatteGrunnlag,
                        prosentsats = prosentsats,
                        grunn = EkstraInfo.Reasons.SKATT_BIARBEIDSGIVER,
                        skattekort = skattekort,
                        verdier =
                            mapOf(
                                "grunnlag" verdi skatteGrunnlag,
                                "prosentSats" verdi prosentsats,
                                "oppdragFaggruppe" verdi oppdragFaggruppe,
                                "allokertFaggruppe" verdi allokertFaggruppe,
                            ),
                    )
                    return@forEach
                }

                if (skatteGrunnlag <= ZERO) return@forEach
                tabellkortPerioder.add(TabellkortPeriode(delperiode, scratchpad, skatteGrunnlag, forskuddstrekk))
            }
        }

        return tabellkortPerioder
    }

    private fun fordelTabelltrekk(
        tabellkortPerioder: List<TabellkortPeriode>,
        skattekort: Skattekort,
    ) {
        val tabellNummer = requireNotNull(tabellkortPerioder.first().forskuddstrekk.trekktabell) { "Tabellkort mangler tabellnummer" }
        val totalGrunnlag = tabellkortPerioder.sumOf { it.skatteGrunnlag }
        if (totalGrunnlag <= ZERO) return
        val (totalDager, partitionEkstraInfo) = tabellkortPerioder.beregnTabellkortDager()

        tabellkortPerioder
            .map { it.scratchpad }
            .distinctBy { System.identityHashCode(it) }
            .forEach { scratchpad ->
                scratchpad.ekstraInfo.addAll(partitionEkstraInfo)
            }

        val tabelltrekkBeregning = SkatteTabell.beregnTabellTrekk(tabellNummer, totalDager, totalGrunnlag)
        val totalSkatt = tabelltrekkBeregning.trekkbeloep

        tabellkortPerioder
            .asSequence()
            .filter { it.skatteGrunnlag > ZERO }
            .map { periode ->
                periode to (totalSkatt * periode.skatteGrunnlag / totalGrunnlag).setScale(0, RoundingMode.HALF_UP)
            }.forEach { (periode, lokalSkatt) ->
                leggTilSkattLinje(
                    scratchpad = periode.scratchpad,
                    periode = periode.delperiode.periode,
                    beloep = lokalSkatt,
                    satstype = Satstype.TABELL,
                    sats = ZERO,
                    skattekort = skattekort,
                    ekstraInfo =
                        setOf(
                            TekstEkstraInfo(
                                grunn = EkstraInfo.Reasons.SKATT_TABELL_HOVEDARBEIDSGIVER,
                                verdier =
                                    mapOf(
                                        "trekktabell" verdi tabellNummer,
                                        "trekkgrunnlag" verdi totalGrunnlag,
                                        "dager" verdi totalDager,
                                        "totalSkatt" verdi totalSkatt,
                                        "skattegrunnlag" verdi periode.skatteGrunnlag,
                                        "lokalSkatt" verdi lokalSkatt,
                                    ),
                            ),
                        ),
                )
            }
    }

    private fun beregnProsentSkattForTabellkort(
        scratchpad: BeregningsScratchpad,
        delperiode: Delperiodegrunnlag,
        skatteGrunnlag: BigDecimal,
        prosentsats: BigDecimal,
        grunn: EkstraInfo.Reasons,
        skattekort: Skattekort?,
        verdier: Map<String, EkstraInfoValue>,
    ) {
        val skattBeloep = beregnProsentBeloep(skatteGrunnlag, prosentsats)

        leggTilSkattLinje(
            scratchpad = scratchpad,
            periode = delperiode.periode,
            beloep = skattBeloep,
            satstype = Satstype.PROSENT,
            sats = prosentsats,
            skattekort = skattekort,
            ekstraInfo =
                setOf(
                    TekstEkstraInfo(
                        grunn = grunn,
                        verdier = verdier,
                    ),
                ),
        )
    }

    private fun utledSkattesatsForOverskudd(
        kontekst: Beregningsgrunnlag,
        trekkKode: ForskuddstrekkKode,
        standardSkatteprosent: BigDecimal,
    ): ProsentsatsMedGrunn {
        val skattekort =
            kontekst.skatteKontekst.skattekort
                ?: return ProsentsatsMedGrunn(
                    standardSkatteprosent,
                    TekstEkstraInfo(
                        grunn = EkstraInfo.Reasons.SKATTESATS_DEFAULT,
                        verdier =
                            mapOf(
                                "prosentSats" verdi standardSkatteprosent,
                            ),
                    ),
                )
        val erSvalbard = skattekort.tilleggsopplysning.contains(Tilleggsopplysning.OPPHOLD_PAA_SVALBARD)
        val erTiltakssone = skattekort.tilleggsopplysning.contains(Tilleggsopplysning.OPPHOLD_I_TILTAKSSONE)
        val erKildeskattPensjonist = skattekort.tilleggsopplysning.contains(Tilleggsopplysning.KILDESKATT_PENSJONIST)
        val sats =
            when (trekkKode) {
                ForskuddstrekkKode.LOENN_FRA_NAV -> {
                    when {
                        erSvalbard -> BigDecimal("15.6")
                        erTiltakssone -> BigDecimal(40)
                        else -> BigDecimal(44)
                    }
                }

                ForskuddstrekkKode.PENSJON_FRA_NAV -> {
                    when {
                        erSvalbard -> BigDecimal("13.1")
                        erTiltakssone -> BigDecimal(28)
                        erKildeskattPensjonist -> BigDecimal(15)
                        else -> BigDecimal(32)
                    }
                }

                ForskuddstrekkKode.UFOERETRYGD_FRA_NAV -> {
                    when {
                        erSvalbard -> BigDecimal("15.6")
                        erTiltakssone -> BigDecimal(40)
                        else -> BigDecimal(44)
                    }
                }

                else -> {
                    throw IllegalStateException("Ingen andre trekkoder er lov å bruke i NAV")
                }
            }
        val ekstraInfo =
            when {
                erSvalbard -> {
                    TekstEkstraInfo(
                        grunn = EkstraInfo.Reasons.SKATTESATS_SVALBARD,
                        verdier =
                            mapOf(
                                "prosentSats" verdi sats,
                            ),
                    )
                }

                erTiltakssone -> {
                    TekstEkstraInfo(
                        grunn = EkstraInfo.Reasons.SKATTESATS_TILTAKSSONE,
                        verdier =
                            mapOf(
                                "prosentSats" verdi sats,
                            ),
                    )
                }

                erKildeskattPensjonist -> {
                    TekstEkstraInfo(
                        grunn = EkstraInfo.Reasons.SKATTESATS_KILDESKATT_PENSJONIST,
                        verdier =
                            mapOf(
                                "prosentSats" verdi sats,
                            ),
                    )
                }

                else -> {
                    TekstEkstraInfo(
                        grunn = EkstraInfo.Reasons.SKATTESATS_DEFAULT,
                        verdier =
                            mapOf(
                                "prosentSats" verdi sats,
                            ),
                    )
                }
            }
        return ProsentsatsMedGrunn(sats, ekstraInfo)
    }

    private fun <K : Beregningsgrunnlag> beregnMedDefaultSkatt(kontekst: K): K {
        kontekst.beregningsrekkefolge().forEach { (segment, delperiode) ->
            beregnDelperiodeMedDefaultskatt(segment, delperiode, kontekst.skatteKontekst.skattekort)
        }
        return kontekst
    }

    private fun beregnDelperiodeMedDefaultskatt(
        segment: Segmentgrunnlag,
        delperiode: Delperiodegrunnlag,
        skattekort: Skattekort?,
    ) {
        val scratchpad = scratchpadFor(segment, delperiode)
        val skatteGrunnlag = delperiode.periode.hverdager.sumOf { scratchpad.kapasitetStyring.current()[it] ?: ZERO }
        val skattesats = delperiode.fagomraade.faggruppe.standardSkatteprosent
        val skattBeloep = beregnProsentBeloep(skatteGrunnlag, skattesats)

        segment.beregningsStatusflagg.add(BeregningsStatusflagg.MANGLER_SKATTEKORT)

        leggTilSkattLinje(
            scratchpad = scratchpad,
            periode = delperiode.periode,
            beloep = skattBeloep,
            satstype = Satstype.PROSENT,
            sats = skattesats,
            skattekort = skattekort,
            ekstraInfo =
                setOf(
                    TekstEkstraInfo(
                        grunn = EkstraInfo.Reasons.SKATT_MANGLENDE_SKATTEKORT,
                        verdier =
                            mapOf(
                                "grunnlag" verdi skatteGrunnlag,
                                "prosentSats" verdi skattesats,
                            ),
                    ),
                ),
        )
    }

    // --- Helpers ---

    private fun finnForskuddstrekk(
        skattekort: Skattekort,
        delperiode: Delperiodegrunnlag,
    ): Forskuddstrekk? {
        val trekkKode =
            delperiode.fagomraade
                .faggruppe
                .forskuddsTrekkKode
        return skattekort.forskuddsTrekk.find { it.trekkKode == trekkKode }
    }

    private fun scratchpadFor(
        segment: Segmentgrunnlag,
        delperiode: Delperiodegrunnlag,
    ): BeregningsScratchpad {
        val oppdragId = delperiode.oppdragId
        return segment.scratchpad.getOrPut(oppdragId) { BeregningsScratchpad() }
    }

    private fun beregnProsentBeloep(
        grunnlag: BigDecimal,
        prosentsats: BigDecimal,
    ): BigDecimal = (grunnlag * prosentsats / BigDecimal(100)).setScale(0, RoundingMode.HALF_UP)

    private fun leggTilFrikortBrukLinje(
        scratchpad: BeregningsScratchpad,
        periode: DatoIntervall,
        forbruktFrikort: BigDecimal,
        ekstraInfo: Set<EkstraInfo>,
    ) {
        if (forbruktFrikort <= ZERO) return

        val frikortBrukLinje =
            Delberegningslinje(
                id = null,
                delberegningId = null,
                type = DelberegningType.FRIKORT_BRUK,
                dagsbeloep = emptyList(),
                beloep = forbruktFrikort,
                ekstra = ekstraInfo,
                satsType = Satstype.PROSENT,
                sats = ZERO,
            )

        scratchpad.beregninger
            .getOrPut(periode) { mutableListOf() }
            .add(frikortBrukLinje)
    }

    private fun leggTilSkattLinje(
        scratchpad: BeregningsScratchpad,
        periode: DatoIntervall,
        beloep: BigDecimal,
        satstype: Satstype,
        sats: BigDecimal,
        skattekort: Skattekort?,
        ekstraInfo: Set<TekstEkstraInfo>,
    ) {
        val skatteKonto = skattekort?.skattekonto() ?: SkatteKonto.FORSKUDDSTREKK
        val kapasitet = scratchpad.kapasitetStyring.snapshot(periode)

        val skattDelberegningslinje =
            Delberegningslinje(
                id = null,
                delberegningId = null,
                type = DelberegningType.SKATT,
                dagsbeloep = emptyList(),
                beloep = beloep,
                ekstra = ekstraInfo,
                kapasitet = kapasitet,
                satsType = satstype,
                sats = sats,
                kodeKlasse = LinjeKlassekode.Skatt(skatteKonto.klassekode),
                reskontroKonto = skatteKonto.konto,
            )

        scratchpad.beregninger
            .getOrPut(periode) { mutableListOf() }
            .add(-skattDelberegningslinje)
        if (beloep > ZERO) {
            scratchpad.kapasitetStyring.consumeSkatt(periode, beloep)
        }
    }
}
