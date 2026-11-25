package no.nav.utbetaling.beregning.trekk

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import com.ibm.mq.jakarta.jms.MQQueue
import com.zaxxer.hikari.HikariDataSource
import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing

import no.nav.utbetaling.beregning.jdbc.Jdbc
import no.nav.utbetaling.beregning.jdbc.isHealthy
import no.nav.utbetaling.beregning.jdbc.migrate
import no.nav.utbetaling.beregning.log.Log
import no.nav.utbetaling.beregning.mq.DefaultMQ
import no.nav.utbetaling.beregning.mq.MQConfig
import no.nav.utbetaling.beregning.trekk.Metrics.prometheusMeterRegistry
import no.nav.utbetaling.beregning.trekk.config.ApplicationState
import no.nav.utbetaling.beregning.trekk.config.PropertiesConfig
import no.nav.utbetaling.beregning.trekk.config.applicationLifecycleConfig
import no.nav.utbetaling.beregning.trekk.config.commonConfig
import no.nav.utbetaling.beregning.trekk.config.internalNaisRoutes
import no.nav.utbetaling.beregning.trekk.config.loadEnv
import no.nav.utbetaling.beregning.trekk.config.securityConfig
import no.nav.utbetaling.beregning.trekk.mq.TrekkConsumer

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::trekk).start(true)
}

private fun Application.trekk() {
    PropertiesConfig.load(environment.config.loadEnv())
    val applicationState = ApplicationState()
    val dataSource = initializeDatabase(PropertiesConfig.postgresProperties)

    applicationLifecycleConfig(applicationState)
    commonConfig()
    securityConfig(PropertiesConfig.azureAdProperties)
    routing {
        internalNaisRoutes(
            applicationState,
            readynessCheck = { applicationState.ready && withContext(Dispatchers.IO) { dataSource.isHealthy() } },
        )
    }

    Log.info("=== Kjører database migrasjoner ===")
    dataSource.migrate()
    Log.info("=== Database migrasjoner fullført ===")

    val mqConfig =
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

    val trekkConsumer =
        TrekkConsumer(
            trekkService = TrekkService(dataSource),
            mq = mqConfig,
            listenerQueue = MQQueue(PropertiesConfig.mqProperties.trekkQueue),
            errorQueue = MQQueue(PropertiesConfig.mqProperties.trekkfeilQueue),
        )

    monitor.subscribe(ApplicationStarted) {
        Log.info("Bruker profil: ${PropertiesConfig.applicationProperties.profile}")
        trekkConsumer.start()
        Log.info("Trekk startet og lytter på kø: ${PropertiesConfig.mqProperties.trekkQueue}")
    }
    monitor.subscribe(ApplicationStopping) {
        Log.info("Trekk applikasjonen stopper")
        trekkConsumer.close()
        Log.info("MQ-konsument avsluttet lytting på kø: ${PropertiesConfig.mqProperties.trekkQueue}")
    }
}

internal fun Application.initializeDatabase(postgresProperties: PropertiesConfig.PostgresProperties): HikariDataSource {
    val dataSource =
        Jdbc.initialize(
            config = postgresProperties.toJdbcConfig(),
        ) {
            poolName = postgresProperties.name
            metricsTrackerFactory = MicrometerMetricsTrackerFactory(prometheusMeterRegistry)
        }
    monitor.subscribe(ApplicationStopped) {
        dataSource.close()
    }
    return dataSource
}
