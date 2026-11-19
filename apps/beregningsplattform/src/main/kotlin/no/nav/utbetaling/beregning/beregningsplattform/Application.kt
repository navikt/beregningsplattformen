package no.nav.utbetaling.beregning.beregningsplattform

import com.ibm.mq.jakarta.jms.MQQueue
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

import no.nav.utbetaling.beregning.beregningsplattform.config.routingConfig
import no.nav.utbetaling.beregning.beregningsplattform.mq.OppdragXmlConsumer
import no.nav.utbetaling.beregning.log.Log
import no.nav.utbetaling.beregning.mq.DefaultMQ
import no.nav.utbetaling.beregning.mq.MQConfig

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::beregningsplattform).start(true)
}

private fun Application.beregningsplattform() {
    PropertiesConfig.load(environment.config.loadEnv())
    val applicationState = ApplicationState()

    applicationLifecycleConfig(applicationState)
    commonConfig()
    securityConfig(PropertiesConfig.azureAdProperties)
    routingConfig(
        applicationState = applicationState,
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
