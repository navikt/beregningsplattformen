package no.nav.utbetaling.beregning.beregningsplattform.listener

import javax.sql.DataSource

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.kotest.core.listeners.AfterEachListener
import io.kotest.core.listeners.BeforeSpecListener
import io.kotest.core.test.TestCase
import io.kotest.engine.test.TestResult
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.ext.ScriptUtils
import org.testcontainers.jdbc.JdbcDatabaseDelegate

import no.nav.utbetaling.beregning.beregningsplattform.config.PropertiesConfig

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

    /*
     * Når skattekort får migreringsfiler i src/main/resources/db/migration/skattekort/,
     * legg disse importene øverst i filen:
     * import io.kotest.core.spec.Spec
     * import no.nav.utbetaling.beregning.jdbc.migrate
     *
     * Aktiver deretter denne metoden, slik at migreringene kjører før testene:
     * override suspend fun beforeSpec(spec: Spec) {
     *     dataSource.migrate(schema = "skattekort")
     *     // Legg til dataSource.migrate(schema = "abc") når abc får egne migreringsfiler.
     * }
     */

    fun loadDataSet(script: String) {
        if (script.isNotEmpty()) {
            ScriptUtils.runInitScript(JdbcDatabaseDelegate(container, ""), script)
        }
    }

    override suspend fun afterEach(
        testCase: TestCase,
        result: TestResult,
    ) {
        dataSource.transaction { session ->
            val tables = mutableListOf<String>()
            // Rydder nå bare public og beholder Flyways historikk.
            // Når moduler får egne skjemaer, erstatt SELECT-spørringen med:
            // SELECT format('%I.%I', schemaname, tablename) AS table_name
            // FROM pg_tables
            // WHERE schemaname IN ('public', 'skattekort', 'abc')
            //   AND tablename <> 'flyway_schema_history'
            // Ta bare med skjemaer testene skal rydde. Bytt også til rs.string("table_name") nedenfor.
            // format('%I.%I', ...) siterer skjema- og tabellnavn, slik at TRUNCATE treffer riktig tabell.
            session.list(
                queryOf("SELECT tablename FROM pg_tables WHERE schemaname='public' AND tablename <> 'flyway_schema_history'"),
            ) { rs -> tables += rs.string("tablename") }

            if (tables.isNotEmpty()) {
                session.execute(queryOf("TRUNCATE TABLE ${tables.joinToString(", ")} RESTART IDENTITY CASCADE"))
            }
        }
    }
}

fun <A> DataSource.transaction(operation: (TransactionalSession) -> A): A =
    using(sessionOf(this, returnGeneratedKey = true)) { session ->
        session.transaction { tx ->
            operation(tx)
        }
    }
