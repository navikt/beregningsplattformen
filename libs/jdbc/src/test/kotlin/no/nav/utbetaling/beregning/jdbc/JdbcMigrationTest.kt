package no.nav.utbetaling.beregning.jdbc

import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator
import java.util.UUID
import javax.sql.DataSource

import com.zaxxer.hikari.HikariDataSource
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.flywaydb.core.api.CoreErrorCode
import org.flywaydb.core.api.FlywayException
import org.flywaydb.core.api.exception.FlywayValidateException

class JdbcMigrationTest :
    FunSpec({
        test("migreringer kjøres én gang og hoppes over ved neste kjøring") {
            migrationDataSource().use { dataSource ->
                withMigrations(
                    "V1__create_marker.sql" to
                        """
                        CREATE TABLE migration_marker (id INT PRIMARY KEY);
                        INSERT INTO migration_marker VALUES (1);
                        """.trimIndent(),
                ) { directory ->
                    val firstResult =
                        dataSource.migrate("filesystem") {
                            locations("filesystem:$directory")
                        }
                    val secondResult =
                        dataSource.migrate("filesystem") {
                            locations("filesystem:$directory")
                        }

                    firstResult.migrationsExecuted shouldBe 1
                    secondResult.migrationsExecuted shouldBe 0

                    dataSource.connection.use { connection ->
                        connection.createStatement().use { statement ->
                            statement.executeQuery("SELECT COUNT(*) FROM \"filesystem\".migration_marker").use { result ->
                                result.next() shouldBe true
                                result.getInt(1) shouldBe 1
                            }
                        }
                    }
                }
            }
        }

        test("kjører migreringer fra hver classpath-mappe med eget schema og egen historikk") {
            migrationDataSource().use { dataSource ->
                val firstResult = dataSource.migrate("first")

                firstResult.migrationsExecuted shouldBe 1
                hasTable(dataSource, "first", "jdbc_first_marker") shouldBe true
                hasTable(dataSource, "first", "jdbc_second_marker") shouldBe false
                hasTable(dataSource, "second", "jdbc_second_marker") shouldBe false
                dataSource.connection.use { connection ->
                    connection.createStatement().use { statement ->
                        statement.executeQuery("SELECT marker_value FROM \"first\".jdbc_first_marker").use { result ->
                            result.next() shouldBe true
                            result.getString(1) shouldBe "first-marker"
                            result.next() shouldBe false
                        }
                        statement
                            .executeQuery(
                                "SELECT COUNT(*) FROM \"first\".\"flyway_schema_history\" WHERE \"version\" = '1' AND \"success\"",
                            ).use { result ->
                                result.next() shouldBe true
                                result.getInt(1) shouldBe 1
                            }
                    }
                }

                val secondResult = dataSource.migrate("second")

                secondResult.migrationsExecuted shouldBe 1
                hasTable(dataSource, "first", "jdbc_first_marker") shouldBe true
                hasTable(dataSource, "first", "jdbc_second_marker") shouldBe false
                hasTable(dataSource, "second", "jdbc_first_marker") shouldBe false
                hasTable(dataSource, "second", "jdbc_second_marker") shouldBe true
                dataSource.connection.use { connection ->
                    connection.createStatement().use { statement ->
                        statement.executeQuery("SELECT marker_value FROM \"second\".jdbc_second_marker").use { result ->
                            result.next() shouldBe true
                            result.getString(1) shouldBe "second-marker"
                            result.next() shouldBe false
                        }
                        statement
                            .executeQuery(
                                "SELECT COUNT(*) FROM \"second\".\"flyway_schema_history\" WHERE \"version\" = '1' AND \"success\"",
                            ).use { result ->
                                result.next() shouldBe true
                                result.getInt(1) shouldBe 1
                            }
                    }
                }

                dataSource.migrate("first").migrationsExecuted shouldBe 0
                dataSource.migrate("second").migrationsExecuted shouldBe 0
            }
        }

        test("manglende classpath-mappe gir feil") {
            migrationDataSource().use { dataSource ->
                shouldThrow<FlywayException> {
                    dataSource.migrate("missing")
                }
            }
        }

        test("feil i en migrering kastes videre til den som kaller") {
            migrationDataSource().use { dataSource ->
                withMigrations("V1__broken.sql" to "THIS IS NOT VALID SQL") { directory ->
                    shouldThrow<FlywayException> {
                        dataSource.migrate("broken") {
                            locations("filesystem:$directory")
                        }
                    }
                }
            }
        }

        test("avviser endret checksum uten å endre eksisterende data eller historikk") {
            migrationDataSource().use { dataSource ->
                withMigrations(
                    "V1__create_marker.sql" to MARKER_MIGRATION,
                ) { directory ->
                    dataSource
                        .migrate("checksum") {
                            locations("filesystem:$directory")
                        }.migrationsExecuted shouldBe 1
                    val rowsBeforeValidation = markerValues(dataSource, "checksum")
                    val versionsBeforeValidation = migrationVersions(dataSource, "checksum")
                    rowsBeforeValidation shouldBe listOf("existing-marker")
                    versionsBeforeValidation shouldBe listOf("1")
                    val migrationFile = directory.resolve("V1__create_marker.sql")
                    Files.writeString(
                        migrationFile,
                        Files.readString(migrationFile) +
                            "\nCREATE TABLE checksum_added_marker (id INT PRIMARY KEY);\n",
                    )

                    shouldThrow<FlywayValidateException> {
                        dataSource.migrate("checksum") {
                            locations("filesystem:$directory")
                        }
                    }

                    hasTable(dataSource, "checksum", "checksum_added_marker") shouldBe false
                    markerValues(dataSource, "checksum") shouldBe rowsBeforeValidation
                    migrationVersions(dataSource, "checksum") shouldBe versionsBeforeValidation
                }
            }
        }

        test("avviser dupliserte versjoner før noen migreringstabeller opprettes") {
            migrationDataSource().use { dataSource ->
                withMigrations(
                    "V1__first.sql" to "CREATE TABLE first_marker (id INT PRIMARY KEY);",
                    "V1__second.sql" to "CREATE TABLE second_marker (id INT PRIMARY KEY);",
                ) { directory ->
                    val failure =
                        shouldThrow<FlywayException> {
                            dataSource.migrate("duplicate") {
                                locations("filesystem:$directory")
                            }
                        }

                    generateSequence<Throwable>(failure) { it.cause }
                        .filterIsInstance<FlywayException>()
                        .any { it.errorCode == CoreErrorCode.DUPLICATE_VERSIONED_MIGRATION } shouldBe true
                    hasTable(dataSource, "duplicate", "first_marker") shouldBe false
                    hasTable(dataSource, "duplicate", "second_marker") shouldBe false
                }
            }
        }

        test("avviser ugyldig filnavn før fixturetabellen opprettes") {
            migrationDataSource().use { dataSource ->
                withMigrations(
                    "V1_create_marker.sql" to MARKER_MIGRATION,
                ) { directory ->
                    shouldThrow<FlywayException> {
                        dataSource.migrate("invalid_name") {
                            locations("filesystem:$directory")
                        }
                    }

                    hasTable(dataSource, "invalid_name", "migration_marker") shouldBe false
                }
            }
        }

        test("avviser manglende tidligere migreringsfil uten å endre data eller historikk") {
            migrationDataSource().use { dataSource ->
                withMigrations(
                    "V1__create_marker.sql" to MARKER_MIGRATION,
                    "V2__add_status.sql" to "ALTER TABLE migration_marker ADD COLUMN status VARCHAR(20) DEFAULT 'created' NOT NULL;",
                ) { directory ->
                    dataSource
                        .migrate("missing_previous") {
                            locations("filesystem:$directory")
                        }.migrationsExecuted shouldBe 2
                    val rowsBeforeValidation = markerValues(dataSource, "missing_previous")
                    val versionsBeforeValidation = migrationVersions(dataSource, "missing_previous")
                    rowsBeforeValidation shouldBe listOf("existing-marker")
                    versionsBeforeValidation shouldBe listOf("1", "2")
                    Files.delete(directory.resolve("V1__create_marker.sql"))

                    shouldThrow<FlywayValidateException> {
                        dataSource.migrate("missing_previous") {
                            locations("filesystem:$directory")
                        }
                    }

                    markerValues(dataSource, "missing_previous") shouldBe rowsBeforeValidation
                    migrationVersions(dataSource, "missing_previous") shouldBe versionsBeforeValidation
                }
            }
        }
    })

