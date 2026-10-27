package no.nav.sokos.dare.poc.beregning

import java.math.BigDecimal

import kotlin.reflect.KClass
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

import no.nav.sokos.dare.poc.structural.IdValueClass

const val MASK = "***SKJULT***"

@Serializable
sealed interface EkstraInfo {
    @Serializable
    enum class Reasons(
        val template: String? = null,
        val clazz: KClass<out EkstraInfo> = TekstEkstraInfo::class,
    ) {
        FEIL_I_BEREGNING("For dette trekket finnes det negative satsbøtter. Dette er ikke støttet. Denne beregningen er feil"),
        TIDLIGERE_UTBETALT("Tidligere utbetalt i beregning {beregningId}, overført {overfoertDato:dato}"),
        KREVER_TILBAKEKREVING(
            "Beregning for oppdrag {oppdrag} for periode {periode} har ytelse {ytelse:beloep}, " +
                "men tidligere utbetalt {utbetalt:beloep}. Dette krever tilbakekreving, som ikke er implementert enda",
        ),
        SKATT_MANGLENDE_SKATTEKORT("Skattekort mangler, grunnlag {grunnlag:beloep}, bruker fast skattesats {prosentSats:prosent} prosent"),
        SKATT_TIDLIGERE_AAR_FAST_SKATT("Skatt for tidligere år beregnet med fast skattesats"),
        SKATT_TIDLIGERE_AAR_PROSENT_TILTAKSSONE_SKATT("Skatt for tidligere år beregnet med prosentsats for tiltakssone"),
        SKATT_TIDLIGERE_AAR_PROSENT_SVALBARD_SKATT("Skatt for tidligere år beregnet med prosentsats for Svalbard"),

        SKATT_TREKK_DEFAULT("type: DefaultSkatteTrekk, fagomraade: {fagomraade}, grunnlag: {grunnlag:beloep}, prosentSats: {prosentSats:prosent}"),

        SKATT_TREKK_TABELL(
            "type: TabellSkatteTrekk, inntektsår: {inntektsAar:heltall}, grunnlag: {grunnlag:beloep}, utstedt-dato: {utstedtDato:dato}," +
                " trekkTabell: {trekktabell}, skattekort-identifikator: {skattekortIdentifikator}, utsettes: {utsettes}, " +
                "periode: {periode}, skattedager: {skattedager:heltall}",
        ),
        SKATT_TREKK_PROSENT(
            "type: ProsentSkatteTrekk, inntektsår: {inntektsAar:heltall}, grunnlag: {grunnlag:beloep}, utstedt-dato: {utstedtDato:dato}," +
                " prosentSats: {prosentSats:prosent}, skattekort-identifikator: {skattekortIdentifikator}",
        ),
        SKATT_TILLEGGSOPPLYSNING("Tilleggsopplysning om skatt"),

        STOPPNIVAA("Dummy stoppnivaa"),
        MANUELL("{tekst}"),
        OVERSTYRT_MOTTAKER("Mottaker er overstyrt til {utbetalesTilId}"),
        KID_ENDRET("Kid har endret seg"),
        TREKK(
            "trekk: {kode} {alternativ}, Fra: {datoFom:dato}, Til: {datoTom:dato}, sats: {sats:beloep}, skattered: {redusererSkattegrunnlag}, saldo: {saldo}, trekkgrunnlag: {trekkgrunnlag}, kapasitet: {kapasitet} ",
        ),

        TREKK_MAANED_HALVERING(
            "Månedssats halvert fra {opprinneligSats:beloep} til {halvertSats:beloep} fordi dette er første trekk for samme oppdrag og trekkvedtak i overføringsmåneden",
        ),
        FRIKORT_UTEN_BELOPSGRENSE_BRUKT("Frikort uten beløpsgrense brukt. Grunnlag={grunnlag:beloep}, dekket av frikort={dekketAvFrikort:beloep}"),
        FRIKORT_MED_BELOEPSGRENSE_BRUKT("Frikortbeløp brukt. Grunnlag={grunnlag:beloep}, dekket av frikort={dekketAvFrikort:beloep}"),
        FRIKORT_MED_BELOEPSGRENSE_BRUKT_RESTSKATT(
            "Frikortbeløp brukt. Grunnlag={grunnlag:beloep}, dekket av frikort={dekketAvFrikort:beloep}, beskattet overskudd={overskudd:beloep}, sats={prosentSats:prosent}%",
        ),
        SKATTESATS_SVALBARD("Skattesats overstyrt pga opphold på svalbard, sats er {prosentSats:prosent}"),
        SKATTESATS_TILTAKSSONE("Skattesats overstyrt pga opphold i tiltakssone, sats er {prosentSats:prosent}"),
        SKATTESATS_KILDESKATT_PENSJONIST("Skattesats overstyrt pga kildeskatt, sats er {prosentSats:prosent}"),
        SKATTESATS_DEFAULT("Skattesats satt til {prosentSats:prosent}"),
        SKATTESATS_FORRIGE_AAR("Beregning for forrige år ({aar:heltall}), bruker fast skattesats {prosentSats:prosent}%"),
        SKATTESATS_FJORAARS_SKATTEKORT(
            "Bruker fjorårets skattekort for januar {aar:heltall}, mangler skattekort for inneværende år",
        ),
        SKATTESATS_MANGLER_SKATTEKORT_ETTER_JANUAR(
            "Mangler skattekort for inneværende år, bruker fast skattesats {prosentSats:prosent}%",
        ),

        SKATT_ETTERBETALING(
            "Skatt prosentberegnes fra grunnlag {grunnlag:beloep} med sats {prosentSats:prosent} prosent fordi det er etterbetaling av en tidligere utbetalt ytelse",
        ),
        SKATT_BIARBEIDSGIVER(
            "Skatt prosentberegnes fra grunnlag {grunnlag:beloep} med sats {prosentSats:prosent} prosent fordi ytelsen ikke er i faggruppen skattekortet er allokert til" +
                " (oppdrag: {oppdragFaggruppe} vs {allokertFaggruppe})",
        ),
        SKATT_TABELL_HOVEDARBEIDSGIVER(
            "Skatt trukket etter tabell {trekktabell}, grunnlag {trekkgrunnlag:beloep}, arbeidsdager {dager:heltall}," +
                " total beregnet skatt {totalSkatt:beloep}, lokalt skattegrunnlag {skattegrunnlag:beloep}, lokal skatteandel {lokalSkatt:beloep}",
        ),
        SKATT_TABELL_VERDIBEREGNET_PARTISJON(
            "Verdiberegnet tabellkortdager-partisjon: brutto ytelse {bruttoYtelse:beloep}, vedtakssats {vedtakssats:beloep}, " +
                "arbeidsdager {arbeidsdager:heltall}, skattedager {skattedager:heltall}",
        ),
        AVVENT("Beregningen er satt på vent ({kodeArsak}). Overføringsdato: {datoOverfores:dato}"),
    }
}

