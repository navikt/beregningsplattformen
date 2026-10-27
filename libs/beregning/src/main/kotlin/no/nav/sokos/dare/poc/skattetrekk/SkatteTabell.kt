package no.nav.sokos.dare.poc.skattetrekk

import java.math.BigDecimal
import java.time.LocalDateTime

import kotlin.math.roundToLong

import no.skatteetaten.fastsetting.formueinntekt.forskudd.trekkrutine2026.Periode
import no.skatteetaten.fastsetting.formueinntekt.forskudd.trekkrutine2026.Tabellnummer
import no.skatteetaten.fastsetting.formueinntekt.forskudd.trekkrutine2026.Trekkrutine

import no.nav.sokos.dare.poc.beregning.intRound

private const val TREKKRUTINE_PACKAGE_PREFIX = "no.skatteetaten.fastsetting.formueinntekt.forskudd.trekkrutine"

object SkatteTabell {
    fun beregnTabellTrekk(
        tabellNummer: String,
        dager: Int,
        trekkgrunnlag: BigDecimal,
    ): TabelltrekkBeregning {
        val periodeForTrekktabell = periodeForTrekktabell(dager)
        val periode = periodeForTrekktabell.periode
        val trekkgrunnlagLong = trekkgrunnlag.intRound().toLong()

        val trekkbeloep: Long =
            when (periodeForTrekktabell) {
                // Regler uten justering
                PeriodeForTrekktabell.PERIODE_1_DAG_A,
                PeriodeForTrekktabell.PERIODE_2_DAGER_A,
                PeriodeForTrekktabell.PERIODE_3_DAGER_A,
                PeriodeForTrekktabell.PERIODE_4_DAGER_A,
                PeriodeForTrekktabell.PERIODE_1_UKE_B,
                PeriodeForTrekktabell.PERIODE_14_DAGER_C,
                PeriodeForTrekktabell.PERIODE_1_MAANED_E,
                -> beregnTrekkBelop(tabellNummer, periode, trekkgrunnlagLong)
                // Regler med justering
                PeriodeForTrekktabell.PERIODE_HALV_MAANED_C ->
                    (beregnTrekkBelop(tabellNummer, periode, trekkgrunnlagLong * 2) / 2.0).roundToLong()
                PeriodeForTrekktabell.PERIODE_UKER_D -> (beregnTrekkBelop(tabellNummer, periode, trekkgrunnlagLong / 3) * 3)
                PeriodeForTrekktabell.PERIODE_4_UKER_E -> (beregnTrekkBelop(tabellNummer, periode, trekkgrunnlagLong / 2) * 2)
                PeriodeForTrekktabell.PERIODE_FLERE_MAANEDER_F -> {
                    val maaneder = Math.round(dager / 30.0)
                    (beregnTrekkBelop(tabellNummer, periode, trekkgrunnlagLong / maaneder) * maaneder)
                }
                PeriodeForTrekktabell.PERIODE_FLERE_UKER_G -> {
                    val uker = Math.round(dager / 7.0)
                    (beregnTrekkBelop(tabellNummer, periode, trekkgrunnlagLong / uker) * uker)
                }
            }
        return TabelltrekkBeregning(trekkbeloep.toBigDecimal(), periode.name, periodeForTrekktabell.regel)
    }

    private fun beregnTrekkBelop(
        tabell: String,
        periode: Periode,
        trekkgrunnlag: Long,
    ): Long {
        val aar = LocalDateTime.now().year
        return if (periode.javaClass.name.contains(".trekkrutine$aar.")) {
            Trekkrutine.beregnTabelltrekk(Tabellnummer.valueOf("TABELL_$tabell"), periode, trekkgrunnlag)
        } else {
            beregnTrekkBelopForAar(aar, tabell, periode, trekkgrunnlag)
        }
    }

    // Fordi vi ikke ønsker å oppdatere alle tester årlig.
    private fun beregnTrekkBelopForAar(
        aar: Int,
        tabell: String,
        periode: Periode,
        trekkgrunnlag: Long,
    ): Long {
        val prefix: String = TREKKRUTINE_PACKAGE_PREFIX + aar + "."

        val tabellNummerClass = Class.forName(prefix + "Tabellnummer")
        val tabellNummer = tabellNummerClass.getMethod("valueOf", String::class.java).invoke(null, "TABELL_$tabell")

        val periodeClass = Class.forName(prefix + "Periode")
        val periodeForIAar = periodeClass.getMethod("valueOf", String::class.java).invoke(null, periode.name)

        val trekkRutineClass = Class.forName(prefix + "Trekkrutine")

        Trekkrutine.beregnTabelltrekk(Tabellnummer.valueOf("TABELL_$tabell"), periode, trekkgrunnlag)

        return trekkRutineClass.getMethod("beregnTabelltrekk", tabellNummerClass, periodeClass, Long::class.java).invoke(
            null, // Because static call
            tabellNummer,
            periodeForIAar,
            trekkgrunnlag,
        ) as Long
    }

