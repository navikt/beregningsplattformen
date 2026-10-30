package no.nav.sokos.dare.poc.beregning.integrasjon

import no.nav.sokos.dare.poc.aktoer.AktoerId
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Beregningsmotor
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Skattemotor
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Trekkmotor
import no.nav.sokos.dare.poc.beregning.beregningsmotor.Ytelsesmotor
import no.nav.sokos.dare.poc.beregning.dataklasser.BeregningsStatus
import no.nav.sokos.dare.poc.beregning.dto.BeregningsmotorKontekstPresentasjon
import no.nav.sokos.dare.poc.beregning.fordeler.BeregningsFordeler
import no.nav.sokos.dare.poc.beregning.fordeler.FordelingInput
import no.nav.sokos.dare.poc.oppdrag.OppdragConverter.toOppdrag
import no.nav.sokos.dare.poc.oppdrag.OppdragConverter.toOppdragslinjer
import no.nav.sokos.dare.poc.oppdrag.OppdragId
import no.nav.sokos.dare.poc.oppdrag.YtelseKlasseKode
import no.nav.sokos.dare.poc.oppdrag25.tilAvventPerioder

/** Independent, read-only legacy execution. Only the completed result crosses the oracle boundary. */
object LegacyReferanseOracle {
    fun beregn(
        scenario: IntegrasjonsScenario,
        klassekoder: List<YtelseKlasseKode>,
    ): BeregningsmotorKontekstPresentasjon {
        // Read identical fixture values separately so no mutable input or execution state is shared.
        val data = scenario.les()
        val oppdrag110 = OppdragFileRepository.les(scenario.mappe, "oppdrag.json")
        val oppslag = klassekoder.associateBy { it.kode }
        val oppdrag =
            oppdrag110.map {
                it
                    .toOppdrag(AktoerId(1), OppdragId(0))
                    .copy(avventPerioder = it.tilAvventPerioder())
                    .medOppdragslinjer(it.toOppdragslinjer(oppslag))
            }
        val fordeler = BeregningsFordeler(Beregningsmotor(Ytelsesmotor(), Trekkmotor(), Skattemotor()))
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
        return BeregningsmotorKontekstPresentasjon(kontekst)
    }
}
