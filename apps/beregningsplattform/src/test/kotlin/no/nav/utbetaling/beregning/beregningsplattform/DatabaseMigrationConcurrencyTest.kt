package no.nav.utbetaling.beregning.beregningsplattform

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS
import javax.sql.DataSource

import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.flywaydb.core.api.callback.Callback
import org.flywaydb.core.api.callback.Context
import org.flywaydb.core.api.callback.Event

import no.nav.utbetaling.beregning.beregningsplattform.listener.PostgresListener
import no.nav.utbetaling.beregning.jdbc.Jdbc
import no.nav.utbetaling.beregning.jdbc.JdbcConfig
import no.nav.utbetaling.beregning.jdbc.migrate

private const val CONCURRENT_SCHEMA = "jdbc_test_concurrent"
private const val MARKER_TABLE = "migration_marker"
private const val FIRST_SCHEMA = "jdbc_test_first"

class DatabaseMigrationConcurrencyTest :
    FunSpec({
        extensions(PostgresListener)

        afterEach {
            PostgresListener.dataSource.connection.use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute("DROP SCHEMA IF EXISTS \"$CONCURRENT_SCHEMA\" CASCADE")
                }
            }
        }

        test("to migreringsinstanser oppgraderer samme skjema uten å kjøre V2 dobbelt") {
            val jdbcConfig =
                JdbcConfig(
                    jdbcUrl = PostgresListener.container.jdbcUrl,
                    username = PostgresListener.container.username,
                    password = PostgresListener.container.password,
                )
            Jdbc
                .initialize(jdbcConfig) {
                    addDataSourceProperty("socketTimeout", 10)
                }.use { firstDataSource ->
                    Jdbc
                        .initialize(jdbcConfig) {
                            addDataSourceProperty("socketTimeout", 10)
                        }.use { secondDataSource ->
                            firstDataSource
                                .migrate(CONCURRENT_SCHEMA) {
                                    locations("classpath:db/migration/$FIRST_SCHEMA")
                                    target("1")
                                }.migrationsExecuted shouldBe 1

                            val firstEnteredV2 = CountDownLatch(1)
                            val releaseFirstV2 = CountDownLatch(1)
                            val secondReachedMigrate = CountDownLatch(1)

                            val (firstResult, secondResult) =
                                coroutineScope {
                                    val first =
                                        async(Dispatchers.IO) {
                                            runInterruptible {
                                                firstDataSource.migrate(CONCURRENT_SCHEMA) {
                                                    locations("classpath:db/migration/$FIRST_SCHEMA")
                                                    callbacks(
                                                        LatchCallback(Event.BEFORE_EACH_MIGRATE) {
                                                            firstEnteredV2.countDown()
                                                            check(releaseFirstV2.await(30, SECONDS)) {
                                                                "Tidsfristen for å frigi første V2-migrering gikk ut"
                                                            }
                                                        },
                                                    )
                                                }
                                            }
                                        }
                                    try {
                                        withContext(Dispatchers.IO) {
                                            firstEnteredV2.await(30, SECONDS)
                                        } shouldBe true
                                        val second =
                                            async(Dispatchers.IO) {
                                                runInterruptible {
                                                    secondDataSource.migrate(CONCURRENT_SCHEMA) {
                                                        locations("classpath:db/migration/$FIRST_SCHEMA")
                                                        callbacks(
                                                            LatchCallback(Event.BEFORE_MIGRATE) {
                                                                secondReachedMigrate.countDown()
                                                            },
                                                        )
                                                    }
                                                }
                                            }

                                        withContext(Dispatchers.IO) {
                                            secondReachedMigrate.await(30, SECONDS)
                                        } shouldBe true
                                        withTimeoutOrNull(500.milliseconds) {
                                            second.await()
                                        } shouldBe null
                                        releaseFirstV2.countDown()

                                        val firstResult =
                                            withTimeoutOrNull(30.seconds) { first.await() }
                                                ?: error("Første migreringsinstans fullførte ikke innen tidsfristen")
                                        val secondResult =
                                            withTimeoutOrNull(30.seconds) { second.await() }
                                                ?: error("Andre migreringsinstans fullførte ikke innen tidsfristen")
                                        firstResult to secondResult
                                    } finally {
                                        releaseFirstV2.countDown()
                                    }
                                }

                            firstResult.migrationsExecuted shouldBe 1
                            secondResult.migrationsExecuted shouldBe 0
                            hasColumn(PostgresListener.dataSource, CONCURRENT_SCHEMA, MARKER_TABLE, "status") shouldBe true
                            markerRows(PostgresListener.dataSource, CONCURRENT_SCHEMA) shouldBe
                                listOf(ConcurrentMarkerRow("first-marker", "created"))
                            migrationVersions(PostgresListener.dataSource, CONCURRENT_SCHEMA) shouldBe listOf("1", "2")
                        }
                }
        }
    })

private class LatchCallback(
    private val callbackEvent: Event,
    private val action: () -> Unit,
) : Callback {
    override fun supports(
        event: Event,
        context: Context?,
    ): Boolean = event == callbackEvent

    override fun canHandleInTransaction(
        event: Event,
        context: Context,
    ): Boolean = true

    override fun handle(
        event: Event,
        context: Context,
    ) {
        action()
    }

    override fun getCallbackName(): String = "test-$callbackEvent"
}

private data class ConcurrentMarkerRow(
    val value: String,
    val status: String,
)

private fun hasColumn(
    dataSource: DataSource,
    schema: String,
    table: String,
    column: String,
): Boolean =
    dataSource.connection.use { connection ->
        connection
            .prepareStatement(
                "SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = ? AND table_name = ? AND column_name = ?)",
            ).use { statement ->
                statement.setString(1, schema)
                statement.setString(2, table)
                statement.setString(3, column)
                statement.executeQuery().use { result ->
                    result.next()
                    result.getBoolean(1)
                }
            }
    }

private fun markerRows(
    dataSource: DataSource,
    schema: String,
): List<ConcurrentMarkerRow> =
    dataSource.connection.use { connection ->
        connection.createStatement().use { statement ->
            statement
                .executeQuery("SELECT marker_value, status FROM \"$schema\".$MARKER_TABLE ORDER BY id")
                .use { result ->
                    buildList {
                        while (result.next()) {
                            add(ConcurrentMarkerRow(result.getString("marker_value"), result.getString("status")))
                        }
                    }
                }
        }
    }

private fun migrationVersions(
    dataSource: DataSource,
    schema: String,
): List<String> =
    dataSource.connection.use { connection ->
        connection.createStatement().use { statement ->
            statement
                .executeQuery(
                    "SELECT version FROM \"$schema\".flyway_schema_history WHERE version IS NOT NULL ORDER BY installed_rank",
                ).use { result ->
                    buildList {
                        while (result.next()) {
                            add(result.getString("version"))
                        }
                    }
                }
        }
    }
