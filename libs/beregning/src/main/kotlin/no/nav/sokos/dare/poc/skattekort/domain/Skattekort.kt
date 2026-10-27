package no.nav.sokos.dare.poc.skattekort.domain

import java.time.LocalDateTime

import kotlinx.serialization.Serializable

import no.nav.sokos.dare.poc.BigDecimalJson
import no.nav.sokos.dare.poc.LocalDateJson
import no.nav.sokos.dare.poc.LocalDateTimeJson
import no.nav.sokos.dare.poc.aktoer.AktoerId
import no.nav.sokos.dare.poc.skattetrekk.SkatteKonto
import no.nav.sokos.dare.poc.structural.IdValueClass

@Serializable
@JvmInline
value class SkattekortId(
    override val id: Long,
) : IdValueClass

@Serializable
data class Skattekort(
    val id: SkattekortId? = null,
    val aktoerId: AktoerId,
    val inntektsaar: Int,
    val kilde: String,
    val identifikator: Int?,
    val utstedtDato: LocalDateJson?,
    val tidspktReg: LocalDateTimeJson = LocalDateTimeJson.now(),
    val tilleggsopplysning: Set<Tilleggsopplysning> = emptySet(),
    val forskuddsTrekk: List<Forskuddstrekk> = listOf(),
) {
    fun svalbard(): Boolean = tilleggsopplysning.contains(Tilleggsopplysning.OPPHOLD_PAA_SVALBARD)

    fun tiltakssone(): Boolean = tilleggsopplysning.contains(Tilleggsopplysning.OPPHOLD_I_TILTAKSSONE)

    fun skattekonto(): SkatteKonto = SkatteKonto.from(tilleggsopplysning)

    fun erGyldigForBeregningsDato(beregnet: LocalDateTime): Skattekort? {
        if (beregnet.year == inntektsaar || (beregnet.year == inntektsaar + 1 && beregnet.monthValue == 1)) {
            return this
        } else {
            return null
        }
    }
}

@Serializable
@JvmInline
value class ForskuddstrekkId(
    override val id: Long,
) : IdValueClass

@Serializable
data class Forskuddstrekk(
    val id: ForskuddstrekkId? = null,
    val typeForskuddsTrekk: ForskuddstrekkType,
    val trekkKode: ForskuddstrekkKode,
    val trekktabell: String? = null,
    val prosentsats: BigDecimalJson? = null,
    val antallMndForTrekk: BigDecimalJson? = null,
    val frikortBelop: Int? = null,
    val tidspktReg: LocalDateTimeJson = LocalDateTimeJson.now(),
)

enum class ForskuddstrekkKode(
    val value: String,
) {
    LOENN_FRA_HOVEDARBEIDSGIVER("loennFraHovedarbeidsgiver"),
    LOENN_FRA_BIARBEIDSGIVER("loennFraBiarbeidsgiver"),
    LOENN_FRA_NAV("loennFraNAV"),
    PENSJON("pensjon"),
    PENSJON_FRA_NAV("pensjonFraNAV"),
    LOENN_TIL_UTENRIKSTJENESTEMANN("loennTilUtenrikstjenestemann"),
    LOENN_KUN_TRYGDEAVGIFT_TIL_UTENLANDSK_BORGER("loennKunTrygdeavgiftTilUtenlandskBorger"),
    LOENN_KUN_TRYGDEAVGIFT_TIL_UTENLANDSK_BORGER_SOM_GRENSEGJENGER("loennKunTrygdeavgiftTilUtenlandskBorgerSomGrensegjenger"),
    UFOERETRYGD_FRA_NAV("ufoeretrygdFraNAV"),
    UFOEREYTELSER_FRA_ANDRE("ufoereytelserFraAndre"),
    INTRODUKSJONSSTOENAD("introduksjonsstoenad"),
    ;

    companion object {
        fun fromValue(kode: String): ForskuddstrekkKode = entries.find { it.value == kode } ?: error("Ukjent Trekkode: $kode")
    }
}

enum class Tilleggsopplysning(
    val value: String,
) {
    OPPHOLD_PAA_SVALBARD("oppholdPaaSvalbard"),
    KILDESKATT_PENSJONIST("kildeskattpensjonist"),
    OPPHOLD_I_TILTAKSSONE("oppholdITiltakssone"),
    KILDESKATT_PAA_LOENN("kildeskattPaaLoenn"),
    ;

    companion object {
        fun fromValue(value: String): Tilleggsopplysning =
            Tilleggsopplysning.entries.find { it.value == value }
                ?: error("Ukjent Tilleggsopplysning: $value")
    }
}

enum class ForskuddstrekkType(
    val value: String,
) {
    FRIKORT("frikort"),
    TABELLKORT("tabell"),
    PROSENTKORT("prosent"),
    ;

    companion object {
        fun fromValue(value: String): ForskuddstrekkType =
            entries.find { it.value == value }
                ?: error("Ukjent ForskuddstrekkType: $value")
    }
}
