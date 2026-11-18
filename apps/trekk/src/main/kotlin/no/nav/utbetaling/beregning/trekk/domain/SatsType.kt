package no.nav.utbetaling.beregning.trekk.domain

enum class SatsType(
    val beskrivelse: String,
) {
    D("dagsats"),
    M("maanedssats"),
    P("prosent av grunnlag"),
}
