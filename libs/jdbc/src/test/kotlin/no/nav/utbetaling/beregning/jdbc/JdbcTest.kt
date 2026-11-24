package no.nav.utbetaling.beregning.jdbc

import java.sql.DriverManager
import java.util.UUID

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class JdbcTest :
    FunSpec({
        test("initialisering bruker tilkoblingsinnstillingene fra JdbcConfig") {
            val config =
                JdbcConfig(
                    jdbcUrl = "jdbc:h2:mem:${UUID.randomUUID()}",
                    username = "test_user",
                    password = "test_password",
                )

            DriverManager.getConnection(config.jdbcUrl, config.username, config.password).use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute("CREATE TABLE configuration_marker (id INT PRIMARY KEY)")
                    statement.executeUpdate("INSERT INTO configuration_marker VALUES (1)")
                }

                Jdbc.initialize(config).use { dataSource ->
                    dataSource.connection.use { pooledConnection ->
                        pooledConnection.createStatement().use { statement ->
                            statement.executeQuery("SELECT id FROM configuration_marker").use { result ->
                                result.next() shouldBe true
                                result.getInt(1) shouldBe 1
                            }
                        }
                    }
                }
            }
        }

        test("frisk pool er sunn og returnerer forbindelsen etter lån") {
            Jdbc
                .initialize(
                    config =
                        JdbcConfig(
                            jdbcUrl = "jdbc:h2:mem:${UUID.randomUUID()}",
                            username = "sa",
                            password = "",
                        ),
                ) {
                    maximumPoolSize = 1
                    minimumIdle = 1
                }.use { dataSource ->
                    dataSource.isHealthy() shouldBe true

                    dataSource.connection.use {
                        dataSource.hikariPoolMXBean.activeConnections shouldBe 1
                    }

                    dataSource.hikariPoolMXBean.activeConnections shouldBe 0
                }
        }

        test("lukket pool er ikke sunn") {
            Jdbc
                .initialize(
                    config =
                        JdbcConfig(
                            jdbcUrl = "jdbc:h2:mem:${UUID.randomUUID()}",
                            username = "sa",
                            password = "",
                        ),
                ).use { dataSource ->
                    dataSource.close()

                    dataSource.isHealthy() shouldBe false
                }
        }
    })
