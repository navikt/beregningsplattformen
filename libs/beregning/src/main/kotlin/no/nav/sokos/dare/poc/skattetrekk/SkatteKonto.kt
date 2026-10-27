package no.nav.sokos.dare.poc.skattetrekk

import no.nav.sokos.dare.poc.skattekort.domain.Tilleggsopplysning

enum class SkatteKonto(
    val klassekode: String,
    val konto: String,
) {
    FORSKUDDSTREKK("FSKTSKAT", "0510000"),
    FORSKUDDSTREKK_KILDESKATT("KSKTSKAT", "0512435"),
    FORSKUDDSTREKK_SVALBARD("SSKTSKAT", "0512100"),
    FORSKUDDSTREKK_FRIBELOEP("FSKTSKAT", "0510000"),
    FRIVILLIG_SKATT_BARNEPENSJON("BFRISKAT", "0510099"),
    FRIVILLIG_SKATT_SVALBARD("SFRISKAT", "0512199"),
    MANUELL_SKATT_SVALBARD("SSKT_MAN", "0519994"),
    MANUELL_KILDESKATT("KSKT_MAN", "0519995"),
    MANUELT_FORSKUDDSTREKK_BARNEPENSJON("BPSK_MAN", "0519993"),
    MANUELL_FORSKUDDSSKATT("FSKT_MAN", "0519999"),
    TILBAKEKREVING_SKATT_SVALBARD("SSKT_TB", "0519996"),
    TILBAKEKREVING_KILDESKATT("KSKT_TB", "0519997"),
    TILBAKEKREVING_FORSKUDDSSKATT("FSKT_TB", "0519998"),
    ;

    init {
        validerReskontrokonto(konto)
    }

    companion object {
        private val KILDESKATT_OPPLYSNINGER =
            setOf(
                Tilleggsopplysning.KILDESKATT_PAA_LOENN,
                Tilleggsopplysning.KILDESKATT_PENSJONIST,
            )

        fun from(tilleggsopplysninger: Set<Tilleggsopplysning>): SkatteKonto =
            when {
                Tilleggsopplysning.OPPHOLD_PAA_SVALBARD in tilleggsopplysninger -> FORSKUDDSTREKK_SVALBARD
                tilleggsopplysninger.any { it in KILDESKATT_OPPLYSNINGER } -> FORSKUDDSTREKK_KILDESKATT
                else -> FORSKUDDSTREKK
            }
    }
}

internal fun validerReskontrokonto(konto: String) {
    require(konto.matches(Regex("[0-9]{7}"))) {
        "Reskontokonto må bestå av sju ASCII-sifre"
    }
}
