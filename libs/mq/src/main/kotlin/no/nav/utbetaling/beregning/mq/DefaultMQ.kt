package no.nav.utbetaling.beregning.mq

import java.util.UUID

import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

import com.ibm.mq.constants.CMQC
import com.ibm.mq.jakarta.jms.MQConnectionFactory
import com.ibm.mq.jakarta.jms.MQQueue
import com.ibm.msg.client.jakarta.jms.JmsConstants
import com.ibm.msg.client.jakarta.wmq.WMQConstants
import io.opentelemetry.api.trace.SpanBuilder
import io.opentelemetry.api.trace.StatusCode
import jakarta.jms.ExceptionListener
import jakarta.jms.JMSConsumer
import jakarta.jms.JMSContext
import jakarta.jms.JMSException
import jakarta.jms.JMSProducer
import jakarta.jms.Message
import jakarta.jms.MessageListener
import jakarta.jms.TextMessage

import no.nav.utbetaling.beregning.log.Log

interface MQProducer {
    fun produce(
        message: String,
        config: JMSProducer.() -> Unit = {},
    ): String
}

class DefaultMQProducer(
    private val mq: MQ,
    private val queue: MQQueue,
) : MQProducer {
    override fun produce(
        message: String,
        config: JMSProducer.() -> Unit,
    ): String {
        Log.info("Producing message on $queue")
        return mq.transaction { ctx ->
            ctx.clientID = UUID.randomUUID().toString()
            val producer = ctx.createProducer().apply(config)
            val textMessage = ctx.createTextMessage(message)

            Tracing.startSpan(
                "mq.produce",
                { builder ->
                    builder
                        .setAttribute("messaging.system", "ibmmq")
                        .setAttribute("messaging.destination", queue.baseQueueName)
                        .setAttribute("messaging.operation", "send")
                },
            ) { span ->
                producer.send(queue, textMessage)
                textMessage.jmsCorrelationID = textMessage.jmsMessageID
                Tracing.storeContext(textMessage.jmsMessageID)
                span.setAttribute("messaging.message_id", textMessage.jmsMessageID)
            }

            textMessage.jmsMessageID
        }
    }
}

interface MQConsumer :
    AutoCloseable,
    ExceptionListener {
    fun start()
}

open class DefaultMQConsumer(
    private val mq: MQ,
    private val queue: MQQueue,
    private val onMessage: (TextMessage) -> Unit,
) : MQConsumer {
    private var context = createContext()
    private var consumer = createConsumer()

    private fun createContext(): JMSContext =
        mq.context.apply {
            autoStart = false
            exceptionListener = this@DefaultMQConsumer
        }

    private fun createConsumer(): JMSConsumer =
        context.createConsumer(queue).apply {
            messageListener = MessageListener(::consume)
        }

    private fun consume(message: Message) {
        Log.info("Consuming message on ${queue.baseQueueName}")
        mq.transacted(context) {
            require(message is TextMessage) {
                "Expected TextMessage on ${queue.baseQueueName}, received ${message.javaClass.name}"
            }
            Tracing.startSpan(
                queue.baseQueueName,
                { builder -> receiveSpan(builder, message) },
            ) { span ->
                try {
                    span.addEvent("message received")
                    onMessage(message)
                    span.addEvent("message processed")
                } catch (e: Exception) {
                    span.recordException(e)
                    span.setStatus(StatusCode.ERROR)
                    throw e
                }
            }
        }
    }

    private fun receiveSpan(
        builder: SpanBuilder,
        message: TextMessage,
    ): SpanBuilder =
        builder
            .setAttribute("messaging.system", "ibmmq")
            .setAttribute("messaging.destination", queue.baseQueueName)
            .setAttribute("messaging.destination_kind", "queue")
            .setAttribute("messaging.message_id", message.jmsMessageID)
            .setAttribute("messaging.operation", "receive")
            .setParent(Tracing.restoreContext(message.jmsCorrelationID))

    override fun onException(exception: JMSException) {
        Log.error("MQ ${queue.baseQueueName} failed, reconnecting...", exception)
        reconnect()
    }

    @Synchronized
    fun reconnect() {
        var attempt = 0
        val maxBackoff = 5.minutes
        while (true) {
            attempt++
            val backoff = minOf(10.seconds * attempt, maxBackoff)
            Log.info("Reconnect attempt $attempt for ${queue.baseQueueName}, backoff $backoff")
            Thread.sleep(backoff.inWholeMilliseconds)
            try {
                runCatching { close() }.onFailure {
                    Log.error("Failed to close MQ ${queue.baseQueueName} before reconnecting", it)
                }
                context = createContext()
                consumer = createConsumer()
                start()
                Log.info("Successfully reconnected MQ ${queue.baseQueueName} after $attempt attempts")
                return
            } catch (e: Exception) {
                Log.error("MQ reconnect ${queue.baseQueueName} failed, reconnecting...", e)
            }
        }
    }

    override fun start() {
        context.start()
    }

    override fun close() {
        context.use {
            consumer.close()
        }
    }
}

interface MQ {
    fun depth(queue: MQQueue): Int

    fun <T : Any> transaction(block: (JMSContext) -> T): T

    fun <T : Any> transacted(
        ctx: JMSContext,
        block: () -> T,
    ): T

    val context: JMSContext
}

class DefaultMQ(
    private val config: MQConfig,
) : MQ {
    private val factory: MQConnectionFactory =
        MQConnectionFactory().apply {
            hostName = config.host
            port = config.port
            queueManager = config.manager
            channel = config.channel
            transportType = WMQConstants.WMQ_CM_CLIENT
            ccsid = JmsConstants.CCSID_UTF8
            userAuthenticationMQCSP = true
            setIntProperty(JmsConstants.JMS_IBM_ENCODING, CMQC.MQENC_NATIVE)
            setIntProperty(JmsConstants.JMS_IBM_CHARACTER_SET, JmsConstants.CCSID_UTF8)
        }

    override val context: JMSContext
        get() =
            factory.createContext(
                config.username,
                config.password,
                JMSContext.SESSION_TRANSACTED,
            )

    override fun depth(queue: MQQueue): Int {
        Log.debug("Checking queue depth for ${queue.baseQueueName}")
        return transaction { ctx ->
            ctx.createBrowser(queue).use { browse ->
                browse.enumeration.toList().size
            }
        }
    }

    override fun <T : Any> transacted(
        ctx: JMSContext,
        block: () -> T,
    ): T {
        Log.debug("MQ transaction created", "$ctx")

        return try {
            val result = block()

            ctx.commit()
            Log.debug("MQ transaction committed", "$ctx")
            result
        } catch (failure: Throwable) {
            try {
                ctx.rollback()
            } catch (rollbackFailure: Throwable) {
                if (rollbackFailure !== failure) failure.addSuppressed(rollbackFailure)
                Log.error("MQ transaction rollback failed", "$ctx", rollbackFailure)
            }
            Log.error("MQ transaction failed, please check secureLogs or backout queue", "$ctx", failure)
            throw failure
        }
    }

    override fun <T : Any> transaction(block: (JMSContext) -> T): T =
        context.use { ctx ->
            transacted(ctx) {
                block(ctx)
            }
        }
}

data class MQConfig(
    val host: String,
    val port: Int,
    val channel: String,
    val manager: String,
    val username: String,
    val password: String,
)
