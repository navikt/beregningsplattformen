package no.nav.utbetaling.beregningsflyt

import java.util.UUID

import no.nav.sokos.dare.poc.aktoer.AktoerId
import no.nav.sokos.dare.poc.oppdrag.OppdragId
import no.nav.sokos.dare.poc.oppdrag.OppdragslinjekjedeId
import no.nav.utbetaling.beregning.oppdrag.Oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.Personidentifikator
import no.nav.utbetaling.beregning.oppdrag.UtbetalesTil
import no.nav.utbetaling.beregningsflyt.identitet.Beregningsidentitet
import no.nav.utbetaling.beregningsflyt.identitet.Oppdragsnokkel

/** Fixture associations, not a production identity resolver. Each scenario has its own identity space. */
internal class IntegrasjonsIdentitet(
    kilde: Oppdragstidslinje,
) : Beregningsidentitet {
    private val oppdrag: Map<Oppdragsnokkel, OppdragId>
    private val gjelder: Map<Personidentifikator, AktoerId>
    private val mottakere: Map<UtbetalesTil, AktoerId>
    private val kildekjeder = kilde.tidslinjer.keys.toSet()
    private val resultatkjeder = linkedMapOf<UUID, OppdragslinjekjedeId>()

    init {
        val opplysninger = kilde.tidslinjer.values.flatMap { tidslinje -> tidslinje.map { it.second } }
        val nokkel = opplysninger.map { Oppdragsnokkel(it.fagområde, it.fagsystemId) }.distinct().single()
        val person = opplysninger.map { requireNotNull(it.gjelderId).opplysning }.distinct().single()
        // Original scenarios associate the sole business assignment with related-payment ID 0,
        // and the source actor (including nonnumeric aliases) with legacy actor ID 1.
        oppdrag = mapOf(nokkel to OppdragId(0))
        gjelder = mapOf(person to AktoerId(1))
        val aliaser = linkedMapOf<UtbetalesTil, AktoerId>(UtbetalesTil.Person(person) to AktoerId(1))
        opplysninger.mapNotNull { it.utbetalesTil?.opplysning }.forEach { mottaker ->
            aliaser.getOrPut(mottaker) { AktoerId(aliaser.size.toLong() + 1) }
        }
        mottakere = aliaser
    }

    override fun oppdragId(nokkel: Oppdragsnokkel): OppdragId = oppdrag.getValue(nokkel)

    override fun gjelderId(personidentifikator: Personidentifikator): AktoerId = gjelder.getValue(personidentifikator)

    override fun mottakerId(utbetalesTil: UtbetalesTil): AktoerId = mottakere.getValue(utbetalesTil)

    override fun resultatKjedeId(kjedeId: UUID): OppdragslinjekjedeId {
        require(kjedeId in kildekjeder) { "Unknown source chain: $kjedeId" }
        return resultatkjeder.getOrPut(kjedeId) { OppdragslinjekjedeId(resultatkjeder.size.toLong()) }
    }
}
