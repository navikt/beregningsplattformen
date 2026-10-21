package no.nav.utbetaling.beregning.oppdrag.inputForBeregning

data class InputForBeregning(
    val fordelInput: FordelingInput,
    val oppdrag: List<Oppdrag>,
)
