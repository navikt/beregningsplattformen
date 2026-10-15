package no.nav.utbetaling.beregning.oppdrag.intern

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

import no.nav.utbetaling.beregning.oppdrag.BehandlendeEnhet
import no.nav.utbetaling.beregning.oppdrag.Bostedsenhet
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.linje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.perPeriode
import no.nav.utbetaling.beregning.oppdrag.xml.Oppdrag110
import no.nav.utbetaling.beregning.oppdrag.xml.OppdragsEnhet120
import no.nav.utbetaling.beregning.tid.Periode.LukketPeriode.Companion.til
import no.nav.utbetaling.beregning.tid.januar

internal class EnhetTest :
    FunSpec({
        test("Hvis behandlendeenhet ikke er satt, så skal bostedsenhet brukes") {
            val oppdrag =
                Oppdrag110(
                    kodeAksjon = "1",
                    kodeEndring = "NY",
                    kodeFagomraade = "AAP",
                    fagsystemId = "1",
                    oppdragGjelderId = "11111111111",
                    datoOppdragGjelderFom = 1.januar,
                    saksbehId = "Z999999",
                    oppdragsEnhet120 =
                        listOf(
                            OppdragsEnhet120(
                                typeEnhet = "BEH",
                                enhet = "8020",
                                datoEnhetFom = 1.januar,
                            ),
                        ),
                    oppdragsLinje150 = listOf(linje(fom = 1.januar, tom = 14.januar)),
                )

            val resultat = FraXmlFormat.opprett(oppdrag)

            resultat.perPeriode { it.bostedsenhet.opplysning to it.behandlendeEnhet.opplysning } shouldBe
                mapOf(
                    1.januar til 14.januar to setOf(Bostedsenhet("8020") to BehandlendeEnhet("8020")),
                )
        }
    })