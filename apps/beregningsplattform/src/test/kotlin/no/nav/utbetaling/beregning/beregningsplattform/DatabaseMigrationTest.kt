package no.nav.utbetaling.beregning.beregningsplattform

import java.nio.file.Files
import java.sql.SQLException
import javax.sql.DataSource

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import org.flywaydb.core.api.FlywayException

import no.nav.utbetaling.beregning.beregningsplattform.listener.PostgresListener
import no.nav.utbetaling.beregning.jdbc.migrate

private const val FIRST_SCHEMA = "jdbc_test_first"
private const val SECOND_SCHEMA = "jdbc_test_second"
private const val RETRY_SCHEMA = "jdbc_test_retry"
private const val MARKER_TABLE = "migration_marker"
private val TEST_SCHEMAS =
    listOf(
        FIRST_SCHEMA,
        SECOND_SCHEMA,
        RETRY_SCHEMA,
    )

class DatabaseMigrationTest :
    FunSpec({
        extensions(PostgresListener)

        afterEach {
            dropTestSchemas(PostgresListener.dataSource)
        }

        test("migrerer et tomt skjema én gang og bevarer data og historikk ved gjentakelse") {
            val firstResult = PostgresListener.dataSource.migrate(FIRST_SCHEMA)

            firstResult.migrationsExecuted shouldBe 2
            val rowsAfterFirstMigration = markerRows(PostgresListener.dataSource, FIRST_SCHEMA)
            val versionsAfterFirstMigration = migrationVersions(PostgresListener.dataSource, FIRST_SCHEMA)
            rowsAfterFirstMigration shouldBe listOf(MarkerRow("first-marker", "created"))
            versionsAfterFirstMigration shouldBe listOf("1", "2")

            val repeatedResult = PostgresListener.dataSource.migrate(FIRST_SCHEMA)

            repeatedResult.migrationsExecuted shouldBe 0
            markerRows(PostgresListener.dataSource, FIRST_SCHEMA) shouldBe rowsAfterFirstMigration
            migrationVersions(PostgresListener.dataSource, FIRST_SCHEMA) shouldBe versionsAfterFirstMigration
        }

        test("oppgraderer et skjema fra V1 til V2 uten å miste eksisterende rader") {
            PostgresListener.dataSource
                .migrate(FIRST_SCHEMA) {
                    target("1")
                }.migrationsExecuted shouldBe 1
            insertMarker(PostgresListener.dataSource, FIRST_SCHEMA, "added-before-v2")

            val result = PostgresListener.dataSource.migrate(FIRST_SCHEMA)

            result.migrationsExecuted shouldBe 1
            markerRows(PostgresListener.dataSource, FIRST_SCHEMA) shouldBe
                listOf(
                    MarkerRow("first-marker", "created"),
                    MarkerRow("added-before-v2", "created"),
                )
            migrationVersions(PostgresListener.dataSource, FIRST_SCHEMA) shouldBe listOf("1", "2")
        }

        test("bruker separate classpath-migreringer og historikk for hvert skjema") {
            PostgresListener.dataSource.migrate(FIRST_SCHEMA).migrationsExecuted shouldBe 2
            PostgresListener.dataSource.migrate(SECOND_SCHEMA).migrationsExecuted shouldBe 1

            markerValues(PostgresListener.dataSource, FIRST_SCHEMA) shouldBe listOf("first-marker")
            markerValues(PostgresListener.dataSource, SECOND_SCHEMA) shouldBe listOf("second-marker")
            migrationVersions(PostgresListener.dataSource, FIRST_SCHEMA) shouldBe listOf("1", "2")
            migrationVersions(PostgresListener.dataSource, SECOND_SCHEMA) shouldBe listOf("1")
        }

        test("ruller tilbake V2-feilen og kjører bare V2 på nytt") {
            val migrationDirectory = tempdir().toPath()
            val versionOne = migrationDirectory.resolve("V1__create_marker.sql")
            val versionTwo = migrationDirectory.resolve("V2__add_status.sql")
            copyMigrationResource(FIRST_SCHEMA, versionOne.fileName.toString(), versionOne)
            copyMigrationResource(FIRST_SCHEMA, versionTwo.fileName.toString(), versionTwo)
            Files.writeString(
                versionTwo,
                """
                ALTER TABLE migration_marker ADD COLUMN retry_status TEXT;
                THIS IS NOT VALID SQL;
                """.trimIndent(),
            )

            val failure =
                shouldThrow<Throwable> {
                    PostgresListener.dataSource.migrate(RETRY_SCHEMA) {
                        locations("filesystem:${migrationDirectory.toAbsolutePath()}")
                    }
                }

            hasFlywayException(failure) shouldBe true
            hasSqlState(failure, "42601") shouldBe true
            markerValues(PostgresListener.dataSource, RETRY_SCHEMA) shouldBe listOf("first-marker")
            migrationVersions(PostgresListener.dataSource, RETRY_SCHEMA) shouldBe listOf("1")
            hasColumn(PostgresListener.dataSource, RETRY_SCHEMA, MARKER_TABLE, "retry_status") shouldBe false

            copyMigrationResource(FIRST_SCHEMA, "V2__add_status.sql", versionTwo)
            val retryResult =
                PostgresListener.dataSource.migrate(RETRY_SCHEMA) {
                    locations("filesystem:${migrationDirectory.toAbsolutePath()}")
                }

            retryResult.migrationsExecuted shouldBe 1
            markerRows(PostgresListener.dataSource, RETRY_SCHEMA) shouldBe
                listOf(MarkerRow("first-marker", "created"))
            migrationVersions(PostgresListener.dataSource, RETRY_SCHEMA) shouldBe listOf("1", "2")
        }

        test("beholder modul A når modul B feiler og fortsetter bare modul B ved nytt forsøk") {
            val migrationDirectory = tempdir().toPath()
            val moduleBMigration = migrationDirectory.resolve("V1__create_marker.sql")
            copyMigrationResource(SECOND_SCHEMA, "V1__create_marker.sql", moduleBMigration)
            Files.writeString(
                moduleBMigration,
                Files.readString(moduleBMigration) + "\nTHIS IS NOT VALID SQL;\n",
            )

            PostgresListener.dataSource.migrate(FIRST_SCHEMA).migrationsExecuted shouldBe 2
            val failure =
                shouldThrow<Throwable> {
                    PostgresListener.dataSource.migrate(SECOND_SCHEMA) {
                        locations("filesystem:${migrationDirectory.toAbsolutePath()}")
                    }
                }

            hasFlywayException(failure) shouldBe true
            hasSqlState(failure, "42601") shouldBe true
            markerValues(PostgresListener.dataSource, FIRST_SCHEMA) shouldBe listOf("first-marker")
            migrationVersions(PostgresListener.dataSource, FIRST_SCHEMA) shouldBe listOf("1", "2")
            hasTable(PostgresListener.dataSource, SECOND_SCHEMA, MARKER_TABLE) shouldBe false

            copyMigrationResource(SECOND_SCHEMA, "V1__create_marker.sql", moduleBMigration)
            PostgresListener.dataSource.migrate(FIRST_SCHEMA).migrationsExecuted shouldBe 0
            PostgresListener.dataSource
                .migrate(SECOND_SCHEMA) {
                    locations("filesystem:${migrationDirectory.toAbsolutePath()}")
                }.migrationsExecuted shouldBe 1

            markerValues(PostgresListener.dataSource, FIRST_SCHEMA) shouldBe listOf("first-marker")
            markerValues(PostgresListener.dataSource, SECOND_SCHEMA) shouldBe listOf("second-marker")
            migrationVersions(PostgresListener.dataSource, FIRST_SCHEMA) shouldBe listOf("1", "2")
            migrationVersions(PostgresListener.dataSource, SECOND_SCHEMA) shouldBe listOf("1")
        }
    })

