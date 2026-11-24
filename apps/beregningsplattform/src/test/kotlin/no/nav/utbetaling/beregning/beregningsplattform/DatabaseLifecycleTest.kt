package no.nav.utbetaling.beregning.beregningsplattform

import java.nio.file.Files
import java.sql.SQLException
import javax.sql.DataSource

import com.zaxxer.hikari.HikariDataSource
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.ktor.server.testing.testApplication
import org.flywaydb.core.api.FlywayException

import no.nav.utbetaling.beregning.beregningsplattform.config.PropertiesConfig
import no.nav.utbetaling.beregning.beregningsplattform.listener.PostgresListener
import no.nav.utbetaling.beregning.jdbc.migrate

private const val BROKEN_SCHEMA = "jdbc_test_broken"
private const val DISCONNECT_SCHEMA = "jdbc_test_disconnect"
private const val FIRST_SCHEMA = "jdbc_test_first"
private const val MARKER_TABLE = "migration_marker"

class DatabaseLifecycleTest :
    FunSpec({
        extensions(PostgresListener)

        afterEach {
            dropLifecycleSchemas(PostgresListener.dataSource)
        }

        test("lukker databasepoolen når testverten stenges") {
            val postgresProperties =
                PropertiesConfig.postgresProperties.copy(
                    jdbcUrl = PostgresListener.container.jdbcUrl,
                    host = PostgresListener.container.host,
                    port = PostgresListener.container.firstMappedPort,
                )
            lateinit var dataSource: HikariDataSource

            testApplication {
                application {
                    dataSource = initializeDatabase(postgresProperties)
                }
                startApplication()

                dataSource.isClosed shouldBe false
                dataSource.connection.use { connection ->
                    connection.isValid(1) shouldBe true
                }
            }

            dataSource.isClosed shouldBe true
        }

        test("ruller tilbake en feilende SQL-migrering og lukker appens databasepool ved oppstart") {
            lateinit var applicationDataSource: HikariDataSource

            val failure =
                shouldThrow<Throwable> {
                    testApplication {
                        application {
                            applicationDataSource = initializeDatabase(testPostgresProperties())
                            applicationDataSource.migrate(BROKEN_SCHEMA)
                        }
                        startApplication()
                    }
                }

            hasFlywayException(failure) shouldBe true
            causeChain(failure).filterIsInstance<SQLException>().any { it.sqlState == "42601" } shouldBe true
            applicationDataSource.isClosed shouldBe true
            hasTable(PostgresListener.dataSource, BROKEN_SCHEMA, MARKER_TABLE) shouldBe false
        }

        test("ruller tilbake migrering når migreringsforbindelsen termineres") {
            val migrationDirectory = tempdir().toPath()
            val migrationFile = migrationDirectory.resolve("V1__create_marker.sql")
            copyMigrationResource(migrationFile)
            Files.writeString(
                migrationFile,
                Files.readString(migrationFile) + "\nSELECT pg_terminate_backend(pg_backend_pid());\n",
            )
            lateinit var applicationDataSource: HikariDataSource

            val failure =
                shouldThrow<Throwable> {
                    testApplication {
                        application {
                            applicationDataSource = initializeDatabase(testPostgresProperties())
                            applicationDataSource.migrate(DISCONNECT_SCHEMA) {
                                locations("filesystem:${migrationDirectory.toAbsolutePath()}")
                            }
                        }
                        startApplication()
                    }
                }

            hasFlywayException(failure) shouldBe true
            causeChain(failure)
                .filterIsInstance<SQLException>()
                .any { it.sqlState == "57P01" || it.sqlState?.startsWith("08") == true } shouldBe true
            applicationDataSource.isClosed shouldBe true
            hasTable(PostgresListener.dataSource, DISCONNECT_SCHEMA, MARKER_TABLE) shouldBe false
            PostgresListener.dataSource.connection.use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeQuery("SELECT 1").use { result ->
                        result.next() shouldBe true
                        result.getInt(1) shouldBe 1
                    }
                }
            }
        }
    })

private fun testPostgresProperties(): PropertiesConfig.PostgresProperties =
    PropertiesConfig.postgresProperties.copy(
        jdbcUrl = PostgresListener.container.jdbcUrl,
        host = PostgresListener.container.host,
        port = PostgresListener.container.firstMappedPort,
    )

private fun copyMigrationResource(destination: java.nio.file.Path) {
    val resource =
        checkNotNull(
            Thread
                .currentThread()
                .contextClassLoader
                .getResourceAsStream("db/migration/$FIRST_SCHEMA/V1__create_marker.sql"),
        )
    resource.use { input -> Files.copy(input, destination) }
}

private fun causeChain(throwable: Throwable): Sequence<Throwable> = generateSequence(throwable) { it.cause }

private fun hasFlywayException(throwable: Throwable): Boolean = causeChain(throwable).any { it is FlywayException }

private fun hasTable(
    dataSource: DataSource,
    schema: String,
    table: String,
): Boolean =
    dataSource.connection.use { connection ->
        connection.metaData.getTables(null, schema, table, arrayOf("TABLE")).use { result ->
            result.next()
        }
    }

private fun dropLifecycleSchemas(dataSource: DataSource) {
    dataSource.connection.use { connection ->
        connection.createStatement().use { statement ->
            listOf(BROKEN_SCHEMA, DISCONNECT_SCHEMA).forEach { schema ->
                statement.execute("DROP SCHEMA IF EXISTS \"$schema\" CASCADE")
            }
        }
    }
}
