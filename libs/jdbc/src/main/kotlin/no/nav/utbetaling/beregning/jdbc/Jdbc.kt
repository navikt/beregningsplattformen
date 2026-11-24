package no.nav.utbetaling.beregning.jdbc

import java.sql.SQLException
import javax.sql.DataSource

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.flywaydb.core.api.output.MigrateResult
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(Jdbc::class.java)

class JdbcConfig(
    val jdbcUrl: String,
    val username: String,
    val password: String,
)

object Jdbc {
    fun initialize(
        config: JdbcConfig,
        hikariConfig: HikariConfig.() -> Unit = {},
    ): HikariDataSource =
        HikariConfig()
            .apply {
                jdbcUrl = config.jdbcUrl
                username = config.username
                password = config.password
                maximumPoolSize = 3
                minimumIdle = 1
                idleTimeout = 300_000
                maxLifetime = 1_800_000
                connectionTimeout = 2_000
                validationTimeout = 1_000
                initializationFailTimeout = 30_000
                hikariConfig()
            }.let(::HikariDataSource)
}

fun DataSource.isHealthy(): Boolean =
    try {
        connection.use { it.isValid(1) }.also { healthy ->
            if (!healthy) {
                logger.warn("Database health check failed")
            }
        }
    } catch (exception: SQLException) {
        logger.warn("Database health check failed (SQLState: {})", exception.sqlState)
        false
    }

fun DataSource.migrate(
    schema: String,
    configure: FluentConfiguration.() -> Unit = {},
): MigrateResult =
    Flyway
        .configure()
        .locations("classpath:db/migration/$schema")
        .failOnMissingLocations(true)
        .validateMigrationNaming(true)
        .apply(configure)
        .dataSource(this)
        .schemas(schema)
        .defaultSchema(schema)
        .load()
        .migrate()