    fun periodeForTrekktabell(skatteDager: Int): PeriodeForTrekktabell =
        when (skatteDager) {
            1 -> PeriodeForTrekktabell.PERIODE_1_DAG_A
            2 -> PeriodeForTrekktabell.PERIODE_2_DAGER_A
            3 -> PeriodeForTrekktabell.PERIODE_3_DAGER_A
            4 -> PeriodeForTrekktabell.PERIODE_4_DAGER_A
            in 5..10 -> PeriodeForTrekktabell.PERIODE_1_UKE_B
            15 -> PeriodeForTrekktabell.PERIODE_HALV_MAANED_C // TODO: Usikker på om dette er korrekt tolkning av "halv måned".
            in 11..17 -> PeriodeForTrekktabell.PERIODE_14_DAGER_C
            in 18..24 -> PeriodeForTrekktabell.PERIODE_UKER_D
            28 -> PeriodeForTrekktabell.PERIODE_4_UKER_E
            in 25..33 -> PeriodeForTrekktabell.PERIODE_1_MAANED_E
            in 34..54 -> PeriodeForTrekktabell.PERIODE_FLERE_UKER_G
            else -> {
                when (skatteDager % 30) {
                    in 0..7, in 25..29 -> PeriodeForTrekktabell.PERIODE_FLERE_MAANEDER_F
                    in 8..24 -> PeriodeForTrekktabell.PERIODE_FLERE_UKER_G
                    else -> throw NotImplementedError("Implementasjonsfeil. Periodeberegning er ufullstendig for $skatteDager dager.")
                }
            }
        }
}

data class TabelltrekkBeregning(
    val trekkbeloep: BigDecimal,
    val trekkTabellPeriode: String,
    val regel: String,
) {
    override fun toString() = "$trekkTabellPeriode($regel)"
}

// Skattebetalingsforskriften § 5-10-2.
enum class PeriodeForTrekktabell(
    val periode: Periode,
    val regel: String,
) {
    // a) I lønn for høyst 4 dager skal arbeidsgiveren trekke den nærmest passende dagtabell.
    PERIODE_1_DAG_A(Periode.PERIODE_1_DAG, "a"),
    PERIODE_2_DAGER_A(Periode.PERIODE_2_DAGER, "a"),
    PERIODE_3_DAGER_A(Periode.PERIODE_3_DAGER, "a"),
    PERIODE_4_DAGER_A(Periode.PERIODE_4_DAGER, "a"),

    // b) I lønn for 5 til 10 dager skal det trekkes etter uketabellen.
    PERIODE_1_UKE_B(Periode.PERIODE_1_UKE, "b"),

    // c) I lønn for 11 til 17 dager skal det trekkes etter 14-dagers tabellen.
    PERIODE_14_DAGER_C(Periode.PERIODE_14_DAGER, "c"),

    //    Når det gjelder lønn for en halv måned skal arbeidsgiveren likevel bruke månedstabellen
    //    på det dobbelte lønnsbeløp og dele trekkbeløpet med 2.
    PERIODE_HALV_MAANED_C(Periode.PERIODE_1_MAANED, "b"),

    // d) I lønn for 18 til 24 dager skal arbeidsgiveren bruke uketabellen på en tredjedel av lønnsbeløpet og multiplisere trekkbeløpet med 3.
    PERIODE_UKER_D(Periode.PERIODE_1_UKE, "d"),

    // e) I lønn for minst 25 dager og høyst 1 måned og 3 dager skal det trekkes etter månedstabellen.
    PERIODE_1_MAANED_E(Periode.PERIODE_1_MAANED, "e"),

    //    Når lønnen gjelder 4 uker og skatteyteren regelmessig får lønnen avregnet for 4 uker, skal arbeidsgiveren dog bruke 14-dagertabellen på
    //    halvparten av lønnsbeløpet og multiplisere trekkbeløpet med 2.
    PERIODE_4_UKER_E(Periode.PERIODE_14_DAGER, "e"),

    // f) Gjelder det lønn for flere måneder, eller hele måneder pluss minst 25 dager eller pluss høyst en uke, skal arbeidsgiveren dele lønnsbeløpet
    //    med nærmeste månedstall, lese av trekkbeløpet i månedstabellen og multiplisere dette med samme månedstall.
    PERIODE_FLERE_MAANEDER_F(Periode.PERIODE_1_MAANED, "f"),

    // g) I lønn for en måned plutt 4 til 24 dager, eller flere m[neder pluss 8 til 24 dager skal lønnsbeløpet deles med nærmeste uketall, og trekkbeløpet skal avleses i uketabellen og
    //    multipliseres med samme uketall.
    PERIODE_FLERE_UKER_G(Periode.PERIODE_1_UKE, "g"),
}
