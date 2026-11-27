package no.nav.utbetaling.beregning.beregningsmottak

import com.ibm.mq.MQC
import com.ibm.mq.jakarta.jms.MQQueue
import jakarta.jms.TextMessage
import no.nav.utbetaling.beregning.mq.DefaultMQ
import no.nav.utbetaling.beregning.mq.DefaultMQConsumer
import no.nav.utbetaling.beregning.mq.MQConfig

class BeregningConsumer() : AutoCloseable {
    val mqConfig = MQConfig(

    )
    private val consumer = DefaultMQConsumer(DefaultMQ(mqConfig), MQQueue(queueName), ::onMessage)

    fun start() {
        consumer.start()
    }

    override fun close() {
        consumer.close()
    }

    private fun onMessage(message: TextMessage) {
        //val oppdrag = Oppdrag.fraXml(message.text)
        // TODO: Lagre i database
        //Metrics.registrerOppdrag(oppdrag)
        //secureLog.info("Received MQ message: {}", oppdrag.toString())
    }
}
