package no.nav.utbetaling.beregning.oppdrag.intern

import java.math.BigDecimal

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

import no.nav.utbetaling.beregning.oppdrag.Opplysning
import no.nav.utbetaling.beregning.oppdrag.Opplysninger
import no.nav.utbetaling.beregning.oppdrag.Part
import no.nav.utbetaling.beregning.oppdrag.Personidentifikator
import no.nav.utbetaling.beregning.oppdrag.Refusjonsinfo
import no.nav.utbetaling.beregning.oppdrag.SaksbehandlerId
import no.nav.utbetaling.beregning.oppdrag.Sats
import no.nav.utbetaling.beregning.oppdrag.TypeSøknad
import no.nav.utbetaling.beregning.oppdrag.YtelseSatstype
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.assertGjenskaptfraTidslinjer
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.linje
import no.nav.utbetaling.beregning.oppdrag.hjelpefunksjoner.oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.xml.Vedtakssats157
import no.nav.utbetaling.beregning.tid.DefaultTidslinjesamling
import no.nav.utbetaling.beregning.tid.Tidslinje
import no.nav.utbetaling.beregning.tid.januar
import no.nav.utbetaling.beregning.tid.til

class BeregningssatserTest :
    FunSpec({
        val opprinneligLinje =
            linje(delytelseId = "1", fom = 1.januar, tom = 31.januar, sats = 100)
                .copy(saksbehId = "LINJE1", vedtakssats157 = Vedtakssats157(BigDecimal("120")))

        test("Beholder satstype, sats og vedtakssats med saksbehandler fra linjen") {
            val resultat = oppdragstidslinje(saksbehandlerId = "OPPDRAG", linjer = listOf(opprinneligLinje))
            val opplysninger =
                resultat.tidslinjer.values
                    .single()
                    .single()
                    .second

            opplysninger.sats shouldBe Opplysning(SaksbehandlerId("LINJE1"), Sats(BigDecimal("100"), YtelseSatstype.DAG.kode))
            opplysninger.vedtaksSats shouldBe Opplysning(SaksbehandlerId("LINJE1"), Sats(BigDecimal("120"), ""))
            resultat.assertGjenskaptfraTidslinjer()
        }

        test("Manglende vedtakssats utledes ikke fra sats") {
            val resultat = oppdragstidslinje(linjer = listOf(opprinneligLinje.copy(vedtakssats157 = null)))

            resultat.tidslinjer.values
                .single()
                .single()
                .second.vedtaksSats shouldBe null
            resultat.assertGjenskaptfraTidslinjer()
        }

        test("Endret satstype og vedtakssats får ny proveniens selv om satsbeløpet er uendret") {
            val opprinnelig = oppdragstidslinje(fagsystemId = "FAGSYSTEM", linjer = listOf(opprinneligLinje))
            val endring =
                oppdragstidslinje(
                    fagsystemId = "FAGSYSTEM",
                    linjer =
                        listOf(
                            opprinneligLinje.copy(
                                delytelseId = "2",
                                refDelytelseId = "1",
                                saksbehId = "LINJE2",
                                typeSats = "UKE",
                                vedtakssats157 = Vedtakssats157(BigDecimal("140")),
                            ),
                        ),
                )
            val resultat = opprinnelig + endring
            val opplysninger =
                resultat.tidslinjer.values
                    .single()
                    .single()
                    .second

            opplysninger.sats shouldBe Opplysning(SaksbehandlerId("LINJE2"), Sats(BigDecimal("100"), YtelseSatstype.UKE.kode))
            opplysninger.vedtaksSats shouldBe Opplysning(SaksbehandlerId("LINJE2"), Sats(BigDecimal("140"), ""))
            resultat.assertGjenskaptfraTidslinjer()
        }

        test("Uendrede satser beholder opprinnelig proveniens ved ny saksbehandler") {
            val opprinnelig = oppdragstidslinje(fagsystemId = "FAGSYSTEM", linjer = listOf(opprinneligLinje))
            val endring =
                oppdragstidslinje(
                    fagsystemId = "FAGSYSTEM",
                    linjer = listOf(opprinneligLinje.copy(delytelseId = "2", refDelytelseId = "1", saksbehId = "LINJE2")),
                )
            val opplysninger =
                (opprinnelig + endring)
                    .tidslinjer.values
                    .single()
                    .single()
                    .second

            opplysninger.sats shouldBe Opplysning(SaksbehandlerId("LINJE1"), Sats(BigDecimal("100"), YtelseSatstype.DAG.kode))
            opplysninger.vedtaksSats shouldBe Opplysning(SaksbehandlerId("LINJE1"), Sats(BigDecimal("120"), ""))
        }

        test("Oppdatering i samme kjede uten vedtakssats arver opprinnelig verdi og proveniens") {
            val opprinnelig = oppdragstidslinje(fagsystemId = "FAGSYSTEM", linjer = listOf(opprinneligLinje))
            val endring =
                oppdragstidslinje(
                    fagsystemId = "FAGSYSTEM",
                    linjer =
                        listOf(
                            opprinneligLinje.copy(
                                delytelseId = "2",
                                refDelytelseId = "1",
                                saksbehId = "LINJE2",
                                vedtakssats157 = null,
                            ),
                        ),
                )
            val opplysninger =
                (opprinnelig + endring)
                    .tidslinjer.values
                    .single()
                    .single()
                    .second

            opplysninger.vedtaksSats shouldBe Opplysning(SaksbehandlerId("LINJE1"), Sats(BigDecimal("120"), ""))
        }

        test("Perioder med kun ulik vedtakssats komprimeres ikke") {
            val opprinnelig = oppdragstidslinje(linjer = listOf(opprinneligLinje))
            val kjedeId = opprinnelig.tidslinjer.keys.single()
            val opplysninger =
                opprinnelig.tidslinjer.values
                    .single()
                    .single()
                    .second
            val endret = opplysninger.copy(vedtaksSats = Opplysning(SaksbehandlerId("LINJE1"), Sats(BigDecimal("140"), "")))
            val tidslinje =
                Tidslinje
                    .Builder<Opplysninger>()
                    .periode(1.januar til 15.januar, opplysninger)
                    .periode(16.januar til 31.januar, endret)
                    .build()
            val resultat = FraTidslinjesamling.opprett(DefaultTidslinjesamling(mapOf(kjedeId to tidslinje)))

            resultat.tidslinjer.values
                .single()
                .toList() shouldBe
                listOf(1.januar til 15.januar to opplysninger, 16.januar til 31.januar to endret)
            resultat.assertGjenskaptfraTidslinjer()
        }

        listOf(null, "120", "140").forEach { nyVedtakssats ->
            test("Sammensatt sats og vedtakssats $nyVedtakssats bevarer daterte kildefelt gjennom gjenskaping og fletting") {
                val opprinnelig =
                    oppdragstidslinje(
                        fagsystemId = "FAGSYSTEM",
                        linjer =
                            listOf(
                                linje(
                                    delytelseId = "1",
                                    fom = 1.januar,
                                    tom = 31.januar,
                                    sats = 100,
                                    skyldnerId = "12345678901",
                                    datoSkyldnerFom = 10.januar,
                                    refunderesId = "12345678902",
                                    refusjonDatoFom = 20.januar,
                                    refusjonMaksDato = 31.januar,
                                    typeSoknad = "NY",
                                ).copy(saksbehId = "LINJE1", vedtakssats157 = Vedtakssats157(BigDecimal("120"))),
                            ),
                    )
                opprinnelig.assertGjenskaptfraTidslinjer()
                val gjenskapt = FraTidslinjesamling.opprett(opprinnelig)
                val endring =
                    oppdragstidslinje(
                        fagsystemId = "FAGSYSTEM",
                        linjer =
                            listOf(
                                opprinneligLinje.copy(
                                    delytelseId = "2",
                                    refDelytelseId = "1",
                                    saksbehId = "LINJE2",
                                    typeSats = "UKE",
                                    vedtakssats157 = nyVedtakssats?.let { Vedtakssats157(BigDecimal(it)) },
                                ),
                            ),
                    )
                val resultat = gjenskapt + endring
                val vedtakSaksbehandler = SaksbehandlerId(if (nyVedtakssats == "140") "LINJE2" else "LINJE1")
                (1..31).forEach { dag ->
                    val opplysninger = resultat[dag.januar].values.single()
                    opplysninger.sats shouldBe Opplysning(SaksbehandlerId("LINJE2"), Sats(BigDecimal("100"), "UKE"))
                    opplysninger.vedtaksSats shouldBe Opplysning(vedtakSaksbehandler, Sats(BigDecimal(nyVedtakssats ?: "120"), ""))
                    opplysninger.typeSøknad shouldBe Opplysning(SaksbehandlerId("LINJE1"), TypeSøknad("NY"))
                    opplysninger.skyldner shouldBe
                        if (dag < 10) null else Opplysning(SaksbehandlerId("LINJE1"), Part.Person(Personidentifikator("12345678901")))
                    opplysninger.refusjonsinfo shouldBe
                        if (dag < 20) {
                            null
                        } else {
                            Opplysning(
                                SaksbehandlerId("LINJE1"),
                                Refusjonsinfo(31.januar, Part.Person(Personidentifikator("12345678902")), 20.januar),
                            )
                        }
                }
                resultat.assertGjenskaptfraTidslinjer()
            }
        }

        mapOf(
            "DAG" to YtelseSatstype.DAG,
            "UKE" to YtelseSatstype.UKE,
            "MND" to YtelseSatstype.MND,
            "14D" to YtelseSatstype.FJORTEN_DAGER,
            "AAR" to YtelseSatstype.AAR,
            "ENG" to YtelseSatstype.ENG,
            "AKTO" to YtelseSatstype.AKTO,
        ).forEach { (kode, forventet) ->
            test("Tolker og beholder kildens satstype $kode") {
                YtelseSatstype.fraXml(kode) shouldBe forventet
                forventet.kode shouldBe kode
                val resultat = oppdragstidslinje(linjer = listOf(opprinneligLinje.copy(typeSats = kode)))

                resultat.tidslinjer.values
                    .single()
                    .single()
                    .second.sats.opplysning.satsType shouldBe forventet.kode
                resultat.assertGjenskaptfraTidslinjer()
            }
        }

        listOf("UKJENT", "dag", " DAG", "DAG ", "14DG", "").forEach { kode ->
            test("Avviser ukjent eller ikke-eksakt satstype '$kode'") {
                shouldThrow<IllegalArgumentException> { YtelseSatstype.fraXml(kode) }.message shouldBe
                    "Ukjent satstype: $kode"
                shouldThrow<IllegalArgumentException> {
                    oppdragstidslinje(linjer = listOf(opprinneligLinje.copy(typeSats = kode)))
                }.message shouldBe "Ukjent satstype: $kode"
            }
        }
    })
