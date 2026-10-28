package no.nav.utbetaling.beregningsflyt.verifikasjon

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.equals.shouldNotBeEqual

class EksempelbrukTest: FunSpec({

    fun minBeregning(navn: String, kontekst: Kontekst, oppdrag: List<Oppdrag>, skattekontekst: SkatteKontekst, trekkvedtak: List<Trekkvedtak>): Resultat {
        return Resultat(periode = Periode(fom = "2026-01-01", tom = "2026-01-31"))
    }

    withData(nameFn = { it.navn }, Scenario.alle){ (navn, kontekst, oppdrag, skattekontekst, trekkvedtak, resultat) ->
        minBeregning(navn, kontekst, oppdrag, skattekontekst, trekkvedtak) shouldNotBeEqual resultat
    }
})