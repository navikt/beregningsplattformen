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
     * Skattekort har nå migreringsfiler i modulens src/main/resources/db/migration/skattekort/.
     * Eksemplet under er ikke aktivt. Når testene trenger dette skjemaet, legg til importene
     * og aktiver beforeSpec. Flyway sjekker og migrerer da skjemaet før hver testklasse.
     *
     * import io.kotest.core.spec.Spec
     * import no.nav.utbetaling.beregning.jdbc.migrate
     *
     * override suspend fun beforeSpec(spec: Spec) {
     *     dataSource.migrate(schema = "skattekort") {
     *         locations("classpath:db/migration/skattekort")
     *     }
     *
     *     // Aktiver først når abc-modulens migreringsmappe finnes på appens classpath:
     *     // dataSource.migrate(schema = "abc") {
     *     //     locations("classpath:db/migration/abc")
     *     // }
     * }
     *
     * Bruk eget migrate-kall og egen mappe per modul for å holde migreringene uavhengige.
     * schema velger skjema og Flyway-historikk, ikke hvilke migreringsfiler som leses.
     * Utvid også oppryddingen i afterEach når testene bruker flere skjemaer.
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
            /*
             * Rydder fortsatt bare i public. For flere skjemaer, erstatt hele session.list-kallet
             * nedenfor med eksemplet. Velg i IN-listen hvilke skjemaer testene skal tømme.
             * Legg til 'abc' i listen når testene også bruker abc-skjemaet.
             *
             * session.list(
             *     queryOf(
             *         """
             *         SELECT format('%I.%I', schemaname, tablename) AS table_name
             *         FROM pg_tables
             *         WHERE schemaname IN ('public', 'skattekort')
             *           AND tablename <> 'flyway_schema_history'
             *         """.trimIndent(),
             *     ),
             * ) { rs -> tables += rs.string("table_name") }
             *
             * format('%I.%I', ...) setter anførselstegn rundt navnene ved behov,
             * slik at TRUNCATE bruker riktig skjema og tabell.
             * Historikken beholdes i hvert skjema. Data lagt inn av migreringer tømmes også,
             * og må lastes på nytt ved behov. CASCADE kan tømme avhengige tabeller utenfor listen.
             */
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
