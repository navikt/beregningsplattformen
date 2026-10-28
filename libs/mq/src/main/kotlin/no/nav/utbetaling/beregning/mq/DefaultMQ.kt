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
import jakarta.jms.MessageListener
import jakarta.jms.TextMessage

import no.nav.utbetaling.beregning.log.Log
import no.nav.utbetaling.beregning.log.logger

val mqLog = logger("mq")

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
            val message = ctx.createTextMessage(message)

            fun spanBuilder(builder: SpanBuilder): SpanBuilder =
                builder
                    .setAttribute("messaging.system", "ibmmq")
                    .setAttribute("messaging.destination", queue.baseQueueName)
                    .setAttribute("messaging.operation", "send")

            Tracing.startSpan(
                "mq.produce",
                ::spanBuilder,
            ) {
                producer.send(queue, message)
                message.jmsCorrelationID = message.jmsMessageID
                Tracing.storeContext(message.jmsMessageID)
                it.setAttribute("messaging.message_id", message.jmsMessageID)
            }
            message.jmsMessageID
        }
    }
}

interface MQConsumer :
    AutoCloseable,
    ExceptionListener {
    // fun onMessage(message: TextMessage)
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
            messageListener =
                MessageListener {
                    val message = it as TextMessage
                    Log.info("Consuming message on ${queue.baseQueueName}")
                    mq.transacted(context) {
                        val context = Tracing.restoreContext(it.jmsCorrelationID)

                        fun spanBuilder(builder: SpanBuilder): SpanBuilder =
                            builder
                                .setAttribute("messaging.system", "ibmmq")
                                .setAttribute("messaging.destination", queue.baseQueueName)
                                .setAttribute("messaging.destination_kind", "queue")
                                .setAttribute("messaging.message_id", message.jmsMessageID)
                                .setAttribute("messaging.operation", "receive")
                                .setParent(context)
                        Tracing.startSpan(
                            queue.baseQueueName,
                            ::spanBuilder,
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
        }

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
            val backoff = minOf((10 * attempt).seconds, maxBackoff)
            Log.info("Reconnect attempt $attempt for ${queue.baseQueueName}, backoff $backoff")
            Thread.sleep(backoff.inWholeMilliseconds)
            try {
                runCatching { close() }
                context = createContext()
                consumer = createConsumer()
                start()
                Log.info("Sucessfully reconnected MQ ${queue.baseQueueName} after $attempt attempts")
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
        consumer.close()
        context.close()
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

        val result =
            runCatching {
                block()
            }.onSuccess {
                ctx.commit()
                Log.debug("MQ transaction committed", "$ctx")
            }.onFailure {
                ctx.rollback()
                Log.error("MQ transaction rolled back, please check secureLogs or backout queue", "$ctx", it)
            }

        return result.getOrThrow()
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
