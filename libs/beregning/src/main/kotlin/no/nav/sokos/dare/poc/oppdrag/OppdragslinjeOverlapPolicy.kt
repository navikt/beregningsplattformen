package no.nav.sokos.dare.poc.oppdrag

class OppdragslinjeOverlapPolicy {
    fun vurder(
        nyOppdragslinje: Oppdragslinje,
        refererteOppdragslinjer: List<Oppdragslinje>,
    ): OppdragslinjeOverlapResultat {
        val slettes =
            refererteOppdragslinjer.takeWhile { eksisterende ->
                nyOppdragslinje.periode.contains(eksisterende.periode) ||
                    (
                        eksisterende.periode.overlaps(nyOppdragslinje.periode) &&
                            !eksisterende.datoVedtakFom.isBefore(nyOppdragslinje.datoVedtakFom)
                    )
            }
        val beholdes = refererteOppdragslinjer.getOrNull(slettes.size)
        val avkortes =
            beholdes
                ?.takeIf { eksisterende ->
                    eksisterende.datoVedtakFom.isBefore(nyOppdragslinje.datoVedtakFom) &&
                        eksisterende.periode.overlaps(nyOppdragslinje.periode)
                }?.copy(datoVedtakTom = nyOppdragslinje.datoVedtakFom.minusDays(1))

        return OppdragslinjeOverlapResultat(
            slettes = slettes,
            avkortes = avkortes,
        )
    }
}

data class OppdragslinjeOverlapResultat(
    val slettes: List<Oppdragslinje>,
    val avkortes: Oppdragslinje?,
)
