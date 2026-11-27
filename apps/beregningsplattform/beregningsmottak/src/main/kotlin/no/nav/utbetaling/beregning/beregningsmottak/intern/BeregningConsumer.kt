package no.nav.utbetaling.beregning.beregningsmottak.intern

import com.ibm.mq.jakarta.jms.MQQueue
import jakarta.jms.TextMessage

import no.nav.utbetaling.beregning.beregningsmottak.Beregningslytter
import no.nav.utbetaling.beregning.beregningsmottak.Stormaskinberegning
import no.nav.utbetaling.beregning.log.Log
import no.nav.utbetaling.beregning.mq.DefaultMQ
import no.nav.utbetaling.beregning.mq.DefaultMQConsumer
import no.nav.utbetaling.beregning.mq.DefaultMQProducer
import no.nav.utbetaling.beregning.mq.MQConfig

internal class BeregningConsumer(
    config: Map<String, String> = System.getenv(),
    private val beregningslyttere: List<Beregningslytter> = emptyList(),
) : AutoCloseable {
    private fun Map<String, String>.configFor(key: String): String = requireNotNull(this[key]) { "Mangler config for $key" }

    private val mqConfig =
        MQConfig(
            host = config.configFor("BEREGNINGSMOTTAK_MQ_HOST"),
            port = config.configFor("BEREGNINGSMOTTAK_MQ_PORT").toInt(),
            channel = config.configFor("BEREGNINGSMOTTAK_MQ_CHANNEL"),
            manager = config.configFor("BEREGNINGSMOTTAK_MQ_MANAGER"),
            username = config.configFor("BEREGNINGSMOTTAK_MQ_USERNAME"),
            password = config.configFor("BEREGNINGSMOTTAK_MQ_PASSWORD"),
        )

    private val mq = DefaultMQ(mqConfig)
    private val queuenavn = config.configFor("BEREGNINGSMOTTAK_MQ_QUEUE")
    private val consumer = DefaultMQConsumer(mq, MQQueue(queuenavn), ::onMessage)
    private val errorProducer = DefaultMQProducer(mq, MQQueue("${queuenavn}_BOQ"))

    fun start() {
        Log.info("Starter ikke beregningsmottak sin MQ-consumer ennå")
        // consumer.start()
    }

    override fun close() {
        // consumer.close()
    }

    private fun onMessage(message: TextMessage) {
        // TODO mappe message til en Stormaskinberegning
        val stormaskinberegning = Stormaskinberegning(todo = "TODO")
        beregningslyttere.forEach { it.nyBeregning(stormaskinberegning) }
    }
}