private data class MarkerRow(
    val value: String,
    val status: String,
)

private fun copyMigrationResource(
    schema: String,
    fileName: String,
    destination: java.nio.file.Path,
) {
    val resource =
        checkNotNull(
            Thread
                .currentThread()
                .contextClassLoader
                .getResourceAsStream("db/migration/$schema/$fileName"),
        )
    resource.use { input -> Files.copy(input, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING) }
}

private fun hasFlywayException(throwable: Throwable): Boolean = generateSequence(throwable) { it.cause }.any { it is FlywayException }

private fun hasSqlState(
    throwable: Throwable,
    sqlState: String,
): Boolean =
    generateSequence(throwable) { it.cause }
        .filterIsInstance<SQLException>()
        .any { it.sqlState == sqlState }

private fun dropTestSchemas(dataSource: DataSource) {
    dataSource.connection.use { connection ->
        connection.createStatement().use { statement ->
            TEST_SCHEMAS.forEach { schema ->
                statement.execute("DROP SCHEMA IF EXISTS \"$schema\" CASCADE")
            }
        }
    }
}

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
): List<MarkerRow> =
    dataSource.connection.use { connection ->
        connection.createStatement().use { statement ->
            statement
                .executeQuery(
                    "SELECT marker_value, status FROM \"$schema\".$MARKER_TABLE ORDER BY id",
                ).use { result ->
                    buildList {
                        while (result.next()) {
                            add(MarkerRow(result.getString("marker_value"), result.getString("status")))
                        }
                    }
                }
        }
    }

private fun markerValues(
    dataSource: DataSource,
    schema: String,
): List<String> =
    dataSource.connection.use { connection ->
        connection.createStatement().use { statement ->
            statement
                .executeQuery(
                    "SELECT marker_value FROM \"$schema\".$MARKER_TABLE ORDER BY id",
                ).use { result ->
                    buildList {
                        while (result.next()) {
                            add(result.getString("marker_value"))
                        }
                    }
                }
        }
    }

private fun insertMarker(
    dataSource: DataSource,
    schema: String,
    value: String,
) {
    dataSource.connection.use { connection ->
        connection
            .prepareStatement(
                "INSERT INTO \"$schema\".$MARKER_TABLE (marker_value) VALUES (?)",
            ).use { statement ->
                statement.setString(1, value)
                statement.executeUpdate() shouldBe 1
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
