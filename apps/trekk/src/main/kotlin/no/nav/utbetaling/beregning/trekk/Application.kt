package no.nav.utbetaling.beregning.trekk

import com.ibm.mq.jakarta.jms.MQQueue
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing

import no.nav.utbetaling.beregning.log.Log
import no.nav.utbetaling.beregning.mq.DefaultMQ
import no.nav.utbetaling.beregning.mq.MQConfig
import no.nav.utbetaling.beregning.trekk.config.ApplicationState
import no.nav.utbetaling.beregning.trekk.config.DatabaseConfig
import no.nav.utbetaling.beregning.trekk.config.PropertiesConfig
import no.nav.utbetaling.beregning.trekk.config.applicationLifecycleConfig
import no.nav.utbetaling.beregning.trekk.config.commonConfig
import no.nav.utbetaling.beregning.trekk.config.databaseHealthy
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
    val dataSource = DatabaseConfig.dataSource

    applicationLifecycleConfig(applicationState)
    commonConfig()
    securityConfig(PropertiesConfig.azureAdProperties)
    routing {
        internalNaisRoutes(
            applicationState,
            readynessCheck = { applicationState.ready && databaseHealthy(dataSource) },
        )
    }

    Log.info("=== Running database migrations ===")
    DatabaseConfig.migrate()
    Log.info("=== Database migrations completed ===")

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
        Log.info("Trekk stopper")
        trekkConsumer.close()
        Log.info("MQ-konsument avsluttet lytting på kø: ${PropertiesConfig.mqProperties.trekkQueue}")
    }
}
