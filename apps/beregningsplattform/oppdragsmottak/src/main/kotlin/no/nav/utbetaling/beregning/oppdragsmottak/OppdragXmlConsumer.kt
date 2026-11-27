package no.nav.utbetaling.beregning.oppdragsmottak

import com.ibm.mq.jakarta.jms.MQQueue
import jakarta.jms.TextMessage

import no.nav.utbetaling.beregning.log.secureLog
import no.nav.utbetaling.beregning.mq.DefaultMQ
import no.nav.utbetaling.beregning.mq.DefaultMQConsumer
import no.nav.utbetaling.beregning.mq.MQConfig
import no.nav.utbetaling.beregning.oppdrag.Oppdrag
import no.nav.utbetaling.beregning.oppdragsmottak.metrics.Metrics

class OppdragXmlConsumer(
    mqConfig: MQConfig,
    queueName: String,
) : AutoCloseable {
    private val consumer = DefaultMQConsumer(DefaultMQ(mqConfig), MQQueue(queueName), ::onMessage)

    fun start() {
        consumer.start()
    }

    override fun close() {
        consumer.close()
    }

    private fun onMessage(message: TextMessage) {
        val oppdrag = Oppdrag.fraXml(message.text)
        // TODO: Lagre i database
        Metrics.registrerOppdrag(oppdrag)
        secureLog.info("Received MQ message: {}", oppdrag.toString())
    }
}
