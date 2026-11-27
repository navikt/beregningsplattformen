package no.nav.utbetaling.beregning.trekk

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import javax.sql.DataSource

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import org.postgresql.util.PSQLException

import no.nav.utbetaling.beregning.trekk.domain.SatsType
import no.nav.utbetaling.beregning.trekk.domain.TrekkFagomraade
import no.nav.utbetaling.beregning.trekk.domain.Trekkperiode
import no.nav.utbetaling.beregning.trekk.domain.Trekkstatus
import no.nav.utbetaling.beregning.trekk.dto.Fagomraade
import no.nav.utbetaling.beregning.trekk.dto.Fagomraader
import no.nav.utbetaling.beregning.trekk.dto.Periode
import no.nav.utbetaling.beregning.trekk.dto.Perioder
import no.nav.utbetaling.beregning.trekk.dto.Trekk
import no.nav.utbetaling.beregning.trekk.listener.PostgresListener
import no.nav.utbetaling.beregning.trekk.repository.TrekkvedtakRepository
import no.nav.utbetaling.beregning.trekk.utils.SqlUtils.transaction

internal class TrekkServiceTest :
    FunSpec({
        extensions(PostgresListener)

        val dataSource = PostgresListener.dataSource
        val repository = TrekkvedtakRepository()
        val service = TrekkService(dataSource = dataSource)

        fun hentVedtak(trekk: Trekk) =
            dataSource
                .transaction { repository.findByEksternTrekkId(trekk.trekkvedtakId!!, it) }
                .shouldNotBeNull()

        fun oppdaterVedtak(trekk: Trekk): Trekk = dataSource.transaction { transaksjon -> service.oppdaterTrekkvedtak(trekk, transaksjon) }

        fun hentReberegningstidspunkt(trekk: Trekk): LocalDateTime? =
            dataSource.transaction { transaksjon ->
                transaksjon.single(
                    queryOf(
                        "SELECT tidspkt_reberegning FROM trekkvedtak WHERE ekstern_trekk_id = :eksternId",
                        mapOf("eksternId" to trekk.trekkvedtakId),
                    ),
                ) { rad -> rad.localDateTimeOrNull("tidspkt_reberegning") }
            }

        test("persisterer et trekkvedtak med verdier i alle felt") {
            val trekk = lagTrekk()
            val trekkJson = """{"trekk"}"""
            val tidspktFoerMottak = LocalDateTime.now().minusSeconds(1)

            service.lagreTrekkvedtakBevis(trekkJson)
            service.lagEllerOppdaterTrekkvedtak(trekk)

            val persistert =
                dataSource
                    .transaction {
                        repository.findByEksternTrekkId(trekk.trekkvedtakId!!, it)
                    }.shouldNotBeNull()

            (persistert.id.shouldNotBeNull().id > 0) shouldBe true
            persistert.eksternTrekkId shouldBe trekk.trekkvedtakId
            persistert.trekktypeKode shouldBe trekk.kodeTrekktype
            persistert.kreditorIdTss shouldBe trekk.kreditorIdTss
            persistert.kreditorTrekkId shouldBe trekk.kreditorTrekkId
            persistert.kreditorRef shouldBe trekk.kreditorRef
            persistert.kreditorKid shouldBe trekk.kreditorKid
            persistert.debitorId shouldBe trekk.debitorId
            persistert.belopSaldo shouldBe trekk.belopSaldo
            persistert.datoPriFom shouldBe trekk.datoPriFom
            persistert.gyldigTom shouldBe trekk.datoGyldigTom
            persistert.ansvarligEnhet shouldBe trekk.enhet
            persistert.kilde shouldBe trekk.kilde
            persistert.trekkstatus shouldBe Trekkstatus.LOPE
            persistert.bruker shouldBe trekk.brukerId
            persistert.tidspktReg shouldBe regTidspkt
            persistert.mottatt.isAfter(tidspktFoerMottak) shouldBe true
            persistert.mottatt.isBefore(LocalDateTime.now().plusSeconds(1)) shouldBe true

            val periode = persistert.trekkperioder.single()
            (periode.id.shouldNotBeNull().id > 0) shouldBe true
            periode shouldBe
                Trekkperiode(
                    id = periode.id,
                    trekkvedtakId = persistert.id.id,
                    datoFom = LocalDate.of(2026, 11, 1),
                    datoTom = LocalDate.of(2026, 12, 31),
                    sats = BigDecimal("100.00"),
                    satstype = SatsType.M,
                    feilReg = false,
                    bruker = "brukerPeriode",
                    tidspktReg = regTidspkt,
                )
            val fagomraade = persistert.fagomraader.single()
            (fagomraade.id.shouldNotBeNull().id > 0) shouldBe true
            fagomraade shouldBe
                TrekkFagomraade(
                    id = fagomraade.id,
                    trekkvedtakId = persistert.id.id,
                    fagomraadeKode = "AAP",
                    aktiv = true,
                    tidspktReg = regTidspkt,
                )
            dataSource.history() shouldBe listOf(trekkJson)
            dataSource.rowCounts() shouldBe listOf(1, 1, 1, 1)
        }

        test("persisterer bare perioder og fagomraader med lopenummer 9999") {
            val opprinneligTrekk = lagTrekk()
            val gjeldendePeriode = opprinneligTrekk.perioder.periode.single()
            val gjeldendeFagomraade = opprinneligTrekk.fagomraader.fagomraade.single()

            val trekk =
                opprinneligTrekk.copy(
                    perioder =
                        Perioder(
                            mutableListOf(
                                gjeldendePeriode.copy(lopenrPeriode = 1, sats = BigDecimal("200.00")),
                                gjeldendePeriode,
                                gjeldendePeriode.copy(
                                    periodeFomDato = LocalDate.of(2027, 1, 1),
                                    periodeTomDato = null,
                                ),
                            ),
                        ),
                    fagomraader =
                        Fagomraader(
                            mutableListOf(
                                gjeldendeFagomraade.copy(lopenrFagomraade = 1, kodeFagomraade = "PEN"),
                                gjeldendeFagomraade,
                                gjeldendeFagomraade.copy(kodeFagomraade = "UFOR"),
                            ),
                        ),
                )

            service.lagreTrekkvedtakBevis("""{"trekk med flere perioder og fagomraader"}""")
            service.lagEllerOppdaterTrekkvedtak(trekk)

            val persistert =
                dataSource
                    .transaction {
                        repository.findByEksternTrekkId(trekk.trekkvedtakId!!, it)
                    }.shouldNotBeNull()

            persistert.trekkperioder.sortedBy { it.datoFom }.map { Triple(it.datoFom, it.datoTom, it.sats) } shouldBe
                listOf(
                    Triple(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 31), BigDecimal("100.00")),
                    Triple(LocalDate.of(2027, 1, 1), null, BigDecimal("100.00")),
                )
            persistert.fagomraader.map { it.fagomraadeKode }.toSet() shouldBe setOf("AAP", "UFOR")
            persistert.trekkperioder.all { it.trekkvedtakId == persistert.id.shouldNotBeNull().id } shouldBe true
            persistert.fagomraader.all { it.trekkvedtakId == persistert.id.shouldNotBeNull().id } shouldBe true
            dataSource.rowCounts() shouldBe listOf(1, 2, 2, 1)
        }

        test("oppdaterer trekkvedtaksfeltene og returnerer trekk med eksisterende intern ID") {
            val opprinneligTrekk = lagTrekk()
            service.lagEllerOppdaterTrekkvedtak(opprinneligTrekk)
            val opprinneligVedtak = hentVedtak(opprinneligTrekk)
            val endretTrekk =
                opprinneligTrekk.copy(
                    kodeTrekktype = "ENDRET",
                    kreditorIdTss = "nyKreditor",
                    kreditorTrekkId = "nyKreditorTrekkId",
                    kreditorRef = "nyReferanse",
                    kreditorKid = "nyKid",
                    debitorId = "nyDebitor",
                    belopSaldo = BigDecimal("250.00"),
                    datoPriFom = LocalDate.of(2026, 10, 1),
                    datoGyldigTom = LocalDate.of(2027, 12, 31),
                    trekkstatus = "HVIL",
                    enhet = "nyEnhet",
                    kilde = "nyKilde",
                    brukerId = "nyBruker",
                    tidsPktReg = regTidspkt.plusHours(1),
                )
            val tidspunktFoerOppdatering = LocalDateTime.now().minusSeconds(1)

            val opprinneligReberegningstidspunkt = hentReberegningstidspunkt(opprinneligTrekk).shouldNotBeNull()
            val resultat = oppdaterVedtak(endretTrekk)
            val oppdatertVedtak = hentVedtak(endretTrekk)

            resultat shouldBe endretTrekk.copy(id = opprinneligVedtak.id.shouldNotBeNull().id)
            oppdatertVedtak.id shouldBe opprinneligVedtak.id
            oppdatertVedtak.eksternTrekkId shouldBe opprinneligTrekk.trekkvedtakId
            oppdatertVedtak.trekktypeKode shouldBe endretTrekk.kodeTrekktype
            oppdatertVedtak.kreditorIdTss shouldBe endretTrekk.kreditorIdTss
            oppdatertVedtak.kreditorTrekkId shouldBe endretTrekk.kreditorTrekkId
            oppdatertVedtak.kreditorRef shouldBe endretTrekk.kreditorRef
            oppdatertVedtak.kreditorKid shouldBe endretTrekk.kreditorKid
            oppdatertVedtak.debitorId shouldBe endretTrekk.debitorId
            oppdatertVedtak.belopSaldo shouldBe endretTrekk.belopSaldo
            oppdatertVedtak.datoPriFom shouldBe endretTrekk.datoPriFom
            oppdatertVedtak.gyldigTom shouldBe endretTrekk.datoGyldigTom
            oppdatertVedtak.trekkstatus shouldBe Trekkstatus.HVIL
            oppdatertVedtak.ansvarligEnhet shouldBe endretTrekk.enhet
            oppdatertVedtak.kilde shouldBe endretTrekk.kilde
            oppdatertVedtak.bruker shouldBe endretTrekk.brukerId
            oppdatertVedtak.tidspktReg shouldBe endretTrekk.tidsPktReg
            oppdatertVedtak.mottatt.isAfter(tidspunktFoerOppdatering) shouldBe true
            val reberegningstidspunkt = hentReberegningstidspunkt(endretTrekk).shouldNotBeNull()
            reberegningstidspunkt.isAfter(opprinneligReberegningstidspunkt) shouldBe true
            reberegningstidspunkt.isAfter(tidspunktFoerOppdatering) shouldBe true
            reberegningstidspunkt.isBefore(LocalDateTime.now().plusSeconds(1)) shouldBe true
            oppdatertVedtak.trekkperioder shouldBe opprinneligVedtak.trekkperioder
            oppdatertVedtak.fagomraader shouldBe opprinneligVedtak.fagomraader
            dataSource.rowCounts() shouldBe listOf(1, 1, 1, 0)
        }

        test("legger til, oppdaterer og fjerner perioder og fagområder uten å endre andre trekkvedtak") {
            val grunnlag = lagTrekk()
            val gjeldendePeriode = grunnlag.perioder.periode.single()
            val gjeldendeFagomraade = grunnlag.fagomraader.fagomraade.single()
            val opprinneligTrekk =
                grunnlag.copy(
                    perioder =
                        Perioder(
                            mutableListOf(
                                gjeldendePeriode,
                                gjeldendePeriode.copy(
                                    periodeFomDato = LocalDate.of(2027, 1, 1),
                                    periodeTomDato = LocalDate.of(2027, 12, 31),
                                ),
                            ),
                        ),
                    fagomraader =
                        Fagomraader(
                            mutableListOf(
                                gjeldendeFagomraade,
                                gjeldendeFagomraade.copy(kodeFagomraade = "PEN"),
                            ),
                        ),
                )
            val annetTrekk = opprinneligTrekk.copy(trekkvedtakId = 43L)
            service.lagEllerOppdaterTrekkvedtak(opprinneligTrekk)
            service.lagEllerOppdaterTrekkvedtak(annetTrekk)
            val opprinneligTrekkvedtak = hentVedtak(opprinneligTrekk)
            val annetTrekkvedtak = hentVedtak(annetTrekk)
            val annetReberegningstidspunkt = hentReberegningstidspunkt(annetTrekk)
            val endretTrekk =
                opprinneligTrekk.copy(
                    kodeTrekkAlternativ = "LOPP",
                    perioder =
                        Perioder(
                            mutableListOf(
                                gjeldendePeriode.copy(
                                    sats = BigDecimal("25.00"),
                                    feilRegPeriode = "J",
                                    brukerIdPeriode = "nyPeriodebruker",
                                    tidsPktRegPeriode = regTidspkt.plusHours(1),
                                ),
                                gjeldendePeriode.copy(
                                    periodeFomDato = LocalDate.of(2028, 1, 1),
                                    periodeTomDato = null,
                                    sats = BigDecimal("50.00"),
                                ),
                                gjeldendePeriode.copy(periodeFomDato = LocalDate.of(2030, 1, 1), lopenrPeriode = 1),
                            ),
                        ),
                    fagomraader =
                        Fagomraader(
                            mutableListOf(
                                gjeldendeFagomraade.copy(
                                    feilRegFagomraade = "J",
                                    tidsPktRegFagomraade = regTidspkt.plusHours(1),
                                ),
                                gjeldendeFagomraade.copy(kodeFagomraade = "UFOR"),
                                gjeldendeFagomraade.copy(kodeFagomraade = "HIST", lopenrFagomraade = 1),
                            ),
                        ),
                )

            oppdaterVedtak(endretTrekk)

            val oppdatertVedtak = hentVedtak(endretTrekk)
            val oppdatertPeriode = oppdatertVedtak.trekkperioder.single { it.datoFom == gjeldendePeriode.periodeFomDato }
            oppdatertPeriode shouldBe
                opprinneligTrekkvedtak.trekkperioder
                    .single { it.datoFom == gjeldendePeriode.periodeFomDato }
                    .copy(
                        sats = BigDecimal("25.00"),
                        satstype = SatsType.P,
                        feilReg = true,
                        bruker = "nyPeriodebruker",
                        tidspktReg = regTidspkt.plusHours(1),
                    )
            val nyPeriode = oppdatertVedtak.trekkperioder.single { it.datoFom == LocalDate.of(2028, 1, 1) }
            nyPeriode.datoTom shouldBe null
            nyPeriode.sats shouldBe BigDecimal("50.00")
            nyPeriode.satstype shouldBe SatsType.P
            nyPeriode.feilReg shouldBe false
            nyPeriode.trekkvedtakId shouldBe opprinneligTrekkvedtak.id.shouldNotBeNull().id
            oppdatertVedtak.trekkperioder.size shouldBe 2
            oppdatertVedtak.fagomraader.map { it.fagomraadeKode }.toSet() shouldBe setOf("AAP", "UFOR")
            oppdatertVedtak.fagomraader.single { it.fagomraadeKode == "AAP" } shouldBe
                opprinneligTrekkvedtak.fagomraader
                    .single { it.fagomraadeKode == "AAP" }
                    .copy(aktiv = false, tidspktReg = regTidspkt.plusHours(1))
            val nyttFagomraade = oppdatertVedtak.fagomraader.single { it.fagomraadeKode == "UFOR" }
            nyttFagomraade.aktiv shouldBe true
            nyttFagomraade.trekkvedtakId shouldBe opprinneligTrekkvedtak.id.shouldNotBeNull().id
            val annetVedtakEtterOppdatering = hentVedtak(annetTrekk)
            annetVedtakEtterOppdatering.trekkperioder shouldBe annetTrekkvedtak.trekkperioder
            annetVedtakEtterOppdatering.fagomraader shouldBe annetTrekkvedtak.fagomraader
            annetVedtakEtterOppdatering.mottatt shouldBe annetTrekkvedtak.mottatt
            hentReberegningstidspunkt(annetTrekk) shouldBe annetReberegningstidspunkt
            dataSource.rowCounts() shouldBe listOf(2, 4, 4, 0)
        }

        listOf("åpen" to null, "lukket" to LocalDate.of(2026, 12, 31)).forEach { (periodetype, sluttdato) ->
            test("oppdaterer en $periodetype periode uten å opprette en ny rad") {
                val grunnlag = lagTrekk()
                val periode =
                    grunnlag.perioder.periode
                        .single()
                        .copy(periodeTomDato = sluttdato)
                val opprinneligTrekk = grunnlag.copy(perioder = Perioder(mutableListOf(periode)))
                service.lagEllerOppdaterTrekkvedtak(opprinneligTrekk)
                val opprinneligPeriode = hentVedtak(opprinneligTrekk).trekkperioder.single()
                val endretTrekk =
                    opprinneligTrekk.copy(
                        kodeTrekkAlternativ = "LOPD",
                        perioder =
                            Perioder(
                                mutableListOf(
                                    periode.copy(
                                        sats = BigDecimal("200.00"),
                                        feilRegPeriode = "J",
                                        brukerIdPeriode = "endretBruker",
                                        tidsPktRegPeriode = regTidspkt.plusHours(1),
                                    ),
                                ),
                            ),
                    )

                oppdaterVedtak(endretTrekk)

                hentVedtak(endretTrekk).trekkperioder.single() shouldBe
                    opprinneligPeriode.copy(
                        sats = BigDecimal("200.00"),
                        satstype = SatsType.D,
                        feilReg = true,
                        bruker = "endretBruker",
                        tidspktReg = regTidspkt.plusHours(1),
                    )
                dataSource.rowCounts() shouldBe listOf(1, 1, 1, 0)
            }
        }

        test("avviser oppdatering av et trekkvedtak som ikke finnes") {
            val trekk = lagTrekk()

            val feil = shouldThrow<IllegalStateException> { oppdaterVedtak(trekk) }

            feil.message shouldBe
                "Kunne ikke finne trekkvedtak for eksternTrekkId ${trekk.trekkvedtakId} og debitorId ${trekk.debitorId}"
            dataSource.rowCounts() shouldBe listOf(0, 0, 0, 0)
        }

        test("ruller tilbake trekkvedtaksendringer og nye perioder når en senere periode ikke kan lagres") {
            val opprinneligTrekk = lagTrekk()
            service.lagEllerOppdaterTrekkvedtak(opprinneligTrekk)
            val opprinneligTrekkvedtak = hentVedtak(opprinneligTrekk)
            val endretTrekkvedtakBevis = """{"endring":"ugyldig sats"}"""
            val opprinneligReberegningstidspunkt = hentReberegningstidspunkt(opprinneligTrekk)
            service.lagreTrekkvedtakBevis(endretTrekkvedtakBevis)
            val periode = opprinneligTrekk.perioder.periode.single()
            val endretTrekk =
                opprinneligTrekk.copy(
                    kreditorRef = "endretReferanse",
                    perioder =
                        Perioder(
                            mutableListOf(
                                periode,
                                periode.copy(
                                    periodeFomDato = LocalDate.of(2026, 1, 1),
                                    periodeTomDato = LocalDate.of(2026, 12, 31),
                                ),
                                periode.copy(
                                    periodeFomDato = LocalDate.of(2027, 1, 1),
                                    periodeTomDato = null,
                                    sats = BigDecimal("1000000000.00"), // > max NUMERIC(11,2)
                                ),
                            ),
                        ),
                )

            val feil = shouldThrow<PSQLException> { oppdaterVedtak(endretTrekk) }

            feil.sqlState shouldBe "22003" // numeric_value_out_of_range
            val vedtakEtterFeil = hentVedtak(opprinneligTrekk)
            vedtakEtterFeil.kreditorRef shouldBe opprinneligTrekkvedtak.kreditorRef
            vedtakEtterFeil.mottatt shouldBe opprinneligTrekkvedtak.mottatt
            vedtakEtterFeil.trekkperioder shouldBe opprinneligTrekkvedtak.trekkperioder
            vedtakEtterFeil.fagomraader shouldBe opprinneligTrekkvedtak.fagomraader
            hentReberegningstidspunkt(opprinneligTrekk) shouldBe opprinneligReberegningstidspunkt
            dataSource.history() shouldBe listOf(endretTrekkvedtakBevis)
            dataSource.rowCounts() shouldBe listOf(1, 1, 1, 1)
        }

        SatsType.entries.forEach { satsType ->
            test("persisterer satstype $satsType fra kodeTrekkAlternativ") {
                val trekk = lagTrekk().copy(kodeTrekkAlternativ = "LOP${satsType.name}")

                service.lagEllerOppdaterTrekkvedtak(trekk)

                val persistert =
                    dataSource
                        .transaction {
                            repository.findByEksternTrekkId(trekk.trekkvedtakId!!, it)
                        }.shouldNotBeNull()
                persistert.trekkperioder.single().satstype shouldBe satsType
            }
        }

        test("ruller tilbake når trekktype er ugyldig") {
            shouldThrow<IllegalArgumentException> {
                service.lagEllerOppdaterTrekkvedtak(lagTrekk().copy(kodeTrekkAlternativ = "LOPX"))
            }

            dataSource.rowCounts() shouldBe listOf(0, 0, 0, 0)
        }
    })

