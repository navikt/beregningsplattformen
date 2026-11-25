package no.nav.beregning.task

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jdbi.v3.core.Jdbi
import org.testcontainers.containers.PostgreSQLContainer
import javax.sql.DataSource

/**
 * 🟢 GRØNN SONE — testinfrastruktur. Ekte Postgres via Testcontainers, fordi
 * FOR UPDATE SKIP LOCKED og TIMESTAMPTZ ikke kan mockes meningsfullt.
 */
object TestDatabase {
    private val container =
        PostgreSQLContainer("postgres:16-alpine").apply { start() }

    private val teller = java.util.concurrent.atomic.AtomicInteger()

    /**
     * Hver test får sitt eget schema i den delte containeren, så parallelle tester ikke
     * tramper på hverandres data. Flyway kjører migreringene inn i schemaet.
     */
    fun nyDataSource(): DataSource {
        val schemaNavn = "task_test_${teller.incrementAndGet()}"
        val ds =
            HikariDataSource(
                HikariConfig().apply {
                    jdbcUrl = container.jdbcUrl
                    username = container.username
                    password = container.password
                    maximumPoolSize = 5
                    schema = schemaNavn
                    connectionInitSql = "CREATE SCHEMA IF NOT EXISTS $schemaNavn"
                },
            )
        Flyway.configure()
            .dataSource(ds)
            .schemas(schemaNavn)
            .locations("classpath:no/nav/beregning/task/db/migration")
            .load()
            .migrate()
        return ds
    }

    fun jdbi(ds: DataSource): Jdbi = Jdbi.create(ds)
}
