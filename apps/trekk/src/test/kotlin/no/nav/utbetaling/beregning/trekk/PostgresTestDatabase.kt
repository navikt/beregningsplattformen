package no.nav.utbetaling.beregning.trekk

import com.zaxxer.hikari.HikariDataSource
import io.kotest.core.listeners.AfterSpecListener
import io.kotest.core.listeners.BeforeEachListener
import io.kotest.core.listeners.BeforeSpecListener
import io.kotest.core.spec.Spec
import io.kotest.core.test.TestCase
import org.flywaydb.core.Flyway
import org.testcontainers.containers.PostgreSQLContainer

object PostgresTestDatabase : BeforeSpecListener, AfterSpecListener, BeforeEachListener {
    private val postgres = PostgreSQLContainer("postgres:18-alpine")
    val dataSource = HikariDataSource()

    override suspend fun beforeSpec(spec: Spec) {
        postgres.start()
        dataSource.jdbcUrl = postgres.jdbcUrl
        dataSource.username = postgres.username
        dataSource.password = postgres.password
        dataSource.maximumPoolSize = 2
        Flyway
            .configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load()
            .migrate()
    }

    override suspend fun afterSpec(spec: Spec) {
        try {
            dataSource.close()
        } finally {
            postgres.stop()
        }
    }

    override suspend fun beforeEach(testCase: TestCase) {
        dataSource.connection.use { connection ->
            connection.createStatement().use {
                it.execute(
                    """
                    TRUNCATE trekkperiode, trekk_i_fagomraade, trekkvedtak, trekkvedtak_historikk
                    RESTART IDENTITY
                    """.trimIndent(),
                )
            }
        }
    }
}
