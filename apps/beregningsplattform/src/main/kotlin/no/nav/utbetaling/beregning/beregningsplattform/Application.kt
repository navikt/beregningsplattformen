package no.nav.utbetaling.beregning.beregningsplattform

import com.zaxxer.hikari.HikariDataSource
import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

import no.nav.utbetaling.beregning.beregningsplattform.config.ApplicationState
import no.nav.utbetaling.beregning.beregningsplattform.config.PropertiesConfig
import no.nav.utbetaling.beregning.beregningsplattform.config.applicationLifecycleConfig
import no.nav.utbetaling.beregning.beregningsplattform.config.commonConfig
import no.nav.utbetaling.beregning.beregningsplattform.config.loadEnv
import no.nav.utbetaling.beregning.beregningsplattform.config.routingConfig
import no.nav.utbetaling.beregning.beregningsplattform.config.securityConfig
import no.nav.utbetaling.beregning.beregningsplattform.metrics.Metrics
import no.nav.utbetaling.beregning.jdbc.Jdbc
import no.nav.utbetaling.beregning.jdbc.migrate
import no.nav.utbetaling.beregning.log.Log
import no.nav.utbetaling.beregning.mq.MQConfig
import no.nav.utbetaling.beregning.oppdragsmottak.OppdragXmlConsumer

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::beregningsplattform).start(true)
}

private fun Application.beregningsplattform() {
    PropertiesConfig.load(environment.config.loadEnv())
    val applicationState = ApplicationState()

    val dataSource = initializeDatabase(PropertiesConfig.postgresProperties)
    Log.info("Starter alle databasemigreringer")
    Log.info("Kjører migreringer for skattekort")
    dataSource.migrate(schema = "skattekort") {
        locations("classpath:db/migration/skattekort")
    }
    Log.info("Migreringer for skattekort er fullført")

    // Log.info("Kjører migreringer for abc")
    // dataSource.migrate(schema = "mottak") {
    //     locations("classpath:db/migration/mo")
    // }
    // Log.info("Migreringer for abc er fullført")

    Log.info("Alle databasemigreringer er fullført")

    applicationLifecycleConfig(applicationState)
    commonConfig()
    securityConfig(PropertiesConfig.azureAdProperties)
    routingConfig(
        applicationState = applicationState,
        dataSource = dataSource,
    )

    val oppdragXmlConsumer =
        OppdragXmlConsumer(
            mqConfig =
                MQConfig(
                    host = PropertiesConfig.mqProperties.hostname,
                    port = PropertiesConfig.mqProperties.port,
                    channel = PropertiesConfig.mqProperties.channel,
                    manager = PropertiesConfig.mqProperties.manager,
                    username = PropertiesConfig.mqProperties.username,
                    password = PropertiesConfig.mqProperties.password,
                ),
            queueName = PropertiesConfig.mqProperties.oppdragXmlQueue,
        )

    monitor.subscribe(ApplicationStarted) {
        Log.info("Bruker profil: ${PropertiesConfig.applicationProperties.profile}")
        oppdragXmlConsumer.start()
        Log.info("Beregningsplattform applikasjonen startet og lytter på kø: ${PropertiesConfig.mqProperties.oppdragXmlQueue}")
    }
    monitor.subscribe(ApplicationStopping) {
        Log.info("Beregningsplattform applikasjonen stopper")
        oppdragXmlConsumer.close()
        Log.info("MQ-konsument avsluttet lytting på kø: ${PropertiesConfig.mqProperties.oppdragXmlQueue}")
    }
}

internal fun Application.initializeDatabase(postgresProperties: PropertiesConfig.PostgresProperties): HikariDataSource {
    val dataSource =
        Jdbc.initialize(
            config = postgresProperties.toJdbcConfig(),
        ) {
            poolName = postgresProperties.name
            metricsTrackerFactory = MicrometerMetricsTrackerFactory(Metrics.prometheusMeterRegistry)
        }
    monitor.subscribe(ApplicationStopped) {
        dataSource.close()
    }
    return dataSource
}
