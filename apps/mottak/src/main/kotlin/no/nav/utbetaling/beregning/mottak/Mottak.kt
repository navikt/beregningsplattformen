package no.nav.utbetaling.beregning.mottak

import com.ibm.mq.jakarta.jms.MQQueue
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing

import no.nav.utbetaling.beregning.log.Log
import no.nav.utbetaling.beregning.mottak.config.ApplicationState
import no.nav.utbetaling.beregning.mottak.config.PropertiesConfig
import no.nav.utbetaling.beregning.mottak.config.applicationLifecycleConfig
import no.nav.utbetaling.beregning.mottak.config.commonConfig
import no.nav.utbetaling.beregning.mottak.config.internalNaisRoutes
import no.nav.utbetaling.beregning.mottak.config.loadEnv
import no.nav.utbetaling.beregning.mottak.config.securityConfig
import no.nav.utbetaling.beregning.mq.DefaultMQ
import no.nav.utbetaling.beregning.mq.MQConfig

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::mottak).start(true)
}

private fun Application.mottak() {
    PropertiesConfig.load(environment.config.loadEnv())
    val applicationState = ApplicationState()

    applicationLifecycleConfig(applicationState)
    commonConfig()
    securityConfig(PropertiesConfig.azureAdProperties)
    routing {
        internalNaisRoutes(applicationState)
    }

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

    Log.info("Får jeg hentet ut servicename: ${PropertiesConfig.mqProperties.username}")
    Log.info("Starting Mottak application with MQ queue: ${PropertiesConfig.mqProperties.mottakXmlQueue}")

    val mottakXMLConsumer = MottakXMLConsumer(mottakXMLQueue, MQQueue(PropertiesConfig.mqProperties.mottakXmlQueue))

    Log.info("Mottak application started and listening for messages on queue: ${PropertiesConfig.mqProperties.mottakXmlQueue}")

    monitor.subscribe(ApplicationStarted) {
        mottakXMLConsumer.start()
    }
    monitor.subscribe(ApplicationStopping) {
        mottakXMLConsumer.close()
    }

    Log.info("Application started with profile: ${PropertiesConfig.applicationProperties.profile}")
}
