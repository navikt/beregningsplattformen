package no.nav.beregning.taskv2

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jdbi.v3.core.Jdbi
import org.testcontainers.containers.PostgreSQLContainer
import java.util.concurrent.atomic.AtomicInteger
import javax.sql.DataSource

object TestDatabase {
    private val container = PostgreSQLContainer("postgres:16-alpine").apply { start() }
    private val teller = AtomicInteger()

    fun nyDataSource(): DataSource {
        val schemaNavn = "taskv2_test_${teller.incrementAndGet()}"
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
            .locations("classpath:no/nav/beregning/taskv2/db/migration")
            .load()
            .migrate()
        return ds
    }

    fun jdbi(ds: DataSource): Jdbi = Jdbi.create(ds)
}
