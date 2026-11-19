package no.nav.utbetaling.beregning.trekk.config

import java.sql.SQLException
import javax.sql.DataSource

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory
import org.flywaydb.core.Flyway
import org.postgresql.ds.PGSimpleDataSource

import no.nav.utbetaling.beregning.log.Log
import no.nav.utbetaling.beregning.trekk.Metrics.prometheusMeterRegistry

object DatabaseConfig {
    val dataSource: DataSource by lazy {
        HikariDataSource(initHikariConfig())
    }

    init {
        if (!(PropertiesConfig.applicationProperties.isLocal || PropertiesConfig.applicationProperties.isTest)) {
            Runtime.getRuntime().addShutdownHook(
                Thread {
                    (dataSource as HikariDataSource).close()
                },
            )
        }
    }

    fun migrate(dataSource: DataSource = this.dataSource) {
        val locations = migrationLocations(PropertiesConfig.applicationProperties.profile)
        Flyway
            .configure()
            .outOfOrder(true)
            .dataSource(dataSource)
            .locations(*locations.toTypedArray())
            .lockRetryCount(-1)
            .validateMigrationNaming(true)
            .sqlMigrationSeparator("__")
            .sqlMigrationPrefix("V")
            .ignoreMigrationPatterns("")
            .load()
            .migrate()
            .migrationsExecuted
        Log.info("Migration ferdig for ${PropertiesConfig.applicationProperties.profile}")
    }

    internal fun migrationLocations(profile: Profile): List<String> =
        when (profile) {
            Profile.PROD -> listOf("classpath:db/migration")
            else -> listOf("classpath:db/migration", "classpath:db/migration-nonprod")
        }

    private fun initHikariConfig(poolname: String = "postgres-pool"): HikariConfig {
        val postgresProperties: PropertiesConfig.PostgresProperties = PropertiesConfig.postgresProperties
        return HikariConfig().apply {
            poolName = poolname
            maximumPoolSize = 15
            minimumIdle = 1
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            connectionTimeout =
                java.time.Duration
                    .ofSeconds(2)
                    .toMillis()
            validationTimeout =
                java.time.Duration
                    .ofSeconds(1)
                    .toMillis()
            initializationFailTimeout =
                java.time.Duration
                    .ofSeconds(30)
                    .toMillis()
            idleTimeout =
                java.time.Duration
                    .ofMinutes(10)
                    .toMillis()
            maxLifetime =
                java.time.Duration
                    .ofMinutes(30)
                    .toMillis()

            when {
                !(PropertiesConfig.applicationProperties.isLocal || PropertiesConfig.applicationProperties.isTest) -> {
                    jdbcUrl = postgresProperties.jdbcUrl
                    Log.info("Setter opp PostgreSQL")
                }

                else -> {
                    Log.info("Setter opp lokal PostgreSQL")
                    this.dataSource =
                        PGSimpleDataSource().apply {
                            user = postgresProperties.username
                            password = postgresProperties.password
                            serverNames = arrayOf(postgresProperties.host)
                            databaseName = postgresProperties.name
                            portNumbers = intArrayOf(postgresProperties.port)
                        }
                }
            }
            metricsTrackerFactory = MicrometerMetricsTrackerFactory(prometheusMeterRegistry)
        }
    }
}

internal suspend fun databaseHealthy(dataSource: DataSource): Boolean =
    withContext(Dispatchers.IO) {
        try {
            val isValid = dataSource.connection.use { it.isValid(1) }
            if (!isValid) {
                Log.warn("Database health check failed")
            }
            isValid
        } catch (exception: SQLException) {
            Log.warn("Database health check failed (SQLState: ${exception.sqlState})")
            false
        }
    }
