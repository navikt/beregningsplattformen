package no.nav.utbetaling.beregning.oppdrag.input_for_beregning

data class InputForBeregning(
    val fordelInput: FordelingInput,
    val oppdrag: List<Oppdrag>,
)
