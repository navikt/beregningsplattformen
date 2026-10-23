package no.nav.utbetaling.beregning.oppdrag.intern

import no.nav.utbetaling.beregning.oppdrag.UgyldigInnsendingException.Companion.valider
import no.nav.utbetaling.beregning.oppdrag.xml.Oppdrag110
import no.nav.utbetaling.beregning.oppdrag.xml.OppdragsLinje150

internal object UbrukteFelter {
    fun valider(oppdrag110: Oppdrag110) {
        val feil =
            validerIngenEnheterPåLinjenivå(oppdrag110) +
                validerIngenOutputdata(oppdrag110) +
                validerIngenStormaskinGenererteFelter(oppdrag110) +
                validerKodeStatus(oppdrag110) +
                validerIngenBeløpsgrenser(oppdrag110) +
                validerIngenKravhaver(oppdrag110) +
                validerIngenKodeArbeidsgiver(oppdrag110) +
                validerIngenValutaGrense(oppdrag110) +
                validerIngenOmpostering(oppdrag110) +
                validerFagsystemIdRef(oppdrag110)

        valider(feil.isEmpty()) {
            feil.joinToString("\n")
        }
    }

    private fun validerIngenEnheterPåLinjenivå(oppdrag110: Oppdrag110) =
        oppdrag110.oppdragsLinje150
            .mapNotNull { linje ->
                "oppdrags-linje-150 med delytelseId ${linje.delytelseId} har satt linje-enhet-160, dette feltet skal ikke være i bruk!"
                    .takeIf { linje.linjeEnhet160.isNotEmpty() }
            }.toSet()

    private fun validerIngenOutputdata(oppdrag110: Oppdrag110): Set<String> {
        fun OppdragsLinje150.feilmelding(felt: String) = "oppdrags-linje-150 med delytelseId $delytelseId har satt $felt, dette skal kun være i bruk til output!"

        return buildSet {
            if (oppdrag110.oppdragGjelder112.isNotEmpty()) add("oppdrag-110 har satt oppdrag-gjelder-112, dette skal kun være i bruk til output!")
            if (oppdrag110.oppdragsStatus111.isNotEmpty()) add("oppdrag-110 har satt oppdrag-status-111, dette skal kun være i bruk til output!")

            oppdrag110.oppdragsLinje150.forEach { linje ->
                addAll(
                    listOfNotNull(
                        linje.feilmelding("linje-status-151").takeIf { linje.linjeStatus151.isNotEmpty() },
                        linje.feilmelding("klassifikasjon-152").takeIf { linje.klassifikasjon152.isNotEmpty() },
                        linje.feilmelding("skyldner-153").takeIf { linje.skyldner153.isNotEmpty() },
                        linje.feilmelding("kid-154").takeIf { linje.kid154.isNotEmpty() },
                        linje.feilmelding("utbetales-til-155").takeIf { linje.utbetalesTil155.isNotEmpty() },
                    ),
                )
            }
        }
    }

    private fun validerIngenStormaskinGenererteFelter(oppdrag110: Oppdrag110): Set<String> {
        fun OppdragsLinje150.feilmelding(felt: String) = "oppdrags-linje-150 med delytelseId $delytelseId har satt $felt, dette skal kun være en verdi som blir generert på stormaskin!"

        return buildSet {
            if (oppdrag110.oppdragsId != null) {
                add("oppdrag-110 har oppdragId, dette skal kun være en verdi som blir generert på stormaskin!")
            }

            oppdrag110.oppdragsLinje150.forEach { linje ->
                addAll(
                    listOfNotNull(
                        linje.feilmelding("linjeId").takeIf { linje.linjeId != null },
                        linje.feilmelding("refLinjeId").takeIf { linje.refLinjeId != null },
                        linje.feilmelding("refOppdragsId").takeIf { linje.refOppdragsId != null },
                    ),
                )
            }
        }
    }

    private fun validerKodeStatus(oppdrag110: Oppdrag110): Set<String> =
        oppdrag110.kodeStatus
            ?.takeIf { Statuser.oppdragstatuser[it.name] == null }
            ?.let { setOf("oppdrag-110 har satt kodeStatus=${it.name}, dette skal kun være en verdi som blir generert på stormaskin!") }
            ?: emptySet()

    private fun validerIngenBeløpsgrenser(oppdrag110: Oppdrag110): Set<String> {
        if (oppdrag110.belopsGrense130.isEmpty()) return emptySet()
        return setOf("oppdrag-110 har satt belops-grense-130, dette feltet skal ikke være i bruk!")
    }

    private fun validerIngenKravhaver(oppdrag110: Oppdrag110): Set<String> =
        oppdrag110.oppdragsLinje150
            .flatMap { linje ->
                listOfNotNull(
                    "oppdrags-linje-150 med delytelseId ${linje.delytelseId} har satt kravhaverId, dette feltet skal ikke være i bruk!"
                        .takeIf { linje.kravhaverId != null },
                    "oppdrags-linje-150 med delytelseId ${linje.delytelseId} har satt datoKravhaverFom, dette feltet skal ikke være i bruk!"
                        .takeIf { linje.datoKravhaverFom != null },
                )
            }.toSet()

    private fun validerIngenKodeArbeidsgiver(oppdrag110: Oppdrag110): Set<String> =
        oppdrag110.oppdragsLinje150
            .mapNotNull { linje ->
                "oppdrags-linje-150 med delytelseId ${linje.delytelseId} har satt kodeArbeidsgiver til ${linje.kodeArbeidsgiver?.name}, dette feltet skal ikke være i bruk!"
                    .takeIf { linje.kodeArbeidsgiver != null }
            }.toSet()

    private fun validerIngenValutaGrense(oppdrag110: Oppdrag110): Set<String> =
        oppdrag110.oppdragsLinje150
            .mapNotNull { linje ->
                "oppdrags-linje-150 med delytelseId ${linje.delytelseId} har satt valuta-190, dette feltet skal ikke være i bruk!"
                    .takeIf { linje.valuta190.isNotEmpty() }
            }.toSet()

    private fun validerIngenOmpostering(oppdrag110: Oppdrag110): Set<String> =
        oppdrag110.ompostering116
            ?.let { setOf("oppdrag-110 har satt ompostering-116, dette feltet skal ikke brukes av fagsystemene!") }
            ?: emptySet()

    private fun validerFagsystemIdRef(oppdrag110: Oppdrag110): Set<String> {
        val linjerMedRef = oppdrag110.oppdragsLinje150.filter { it.refFagsystemId != null }
        if (linjerMedRef.isEmpty()) return emptySet()

        val fagsystemId =
            oppdrag110.fagsystemId
                ?: return setOf("oppdrag-110 har ikke satt fagsystemId!")

        return linjerMedRef
            .filter { it.refFagsystemId != fagsystemId }
            .map { linje ->
                "oppdrags-linje-150 med delytelseId ${linje.delytelseId} har satt refFagsystemId til en annen verdi enn fagsystemId i oppdrag-110!"
            }.toSet()
    }
}
