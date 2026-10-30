package no.nav.utbetaling.beregningsflyt

import io.kotest.core.spec.style.FunSpec

import no.nav.sokos.dare.poc.KlassekodeTestdata
import no.nav.sokos.dare.poc.beregning.dataklasser.BeregningsStatus
import no.nav.sokos.dare.poc.beregning.dto.BeregningsmotorKontekstPresentasjon
import no.nav.sokos.dare.poc.beregning.fordeler.FordelingInput
import no.nav.sokos.dare.poc.beregning.integrasjon.BeregningsmotorKontekstKomparator
import no.nav.sokos.dare.poc.beregning.integrasjon.IntegrasjonsInndata
import no.nav.sokos.dare.poc.beregning.integrasjon.IntegrasjonsScenario
import no.nav.sokos.dare.poc.beregning.integrasjon.LegacyReferanseOracle
import no.nav.sokos.dare.poc.oppdrag.YtelseKlasseKodeRef
import no.nav.utbetaling.beregning.oppdrag.Oppdragstidslinje
import no.nav.utbetaling.beregningsflyt.beregning.Beregningsmotor
import no.nav.utbetaling.beregningsflyt.beregning.Ytelsesmotor
import no.nav.utbetaling.beregningsflyt.fordeler.BeregningsFordeler
import no.nav.utbetaling.beregningsflyt.identitet.Klassekodeoppslag
import no.nav.utbetaling.beregningsflyt.kontekst.BeregningsmotorKontekst

class IntegrasjonsTest :
    FunSpec({
        IntegrasjonsScenario.alle().forEach { scenario ->
            test(scenario.navn) {
                val data = scenario.les()
                val faktisk = beregnTidslinje(data)
                BeregningsmotorKontekstKomparator.sammenlign(faktisk, data.forventet)
                val referanse = LegacyReferanseOracle.beregn(scenario, KlassekodeTestdata.klassekodeLookup.values.toList())
                ResultatSammenligning.sammenlign(faktisk, referanse)
            }
        }
    })

internal fun beregnTidslinje(data: IntegrasjonsInndata): BeregningsmotorKontekstPresentasjon = beregnTidslinjeMedTilstand(data).presentasjon()

internal class Tidslinjeberegning(
    val oppdrag: Oppdragstidslinje,
    val identitet: IntegrasjonsIdentitet,
    val kontekst: BeregningsmotorKontekst,
) {
    fun presentasjon(): BeregningsmotorKontekstPresentasjon = BeregningsmotorKontekstPresentasjon(grunnlag = kontekst, beregninger = kontekst.beregninger())
}

internal fun beregnTidslinjeMedTilstand(data: IntegrasjonsInndata): Tidslinjeberegning {
    val oppdrag = lesTidslinje(data.oppdragJson, data.scenario.navn)
    val identitet = IntegrasjonsIdentitet(oppdrag)
    val klassekoder = Klassekodeoppslag { KlassekodeTestdata.klassekodeLookup.getValue(YtelseKlasseKodeRef(it.kode)) }
    val fordeler = BeregningsFordeler(identitet, klassekoder, Beregningsmotor(Ytelsesmotor(identitet)))
    val kontekst =
        fordeler.fordel(
            input =
                FordelingInput(
                    konto = "konto1",
                    gjelder = data.kontekst.gjelder,
                    beregnet = data.kontekst.beregnet,
                    utbetalesTil = data.kontekst.utbetalesTil,
                    status = BeregningsStatus.BEREGNET,
                    ekstraInfo = mutableListOf(),
                    overfoeringsdato = data.kontekst.overfoeringsdato,
                ),
            periode = data.kontekst.periode,
            oppdrag = oppdrag,
            trekk = data.trekk,
            skatteKontekst = data.skatteKontekst,
            relaterteBeregninger = data.relaterteBeregninger,
            ledigFrikortBeloep = data.kontekst.ledigFrikortBeloep,
        )
    return Tidslinjeberegning(oppdrag, identitet, kontekst)
}
