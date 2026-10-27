package no.nav.sokos.dare.poc.beregning.fordeler

import java.math.BigDecimal

/**
 * Alt det følgende vil avgrenses av perioden til beregningsmotorkonteksten. Om vi har en åpen periode i
 * en trekkperiode vil den lukkes fordi beregningsmotorkontekstens periode er lukket.
 *
 * Trekkperioder med måneds- eller dagsats trenger å spore gjenværende kapasitet under en beregning. Denne
 * klassen har dette ansvaret.
 *
 * Klassen initialiseres under oppretting av beregningsmotorkonteksten, og vil lese satser fra databasen.
 * Det som lagres her under oppretting er derimot satsen minus allerede utførte trekk i lagrede beregninger.
 * Dette kaller vi for gjenværende sats, og representerer et beløp per dag eller per måned.
 *
 * Når beregninger for enkeltoppdrag gjøres vil trekkberegningene sørge for å trekke kapasitet ut av satsbøtta.
 *
 * En analog struktur finnes for saldo på trekkvedtaknivå.
 *
 * boette - dersom trekkperioden har en månedssats, og går over mer enn en måned, vil det lages flere bøtter med boette-angivelser av typen "2026-02"
 *          Om trekkperioden har månedssats og er mindre enn en måned, f.eks. trekkperiode 2026-02-04 til 2026-02-20 vil boette bli "2026-02"
 *          Om trekkperioden har dagsats vil det lages en bøtte per dag, uansett om perioden er lenger enn en måned, med boette = null
 *          Det lages bare bøtter for dag- og måneds-type satser.
 */
data class SatsBoetter(
    val innkommendeSatser: Map<SatsBoetteId, BigDecimal>,
) {
    val satser: MutableMap<SatsBoetteId, BigDecimal> = innkommendeSatser.toMutableMap()

    fun restSats(id: SatsBoetteId): BigDecimal? = satser[id]

    fun brukSats(
        id: SatsBoetteId,
        beloep: BigDecimal,
    ) {
        satser[id]?.let { verdi ->
            val nyverdi = verdi.minus(beloep)
            require(nyverdi.signum() > -1, { "Satsbøtte $id ville ende med negativ verdi om $beloep ble trukket fra $verdi" })
            satser[id] = verdi.minus(beloep)
        }
    }
}
