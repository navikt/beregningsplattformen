package no.nav.utbetaling.beregning.trekk.mq

import kotlinx.serialization.json.Json

import com.ibm.mq.jakarta.jms.MQQueue
import jakarta.jms.Destination
import jakarta.jms.Message
import jakarta.jms.Queue
import jakarta.jms.TextMessage

import no.nav.utbetaling.beregning.log.Log
import no.nav.utbetaling.beregning.log.secureLog
import no.nav.utbetaling.beregning.mq.DefaultMQConsumer
import no.nav.utbetaling.beregning.mq.DefaultMQProducer
import no.nav.utbetaling.beregning.mq.MQ
import no.nav.utbetaling.beregning.trekk.Metrics
import no.nav.utbetaling.beregning.trekk.TrekkService
import no.nav.utbetaling.beregning.trekk.dto.TrekkDTO

class TrekkConsumer(
    private val trekkService: TrekkService,
    mq: MQ,
    listenerQueue: MQQueue,
    errorQueue: MQQueue,
) : AutoCloseable {
    private val consumer = DefaultMQConsumer(mq, listenerQueue, ::onMessage)
    private val errorProducer = DefaultMQProducer(mq, errorQueue)

    fun start() {
        consumer.start()
    }

    override fun close() {
        consumer.close()
    }

    private fun onMessage(message: TextMessage) {
        val jmsMessage =
            runCatching { message.getBody(String::class.java) }
                .getOrElse { exception ->
                    Log.error("Kunne ikke lese beregningsdata. ${message.logContext()}. Feil:", exception)
                    secureLog.error("Mottatt trekkvedtak fra OppdragZ feilet: $message")
                    return
                }
        runCatching {
            secureLog.info("Mottatt trekkvedtak fra OppdragZ: $jmsMessage")
            trekkService.lagreTrekkvedtakBevis(jmsMessage)
            val trekkWrapper = Json.decodeFromString<TrekkDTO>(jmsMessage)
            val trekk = trekkWrapper.trekk ?: throw IllegalArgumentException("Trekkdata mangler i meldingen")
            trekkService.lagEllerOppdaterTrekkvedtak(trekk)
            Metrics.trekkOkCounter.inc()
            message.acknowledge()
        }.onFailure { exception ->
            Log.error("Prosessering av trekkvedtak ${message.jmsMessageID} feilet: ${exception.message} ")
            secureLog.error("Mottatt trekkvedtak fra OppdragZ feilet: $jmsMessage")
            errorProducer.produce(jmsMessage)
            Metrics.trekkFeiletCounter.inc()
            message.acknowledge()
        }
    }

    private fun Message.logContext(): String = "messageId=${jmsMessageID ?: "ukjent"}, replyTo=${jmsReplyTo?.destinationName() ?: "mangler"}"

    private fun Destination.destinationName(): String =
        when (this) {
            is Queue -> queueName
            else -> toString()
        }
}
