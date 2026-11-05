package no.nav.utbetaling.beregningsflyt

import java.math.BigDecimal
import java.time.LocalDateTime
import no.nav.sokos.dare.poc.beregning.fordeler.SkatteKontekst
import no.nav.utbetaling.beregning.oppdrag.Oppdragstidslinje
import no.nav.utbetaling.beregning.tid.Periode

data class Beregningsgrunnlag (
    val beregningsperiode: Periode.LukketPeriode,
    val oppdragstidslinje: Oppdragstidslinje,
    val skatteKontekst: SkatteKontekst,
    val beregningstidspunkt: () -> LocalDateTime = { LocalDateTime.now() },
    val tilgjengeligFrikortbeløp: BigDecimal?, // TODO: Lage en beløpsklasse istedenfor å bruke BigDecimal direkte - og hvorfor er det ikke en del av skatteKonteksten?
)