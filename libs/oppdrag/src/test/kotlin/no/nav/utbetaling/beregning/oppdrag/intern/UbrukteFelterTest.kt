package no.nav.utbetaling.beregning.oppdrag.intern

import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.time.LocalDateTime

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.assertThrows

import no.nav.utbetaling.beregning.oppdrag.UgyldigInnsendingException
import no.nav.utbetaling.beregning.oppdrag.xml.BelopsGrense130
import no.nav.utbetaling.beregning.oppdrag.xml.Kid154
import no.nav.utbetaling.beregning.oppdrag.xml.Klassifikasjon152
import no.nav.utbetaling.beregning.oppdrag.xml.LinjeEnhet160
import no.nav.utbetaling.beregning.oppdrag.xml.LinjeStatus151
import no.nav.utbetaling.beregning.oppdrag.xml.Ompostering116
import no.nav.utbetaling.beregning.oppdrag.xml.Oppdrag110
import no.nav.utbetaling.beregning.oppdrag.xml.OppdragGjelder112
import no.nav.utbetaling.beregning.oppdrag.xml.OppdragsLinje150
import no.nav.utbetaling.beregning.oppdrag.xml.OppdragsStatus111
import no.nav.utbetaling.beregning.oppdrag.xml.Skyldner153
import no.nav.utbetaling.beregning.oppdrag.xml.TfradragTillegg
import no.nav.utbetaling.beregning.oppdrag.xml.TkodeArbeidsgiver
import no.nav.utbetaling.beregning.oppdrag.xml.TkodeStatus
import no.nav.utbetaling.beregning.oppdrag.xml.TkodeStatusLinje
import no.nav.utbetaling.beregning.oppdrag.xml.UtbetalesTil155
import no.nav.utbetaling.beregning.oppdrag.xml.Valuta190
import no.nav.utbetaling.beregning.tid.januar

