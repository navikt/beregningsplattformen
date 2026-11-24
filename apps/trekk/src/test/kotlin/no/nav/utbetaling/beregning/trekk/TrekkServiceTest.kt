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

import no.nav.utbetaling.beregning.trekk.domain.SatsType
import no.nav.utbetaling.beregning.trekk.domain.TrekkFagomraade
import no.nav.utbetaling.beregning.trekk.domain.Trekkperiode
import no.nav.utbetaling.beregning.trekk.domain.Trekkstatus
import no.nav.utbetaling.beregning.trekk.dto.Fagomraade
import no.nav.utbetaling.beregning.trekk.dto.Fagomraader
import no.nav.utbetaling.beregning.trekk.dto.Periode
import no.nav.utbetaling.beregning.trekk.dto.Perioder
import no.nav.utbetaling.beregning.trekk.dto.Trekk
import no.nav.utbetaling.beregning.trekk.repository.TrekkvedtakRepository
import no.nav.utbetaling.beregning.trekk.utils.SqlUtils.transaction

internal class TrekkServiceTest :
    FunSpec({
        extensions(PostgresTestDatabase)

        val dataSource = PostgresTestDatabase.dataSource
        val repository = TrekkvedtakRepository()
        val service = TrekkService(dataSource = dataSource)

        test("persisterer et trekkvedtak med verdier i alle felt") {
            val trekk = lagTrekk()
            val trekkJson = """{"trekk"}"""
            val tidspktFoerMottak = LocalDateTime.now().minusSeconds(1)

            service.createOrUpdateTrekkvedtakFromOs(trekkJson, trekk)

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

            service.createOrUpdateTrekkvedtakFromOs("""{"trekk med flere perioder og fagomraader"}""", trekk)

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

        SatsType.entries.forEach { satsType ->
            test("persisterer satstype $satsType fra kodeTrekkAlternativ") {
                val trekk = lagTrekk().copy(kodeTrekkAlternativ = "LOP${satsType.name}")

                service.createOrUpdateTrekkvedtakFromOs("satstypemapping", trekk)

                val persistert =
                    dataSource
                        .transaction {
                            repository.findByEksternTrekkId(trekk.trekkvedtakId!!, it)
                        }.shouldNotBeNull()
                persistert.trekkperioder.single().satstype shouldBe satsType
            }
        }

        test("ruller tilbake naar trekktype er ugyldig") {
            shouldThrow<IllegalArgumentException> {
                service.createOrUpdateTrekkvedtakFromOs("invalid trekkalternativ", lagTrekk().copy(kodeTrekkAlternativ = "LOPX"))
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
