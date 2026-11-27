package no.nav.utbetaling.beregning.oppdragsmottak

import com.ibm.mq.jakarta.jms.MQQueue
import jakarta.jms.TextMessage

import no.nav.utbetaling.beregning.log.Log
import no.nav.utbetaling.beregning.log.secureLog
import no.nav.utbetaling.beregning.mq.DefaultMQ
import no.nav.utbetaling.beregning.mq.DefaultMQConsumer
import no.nav.utbetaling.beregning.mq.MQConfig
import no.nav.utbetaling.beregning.oppdrag.Oppdrag
import no.nav.utbetaling.beregning.oppdragsmottak.metrics.Metrics

internal class OppdragXmlConsumer(
    config: Map<String, String> = System.getenv(),
) : AutoCloseable {
    private fun Map<String, String>.configFor(key: String): String = requireNotNull(this[key]) { "Mangler config for $key" }

    private val mqConfig =
        MQConfig(
            host = config.configFor("OPPDRAGSMOTTAK_MQ_HOST"),
            port = config.configFor("OPPDRAGSMOTTAK_MQ_PORT").toInt(),
            channel = config.configFor("OPPDRAGSMOTTAK_MQ_CHANNEL"),
            manager = config.configFor("OPPDRAGSMOTTAK_MQ_MANAGER"),
            username = config.configFor("OPPDRAGSMOTTAK_MQ_USERNAME"),
            password = config.configFor("OPPDRAGSMOTTAK_MQ_PASSWORD"),
        )

    private val queueName = config.configFor("OPPDRAGSMOTTAK_MQ_QUEUE")
    private val consumer = DefaultMQConsumer(DefaultMQ(mqConfig), MQQueue(queueName), ::onMessage)

    fun start() {
        consumer.start()
        Log.info("Oppdragsmottak startet og lytter på kø: $queueName")
    }

    override fun close() {
        consumer.close()
        Log.info("Oppdragsmottak avsluttet lytting på kø: $queueName")
    }

    private fun onMessage(message: TextMessage) {
        val oppdrag = Oppdrag.fraXml(message.text)
        // TODO: Lagre i database
        Metrics.registrerOppdrag(oppdrag)
        secureLog.info("Received MQ message: {}", oppdrag.toString())
    }
}