private val regTidspkt = LocalDateTime.of(2026, 11, 23, 12, 0, 0)

private fun lagTrekk(): Trekk =
    Trekk(
        trekkvedtakId = 42L,
        kodeTrekktype = "TEST",
        kreditorIdTss = "testKreditor",
        kreditorTrekkId = "testKreditorTrekkId",
        kreditorRef = "testKreditorRef",
        kreditorKid = "testKreditorKid",
        debitorId = "testDebitor",
        kodeTrekkAlternativ = "LOPM",
        belopSaldo = BigDecimal("100.00"),
        datoPriFom = LocalDate.of(2026, 11, 1),
        datoGyldigTom = LocalDate.of(2026, 12, 31),
        trekkstatus = "LOPE",
        enhet = "testEnhet",
        kilde = "testKilde",
        brukerId = "testBruker",
        tidsPktReg = regTidspkt,
        perioder =
            Perioder(
                mutableListOf(
                    Periode(
                        periodeFomDato = LocalDate.of(2026, 11, 1),
                        periodeTomDato = LocalDate.of(2026, 12, 31),
                        sats = BigDecimal("100.00"),
                        lopenrPeriode = 9999,
                        feilRegPeriode = "N",
                        brukerIdPeriode = "brukerPeriode",
                        tidsPktRegPeriode = regTidspkt,
                    ),
                ),
            ),
        fagomraader =
            Fagomraader(
                mutableListOf(
                    Fagomraade(
                        kodeFagomraade = "AAP",
                        lopenrFagomraade = 9999,
                        feilRegFagomraade = "N",
                        tidsPktRegFagomraade = regTidspkt,
                    ),
                ),
            ),
    )

private fun DataSource.history(): List<String> =
    transaction { session ->
        session.list(queryOf("SELECT trekkvedtak FROM trekkvedtak_historikk ORDER BY id")) { it.string("trekkvedtak") }
    }

private fun DataSource.rowCounts(): List<Int> =
    transaction { session ->
        session
            .single(
                queryOf(
                    """
                    SELECT (SELECT COUNT(*) FROM trekkvedtak) AS vedtak,
                           (SELECT COUNT(*) FROM trekkperiode) AS perioder,
                           (SELECT COUNT(*) FROM trekk_i_fagomraade) AS fagomraader,
                           (SELECT COUNT(*) FROM trekkvedtak_historikk) AS historikk
                    """.trimIndent(),
                ),
            ) {
                listOf(it.int("vedtak"), it.int("perioder"), it.int("fagomraader"), it.int("historikk"))
            }.shouldNotBeNull()
    }