class EkstraInfoValueSerializer(
    private val maskSkjult: Boolean = true,
) : KSerializer<EkstraInfoValue> {
    companion object : KSerializer<EkstraInfoValue> by EkstraInfoValueSerializer()

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("EkstraInfoValue", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: EkstraInfoValue,
    ) {
        encoder.encodeString(if (maskSkjult && value.skjult) MASK else value.verdi)
    }

    override fun deserialize(decoder: Decoder): EkstraInfoValue = EkstraInfoValue(verdi = decoder.decodeString(), skjult = false)
}

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class TekstEkstraInfo private constructor(
    val grunn: EkstraInfo.Reasons,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val verdier: Map<String, EkstraInfoValue> = emptyMap(),
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val tekst: String? = null,
    val template: String = grunn.template ?: "",
) : EkstraInfo {
    init {
        if (EkstraInfoValidering.skalValidere()) {
            val forventedeParametere =
                PLASSHOLDER_REGEX
                    .findAll(grunn.template.orEmpty())
                    .map { it.groupValues[1] }
                    .toSet()
            val manglendeParametere = forventedeParametere - verdier.keys
            val uventedeParametere = verdier.keys - forventedeParametere
            require(manglendeParametere.isEmpty() && uventedeParametere.isEmpty()) {
                "Parametere samsvarer ikke med malen for ${grunn.name}: manglende=${manglendeParametere.sorted()}, uventede=${uventedeParametere.sorted()}"
            }
        }
    }

    constructor(
        grunn: EkstraInfo.Reasons,
        verdier: Map<String, EkstraInfoValue> = emptyMap(),
        tekst: String? = null,
    ) : this(
        grunn = grunn,
        verdier = verdier,
        tekst = tekst,
        template = grunn.template ?: "",
    )

    override fun toString(): String {
        val template = grunn.template ?: return tekst ?: (grunn.name + (" " + verdier.entries.joinToString(", ")))

        return template.replace(PLASSHOLDER_REGEX) { match ->
            val key = match.groupValues[1]
            verdier[key]?.let { if (it.skjult) MASK else it.verdi } ?: match.value
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TekstEkstraInfo

        if (grunn != other.grunn) return false
        if (verdier != other.verdier) return false

        return true
    }

    override fun hashCode(): Int {
        var result = grunn.hashCode()
        result = 31 * result + verdier.hashCode()
        return result
    }

    companion object {
        private val PLASSHOLDER_REGEX = Regex("\\{([^{}:]+)(?::[^{}]+)?}")
    }
}

@Serializable
data class EkstraInfoValue(
    val verdi: String,
    val skjult: Boolean = false,
) {
    constructor(verdi: IdValueClass, skjult: Boolean = false) : this(verdi.id.toString(), skjult)
}

object EkstraInfoDsl {
    private fun Any?.toVerdiString(): String =
        when (this) {
            is BigDecimal -> toPlainString()
            is IdValueClass -> id.toString()
            else -> toString()
        }

    infix fun String.verdi(verdi: Any?): Pair<String, EkstraInfoValue> = this to EkstraInfoValue(verdi.toVerdiString())

    infix fun String.skjult(verdi: Any?): Pair<String, EkstraInfoValue> = this to EkstraInfoValue(verdi.toVerdiString(), skjult = true)
}
