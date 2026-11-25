package no.nav.utbetaling.beregning.trekk.listener

import javax.sql.DataSource

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.kotest.core.listeners.AfterEachListener
import io.kotest.core.listeners.BeforeSpecListener
import io.kotest.core.spec.Spec
import io.kotest.core.test.TestCase
import io.kotest.engine.test.TestResult
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait

import no.nav.utbetaling.beregning.jdbc.migrate
import no.nav.utbetaling.beregning.trekk.config.PropertiesConfig

object PostgresListener : BeforeSpecListener, AfterEachListener {
    private val testConfig = PropertiesConfig.config
    val container: PostgreSQLContainer<Nothing> =
        PostgreSQLContainer<Nothing>("postgres:latest").apply {
            withReuse(false)
            withUsername(testConfig.property("postgres.username").getString())
            withPassword(testConfig.property("postgres.password").getString())
            withDatabaseName(testConfig.property("postgres.name").getString())
            waitingFor(Wait.defaultWaitStrategy())
            start()
        }

    val dataSource: DataSource by lazy {
        HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = container.jdbcUrl
                username = container.username
                password = container.password
                minimumIdle = 0
            },
        )
    }

    override suspend fun beforeSpec(spec: Spec) {
        dataSource.migrate()
    }

    override suspend fun afterEach(
        testCase: TestCase,
        result: TestResult,
    ) {
        dataSource.transaction { session ->
            val tables = mutableListOf<String>()
            session.list(
                queryOf("SELECT tablename FROM pg_tables WHERE schemaname='public' AND tablename <> 'flyway_schema_history'"),
            ) { rs -> tables += rs.string("tablename") }

            if (tables.isNotEmpty()) {
                session.execute(queryOf("TRUNCATE TABLE ${tables.joinToString(", ")} RESTART IDENTITY CASCADE"))
            }
        }
    }
}

private fun <A> DataSource.transaction(operation: (TransactionalSession) -> A): A =
    using(sessionOf(this, returnGeneratedKey = true)) { session ->
        session.transaction { tx ->
            operation(tx)
        }
    }