class UbrukteFelterTest :
    FunSpec({

        test("Får feil om det er satt felter som ikke skal brukes, eller verdier som ikke skal brukes") {
            val oppdrag110 =
                Oppdrag110(
                    fagsystemId = "fagsystemId",
                    kodeAksjon = "1",
                    kodeEndring = "NY",
                    kodeFagomraade = "FOO",
                    oppdragGjelderId = "11111111111",
                    datoOppdragGjelderFom = 1.januar,
                    saksbehId = "Z999999",
                    oppdragsLinje150 =
                        listOf(
                            OppdragsLinje150(
                                delytelseId = "1",
                                kodeEndringLinje = "NY",
                                kodeKlassifik = "TODO",
                                datoVedtakFom = 1.januar,
                                sats = BigDecimal.TEN,
                                fradragTillegg = TfradragTillegg.F,
                                typeSats = "DAG",
                                saksbehId = "Z999999",
                                kodeStatusLinje = TkodeStatusLinje.OPPH,
                                // Ugyldige verdier på linjenivå
                                linjeId = BigInteger.ONE,
                                refLinjeId = BigInteger.TEN,
                                refOppdragsId = 50L,
                                linjeEnhet160 =
                                    listOf(
                                        LinjeEnhet160(
                                            typeEnhet = "BOS",
                                            datoEnhetFom = 1.januar,
                                        ),
                                    ),
                                kravhaverId = "81111111111",
                                datoKravhaverFom = 5.januar,
                                kodeArbeidsgiver = TkodeArbeidsgiver.P,
                                valuta190 =
                                    listOf(
                                        Valuta190(
                                            typeValuta = "SEK",
                                            valuta = "svenske kroner",
                                            datoValutaFom = 5.januar,
                                        ),
                                    ),
                                utbetalesTil155 =
                                    listOfNotNull(
                                        UtbetalesTil155(
                                            utbetalesTilId = "999999999",
                                            datoUtbetalesTilIdFom = 5.januar,
                                            tidspktReg = "${LocalDateTime.now()}",
                                            saksbehId = "Z999999",
                                        ),
                                    ),
                                klassifikasjon152 =
                                    listOf(
                                        Klassifikasjon152(
                                            kodeKlassifik = "Neitakk",
                                            datoKlassifikFom = 1.januar,
                                            tidspktReg = "${LocalDateTime.now()}",
                                            saksbehId = "Z999999",
                                        ),
                                    ),
                                linjeStatus151 =
                                    listOf(
                                        LinjeStatus151(
                                            kodeStatusLinje = TkodeStatusLinje.OPPH,
                                            datoStatusFom = 1.januar,
                                            tidspktReg = "${LocalDateTime.now()}",
                                            saksbehId = "Z999999",
                                        ),
                                    ),
                                kid154 =
                                    listOf(
                                        Kid154(
                                            kid = "123456789",
                                            datoKidFom = 1.januar,
                                            tidspktReg = "${LocalDateTime.now()}",
                                            saksbehId = "Z999999",
                                        ),
                                    ),
                                skyldner153 =
                                    listOf(
                                        Skyldner153(
                                            skyldnerId = "81111111111",
                                            datoSkyldnerFom = 1.januar,
                                            tidspktReg = "${LocalDateTime.now()}",
                                            saksbehId = "Z999999",
                                        ),
                                    ),
                                refFagsystemId = "en-helt-annen-verdi",
                            ),
                        ),
                    // Ugyldige verdier på oppdragsnivå
                    kodeStatus = TkodeStatus.LOPE,
                    oppdragsId = 1234,
                    ompostering116 =
                        Ompostering116(
                            omPostering = "J",
                            tidspktReg = "${LocalDateTime.now()}",
                            saksbehId = "Z999999",
                        ),
                    oppdragsStatus111 =
                        listOf(
                            OppdragsStatus111(
                                kodeStatus = TkodeStatus.OPPH,
                                datoStatusFom = 1.januar,
                                tidspktReg = "${LocalDateTime.now()}",
                                saksbehId = "Z999999",
                            ),
                        ),
                    oppdragGjelder112 =
                        listOf(
                            OppdragGjelder112(
                                oppdragGjelderId = "22222222222",
                                datoOppdragGjelderFom = LocalDate.EPOCH,
                                tidspktReg = "${LocalDateTime.now()}",
                                saksbehId = "Z999999",
                            ),
                        ),
                    belopsGrense130 =
                        listOf(
                            BelopsGrense130(
                                typeGrense = "FOO",
                                belopGrense = BigDecimal.TEN,
                                datoGrenseFom = 1.januar,
                            ),
                        ),
                )

            val ugyldigOppdragException =
                assertThrows<UgyldigInnsendingException> {
                    FraXmlFormat.opprett(oppdrag110)
                }

            val forventetFeilmelding =
                """
                oppdrags-linje-150 med delytelseId 1 har satt linje-enhet-160, dette feltet skal ikke være i bruk!
                oppdrag-110 har satt oppdrag-gjelder-112, dette skal kun være i bruk til output!
                oppdrag-110 har satt oppdrag-status-111, dette skal kun være i bruk til output!
                oppdrags-linje-150 med delytelseId 1 har satt linje-status-151, dette skal kun være i bruk til output!
                oppdrags-linje-150 med delytelseId 1 har satt klassifikasjon-152, dette skal kun være i bruk til output!
                oppdrags-linje-150 med delytelseId 1 har satt skyldner-153, dette skal kun være i bruk til output!
                oppdrags-linje-150 med delytelseId 1 har satt kid-154, dette skal kun være i bruk til output!
                oppdrags-linje-150 med delytelseId 1 har satt utbetales-til-155, dette skal kun være i bruk til output!
                oppdrag-110 har oppdragId, dette skal kun være en verdi som blir generert på stormaskin!
                oppdrags-linje-150 med delytelseId 1 har satt linjeId, dette skal kun være en verdi som blir generert på stormaskin!
                oppdrags-linje-150 med delytelseId 1 har satt refLinjeId, dette skal kun være en verdi som blir generert på stormaskin!
                oppdrags-linje-150 med delytelseId 1 har satt refOppdragsId, dette skal kun være en verdi som blir generert på stormaskin!
                oppdrag-110 har satt kodeStatus=LOPE, dette skal kun være en verdi som blir generert på stormaskin!
                oppdrag-110 har satt belops-grense-130, dette feltet skal ikke være i bruk!
                oppdrags-linje-150 med delytelseId 1 har satt kravhaverId, dette feltet skal ikke være i bruk!
                oppdrags-linje-150 med delytelseId 1 har satt datoKravhaverFom, dette feltet skal ikke være i bruk!
                oppdrags-linje-150 med delytelseId 1 har satt kodeArbeidsgiver til P, dette feltet skal ikke være i bruk!
                oppdrags-linje-150 med delytelseId 1 har satt valuta-190, dette feltet skal ikke være i bruk!
                oppdrag-110 har satt ompostering-116, dette feltet skal ikke brukes av fagsystemene!
                oppdrags-linje-150 med delytelseId 1 har satt refFagsystemId til en annen verdi enn fagsystemId i oppdrag-110!
                """.trimIndent()

            ugyldigOppdragException.message shouldBe forventetFeilmelding
        }
    })
