package no.nav.utbetaling.beregning.mottak

import com.ibm.mq.jakarta.jms.MQQueue
import jakarta.jms.TextMessage

import no.nav.utbetaling.beregning.log.secureLog
import no.nav.utbetaling.beregning.mq.DefaultMQConsumer
import no.nav.utbetaling.beregning.mq.MQ
import no.nav.utbetaling.beregning.oppdrag.Oppdrag

class OppdragXMLConsumer(
    mq: MQ,
    queue: MQQueue,
) : AutoCloseable {
    private val consumer = DefaultMQConsumer(mq, queue, ::onMessage)

    fun start() {
        consumer.start()
    }

    override fun close() {
        consumer.close()
    }

    private fun onMessage(message: TextMessage) {
        val oppdrag = Oppdrag.fraXml(message.text)
        // TODO: Lagre i database
        secureLog.info("Received MQ message: {}", oppdrag.toString())
    }
}

class OppdragZProducer(
    private val mq: MQ,
    private val queue: MQQueue,
) {
    // TODO:
}