private const val MARKER_MIGRATION =
    """
    CREATE TABLE migration_marker (id INT PRIMARY KEY, marker_value VARCHAR(100));
    INSERT INTO migration_marker VALUES (1, 'existing-marker');
    """

private fun hasTable(
    dataSource: DataSource,
    schema: String,
    tableName: String,
): Boolean =
    dataSource.connection.use { connection ->
        connection.metaData.getTables(null, schema, null, arrayOf("TABLE")).use { tables ->
            generateSequence {
                if (tables.next()) {
                    tables.getString("TABLE_SCHEM") to tables.getString("TABLE_NAME")
                } else {
                    null
                }
            }.any { (tableSchema, table) ->
                tableSchema.equals(schema, ignoreCase = true) && table.equals(tableName, ignoreCase = true)
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
                .executeQuery("SELECT marker_value FROM \"$schema\".migration_marker ORDER BY id")
                .use { result ->
                    buildList {
                        while (result.next()) {
                            add(result.getString("marker_value"))
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
                    "SELECT \"version\" FROM \"$schema\".\"flyway_schema_history\" WHERE \"version\" IS NOT NULL ORDER BY \"installed_rank\"",
                ).use { result ->
                    buildList {
                        while (result.next()) {
                            add(result.getString("version"))
                        }
                    }
                }
        }
    }

private fun migrationDataSource(): HikariDataSource =
    Jdbc.initialize(
        config =
            JdbcConfig(
                jdbcUrl = "jdbc:h2:mem:${UUID.randomUUID()};DB_CLOSE_DELAY=-1",
                username = "sa",
                password = "",
            ),
    ) {
        maximumPoolSize = 3
        minimumIdle = 1
    }

private fun withMigrations(
    vararg migrations: Pair<String, String>,
    operation: (Path) -> Unit,
) {
    val directory = Files.createTempDirectory("jdbc-migrations")
    try {
        migrations.forEach { (name, sql) ->
            Files.writeString(directory.resolve(name), sql)
        }
        operation(directory)
    } finally {
        Files.walk(directory).use { paths ->
            paths.sorted(Comparator.reverseOrder()).forEach(Files::delete)
        }
    }
}
