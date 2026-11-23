package no.nav.utbetaling.beregning.beregningsplattform

import com.ibm.mq.jakarta.jms.MQQueue
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
import no.nav.utbetaling.beregning.beregningsplattform.mq.OppdragXmlConsumer
import no.nav.utbetaling.beregning.jdbc.Jdbc
import no.nav.utbetaling.beregning.log.Log
import no.nav.utbetaling.beregning.mq.DefaultMQ
import no.nav.utbetaling.beregning.mq.MQConfig

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::beregningsplattform).start(true)
}

private fun Application.beregningsplattform() {
    PropertiesConfig.load(environment.config.loadEnv())
    val applicationState = ApplicationState()

    val dataSource = initializeDatabase(PropertiesConfig.postgresProperties)

    applicationLifecycleConfig(applicationState)
    commonConfig()
    securityConfig(PropertiesConfig.azureAdProperties)
    routingConfig(
        applicationState = applicationState,
        dataSource = dataSource,
    )

    val mottakXMLQueue =
        DefaultMQ(
            MQConfig(
                host = PropertiesConfig.mqProperties.hostname,
                port = PropertiesConfig.mqProperties.port,
                channel = PropertiesConfig.mqProperties.channel,
                manager = PropertiesConfig.mqProperties.manager,
                username = PropertiesConfig.mqProperties.username,
                password = PropertiesConfig.mqProperties.password,
            ),
        )

    val oppdragXmlConsumer = OppdragXmlConsumer(mottakXMLQueue, MQQueue(PropertiesConfig.mqProperties.oppdragXmlQueue))

    monitor.subscribe(ApplicationStarted) {
        Log.info("Using profile: ${PropertiesConfig.applicationProperties.profile}")
        oppdragXmlConsumer.start()
        Log.info("Mottak application started and listening for messages on queue: ${PropertiesConfig.mqProperties.oppdragXmlQueue}")
    }
    monitor.subscribe(ApplicationStopping) {
        Log.info("Mottak application stopping")
        oppdragXmlConsumer.close()
        Log.info("MQ consumer closed for queue: ${PropertiesConfig.mqProperties.oppdragXmlQueue}")
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
