package no.nav.utbetaling.beregning.beregningsplattform

import com.zaxxer.hikari.HikariDataSource
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ServerReady
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.basic
import io.ktor.server.testing.testApplication
import org.testcontainers.DockerClientFactory

import no.nav.utbetaling.beregning.beregningsplattform.config.AUTHENTICATION_NAME
import no.nav.utbetaling.beregning.beregningsplattform.config.ApplicationState
import no.nav.utbetaling.beregning.beregningsplattform.config.PropertiesConfig
import no.nav.utbetaling.beregning.beregningsplattform.config.applicationLifecycleConfig
import no.nav.utbetaling.beregning.beregningsplattform.config.routingConfig
import no.nav.utbetaling.beregning.beregningsplattform.listener.PostgresListener

class DatabaseReadinessTest :
    FunSpec({
        extensions(PostgresListener)

        test("readiness feiler når PostgreSQL blir utilgjengelig, mens liveness består") {
            val container = PostgresListener.container
            val postgresProperties =
                PropertiesConfig.postgresProperties.copy(
                    jdbcUrl = container.jdbcUrl,
                    host = container.host,
                    port = container.firstMappedPort,
                )
            val applicationState = ApplicationState()
            lateinit var dataSource: HikariDataSource

            testApplication {
                application {
                    dataSource = initializeDatabase(postgresProperties)
                    applicationLifecycleConfig(applicationState)
                    install(Authentication) {
                        basic(name = AUTHENTICATION_NAME) {
                            validate { null }
                        }
                    }
                    routingConfig(applicationState, dataSource)
                }
                startApplication()

                dataSource.isClosed shouldBe false
                client.get("/internal/isReady").status shouldBe HttpStatusCode.InternalServerError
                // Testmotoren sender ikke ServerReady.
                application.monitor.raise(ServerReady, application.environment)
                client.get("/internal/isReady").status shouldBe HttpStatusCode.OK

                val dockerClient = DockerClientFactory.instance().client()
                dockerClient.pauseContainerCmd(container.containerId).exec()
                try {
                    client.get("/internal/isReady").status shouldBe HttpStatusCode.InternalServerError
                    client.get("/internal/isAlive").status shouldBe HttpStatusCode.OK
                } finally {
                    dockerClient.unpauseContainerCmd(container.containerId).exec()
                }

                client.get("/internal/isReady").status shouldBe HttpStatusCode.OK
            }
        }
    })
