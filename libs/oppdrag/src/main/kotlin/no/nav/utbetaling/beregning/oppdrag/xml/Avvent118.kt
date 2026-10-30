package no.nav.utbetaling.beregning.oppdrag.xml

import java.time.LocalDate

/**
 * Denne dataklassen styrer at vi ikke sender over beregningen til utbetaling før datoOverfores
 */
internal data class Avvent118(
    val datoAvventFom: LocalDate? = null, // Påkrevet, ikke opsjonell
    val datoAvventTom: LocalDate? = null, // Påkrevet, ikke opsjonell
    val kodeArsak: String? = null,
    /* avav er avvent avregning, skal ligge og vente uavhengig av hva overføringsdato er satt til. Beregningen denne fører til må reaktiveres manuelt, og blir da satt til REAK
       avrk er avvent refusjonskrav, skal ligge til datoOverføres, og så overføres automatisk. Økonomimedarbeider skal kunne overstyre overføringsdato på beregningen. Da blir beregningen overført
       umiddelbart.
       hvis man får to avvent118 med samme datoAvventFom avviser vi hele oppdraget
       hvis man får to avvent118 med overlappende periode vil også oppdraget avvises
       det er lov å få to avvent118 med ikke-overlappende perioder
     */
    val datoOverfores: LocalDate? = null, // ikke valgfritt hvis AVRK, valgfritt ved AVAV som uansett må reaktiveres manuelt.
    val feilreg: String? = null, // "nei", "j". perioden må være helt lik selv om bare datofom er nøkkel. avvis oppdraget dersom vi får en feilreg="j" som ikke matcher en avvent118 vi allerede har
)
