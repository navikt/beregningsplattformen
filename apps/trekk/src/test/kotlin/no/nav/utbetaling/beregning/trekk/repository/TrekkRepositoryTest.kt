package no.nav.utbetaling.beregning.trekk.repository

import java.math.BigDecimal
import java.sql.Connection
import java.time.LocalDate
import java.time.LocalDateTime

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.flywaydb.core.Flyway
import org.jdbi.v3.core.Jdbi
import org.testcontainers.postgresql.PostgreSQLContainer

import no.nav.utbetaling.beregning.trekk.config.createJdbi
import no.nav.utbetaling.beregning.trekk.domain.SatsType
import no.nav.utbetaling.beregning.trekk.domain.TrekkFagomraade
import no.nav.utbetaling.beregning.trekk.domain.TrekkFagomraadeId
import no.nav.utbetaling.beregning.trekk.domain.Trekkperiode
import no.nav.utbetaling.beregning.trekk.domain.TrekkperiodeId
import no.nav.utbetaling.beregning.trekk.domain.Trekkstatus
import no.nav.utbetaling.beregning.trekk.domain.Trekkvedtak
import no.nav.utbetaling.beregning.trekk.domain.TrekkvedtakId

class TrekkRepositoryTest :
    FunSpec({
        val postgres = autoClose(PostgreSQLContainer("postgres:18-alpine"))
        lateinit var dataSource: HikariDataSource
        lateinit var jdbi: Jdbi
        val repository = TrekkvedtakRepository()
        val historikkRepository = TrekkvedtakHistorikkRepository()
        val registered = LocalDateTime.of(2026, 11, 19, 12, 30)

        beforeSpec {
            postgres.start()
            dataSource =
                autoClose(
                    HikariDataSource(
                        HikariConfig().apply {
                            jdbcUrl = postgres.jdbcUrl
                            username = postgres.username
                            password = postgres.password
                            isAutoCommit = false
                            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
                            maximumPoolSize = 2
                        },
                    ),
                )
            Flyway
                .configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate()
            jdbi = createJdbi(dataSource)
        }

        beforeEach {
            jdbi.useTransaction<Exception> { tx ->
                tx.execute("TRUNCATE trekk_i_fagomraade, trekkperiode, trekkvedtak, trekkvedtak_historikk RESTART IDENTITY CASCADE")
            }
        }

        test("missing external ID has no result") {
            jdbi.useTransaction<Exception> { tx ->
                repository.existsByOsId(42L, tx) shouldBe false
                repository.findByEksternTrekkId(42L, tx) shouldBe null
            }
        }

        test("maps a decision with null optional columns and empty child lists") {
            jdbi.useTransaction<Exception> { tx ->
                tx.execute(
                    """
                    INSERT INTO trekkvedtak
                        (ekstern_trekk_id, trekktype, ansv_enhet, debitor_id, trekkstatus, bruker, tidspkt_reg, mottatt)
                    VALUES (?, 'TEST', '0000', 'test-debitor', 'LOPE', 'test', ?, ?)
                    """.trimIndent(),
                    42L,
                    registered,
                    registered,
                )
                repository.existsByOsId(42L, tx) shouldBe true
                repository.findByEksternTrekkId(42L, tx) shouldBe
                    Trekkvedtak(
                        id = TrekkvedtakId(1L),
                        eksternTrekkId = 42L,
                        trekktypeKode = "TEST",
                        ansvarligEnhet = "0000",
                        debitorId = "test-debitor",
                        trekkstatus = Trekkstatus.LOPE,
                        bruker = "test",
                        tidspktReg = registered,
                        mottatt = registered,
                    )
            }
        }

        test("maps populated optional columns and only the decision's children") {
            jdbi.useTransaction<Exception> { tx ->
                tx.execute(
                    """
                    INSERT INTO trekkvedtak
                        (id, ekstern_trekk_id, trekktype, kreditor_tss_id, kreditor_trekk_id, kreditor_ref,
                         belop_saldo, kreditor_kid, gyldig_tom, ansv_enhet, debitor_id, dato_pri_fom,
                         kilde, trekkstatus, bruker, tidspkt_reg, mottatt)
                    VALUES (1, 42, 'TEST', 'tss', 'trekk', 'ref', 123.45, 'kid', DATE '2026-12-31',
                            '0000', 'test-debitor', DATE '2026-01-01', 'OS', 'HVIL', 'test', ?, ?),
                           (2, 43, 'OTHER', NULL, NULL, NULL, NULL, NULL, NULL,
                            '0000', 'other-debitor', NULL, NULL, 'LOPE', 'test', ?, ?)
                    """.trimIndent(),
                    registered,
                    registered,
                    registered,
                    registered,
                )
                tx.execute(
                    """
                    INSERT INTO trekkperiode (trekkvedtak_id, dato_fom, dato_tom, sats, satstype, feil_reg, bruker, tidspkt_reg)
                    VALUES (1, DATE '2026-01-01', NULL, 12.34, 'M', false, 'test', ?),
                           (1, DATE '2026-02-01', DATE '2026-02-28', 56.78, 'D', true, 'test', ?),
                           (2, DATE '2026-01-01', NULL, 99.00, 'P', false, 'test', ?)
                    """.trimIndent(),
                    registered,
                    registered,
                    registered,
                )
                tx.execute(
                    """
                    INSERT INTO trekk_i_fagomraade (trekkvedtak_id, fagomraade_kode, aktiv, tidspkt_reg)
                    VALUES (1, 'TEST', true, ?), (1, 'INACTIVE', false, ?), (2, 'OTHER', true, ?)
                    """.trimIndent(),
                    registered,
                    registered,
                    registered,
                )

                val result = repository.findByEksternTrekkId(42L, tx).shouldNotBeNull()
                result.copy(trekkperioder = emptyList(), fagomraader = emptyList()) shouldBe
                    Trekkvedtak(
                        id = TrekkvedtakId(1L),
                        eksternTrekkId = 42L,
                        trekktypeKode = "TEST",
                        kreditorIdTss = "tss",
                        kreditorTrekkId = "trekk",
                        kreditorRef = "ref",
                        belopSaldo = BigDecimal("123.45"),
                        kreditorKid = "kid",
                        gyldigTom = LocalDate.of(2026, 12, 31),
                        ansvarligEnhet = "0000",
                        debitorId = "test-debitor",
                        datoPriFom = LocalDate.of(2026, 1, 1),
                        kilde = "OS",
                        trekkstatus = Trekkstatus.HVIL,
                        bruker = "test",
                        tidspktReg = registered,
                        mottatt = registered,
                    )
                result.trekkperioder.sortedBy { it.id?.id } shouldBe
                    listOf(
                        Trekkperiode(
                            id = TrekkperiodeId(1L),
                            trekkvedtakId = 1L,
                            datoFom = LocalDate.of(2026, 1, 1),
                            sats = BigDecimal("12.34"),
                            satstype = SatsType.M,
                            bruker = "test",
                            tidspktReg = registered,
                        ),
                        Trekkperiode(
                            id = TrekkperiodeId(2L),
                            trekkvedtakId = 1L,
                            datoFom = LocalDate.of(2026, 2, 1),
                            datoTom = LocalDate.of(2026, 2, 28),
                            sats = BigDecimal("56.78"),
                            satstype = SatsType.D,
                            feilReg = true,
                            bruker = "test",
                            tidspktReg = registered,
                        ),
                    )
                result.fagomraader.sortedBy { it.id?.id } shouldBe
                    listOf(
                        TrekkFagomraade(TrekkFagomraadeId(1L), 1L, "TEST", true, registered),
                        TrekkFagomraade(TrekkFagomraadeId(2L), 1L, "INACTIVE", false, registered),
                    )

                tx.createQuery("SELECT * FROM trekkvedtak WHERE id = 1")
                    .mapTo(Trekkvedtak::class.java)
                    .one() shouldBe result.copy(trekkperioder = emptyList(), fagomraader = emptyList())
                tx.createQuery("SELECT * FROM trekkperiode WHERE trekkvedtak_id = 1 ORDER BY id")
                    .mapTo(Trekkperiode::class.java)
                    .list() shouldBe result.trekkperioder.sortedBy { it.id?.id }
                tx.createQuery("SELECT * FROM trekk_i_fagomraade WHERE trekkvedtak_id = 1 ORDER BY id")
                    .mapTo(TrekkFagomraade::class.java)
                    .list() shouldBe result.fagomraader.sortedBy { it.id?.id }
            }
        }

        test("history insert returns its generated ID and commits on an autocommit-disabled pool") {
            val message = """{"trekk":"value with 'quotes' and :namedParameter"}"""
            val id =
                jdbi.inTransaction<Long?, Exception> { tx ->
                    tx.connection.autoCommit shouldBe false
                    tx.connection.transactionIsolation shouldBe Connection.TRANSACTION_REPEATABLE_READ
                    historikkRepository.insertTrekkvedtak(message, tx)
                }
            id shouldBe 1L
            jdbi.useTransaction<Exception> { tx ->
                tx
                    .createQuery("SELECT trekkvedtak FROM trekkvedtak_historikk WHERE id = :id")
                    .bind("id", id)
                    .mapTo(String::class.java)
                    .one() shouldBe message
            }
        }

        test("a failure rolls back all history inserts and leaves the pool usable") {
            shouldThrow<IllegalStateException> {
                jdbi.useTransaction<Exception> { tx ->
                    historikkRepository.insertTrekkvedtak("first", tx)
                    historikkRepository.insertTrekkvedtak("second", tx)
                    error("Abort transaction")
                }
            }
            jdbi.useTransaction<Exception> { tx ->
                tx
                    .createQuery("SELECT count(*) FROM trekkvedtak_historikk")
                    .mapTo(Long::class.javaObjectType)
                    .one() shouldBe 0L
                historikkRepository.insertTrekkvedtak("after rollback", tx).shouldNotBeNull()
            }
            jdbi.useTransaction<Exception> { tx ->
                tx
                    .createQuery("SELECT count(*) FROM trekkvedtak_historikk")
                    .mapTo(Long::class.javaObjectType)
                    .one() shouldBe 1L
            }
        }
    })
