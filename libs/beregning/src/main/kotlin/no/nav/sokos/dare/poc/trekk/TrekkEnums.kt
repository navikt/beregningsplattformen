package no.nav.sokos.dare.poc.trekk

enum class TrekkStatus(
    val beskrivelse: String,
) {
    // T1_TREKKSTATUS.KODE_TREKKSTATUS
    AVSL("Avsluttet"),
    FULL("Fullført"),
    HVIL("Hvilende"),
    KANS("Kansellert"),
    LOPE("Aktivt"),
    ;

    companion object {
        fun fromString(status: String): TrekkStatus = entries.first { it.name == status }
    }
}

enum class Trekkberegningstype(
    val beskrivelse: String,
) {
    // refusjonstrekk som har dagsats, månedssats eller prosentsats
    YTELPER("Ytelsesperiode"),

    // utleggstrekk som har månedssats eller prosentsats
    UTBDATO("Utbetalingsdato"),
}

enum class SatsType(
    val beskrivelse: String,
) {
    D("dagsats"),
    M("maanedssats"),
    P("prosent av grunnlag"),
}

@Deprecated(
    "Trekkalternativ er en sammensetning av TrekkAlt og SatsType. Bruk de opprinnelige feltene i stedet. Vi vil ikke ha en modell der det er mulig å ha satstype=D og trekkalternativ=LOPM. Det åpner for feil.",
)
enum class TrekkAlternativ(
    val beskrivelse: String,
    val saldoTillatt: Boolean,
    val periodeTrekk: Boolean,
    val satsType: SatsType,
) {
    // T1_TREKKALTERNATIV.KODE_TREKKALT
    LOPD("Løpende trekk dagsats", false, true, SatsType.D),
    LOPM("Løpende trekk månedssats", false, true, SatsType.M),
    LOPP("Løpende trekk prosentsats", false, false, SatsType.P),
    SALD("Saldotrekk dagsats", true, true, SatsType.D),
    SALM("Saldotrekk månedssats", true, true, SatsType.M),
    SALP("Saldotrekk prosentsats", true, false, SatsType.P),
    ;

    companion object {
        fun fromString(status: String): TrekkAlternativ = entries.first { it.name == status }
    }
}

@Deprecated(
    """SAL === trekkvedtak.belopSaldo != null. LOP === trekkvedtak.any { trekkperiode where not(satstype == P and sats == 100). 
    |Vi  vil ikke ha en modell der det er mulig å ha TrekkAlt=SAL og der saldoBelop == null.""",
)
enum class TrekkAlt(
    val beskrivelse: String,
) {
    LOP("Løpende trekk"),
    SAL("Saldotrekk"),
    ;

    companion object {
        fun fromString(status: String): TrekkAlt = entries.first { it.name == status }
    }
}
