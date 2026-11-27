package no.nav.utbetaling.beregning.trekk.mq

import java.time.LocalDateTime

import com.ibm.mq.jakarta.jms.MQQueue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import jakarta.jms.JMSConsumer
import jakarta.jms.JMSContext
import jakarta.jms.JMSException
import jakarta.jms.JMSProducer
import jakarta.jms.JMSRuntimeException
import jakarta.jms.MessageListener
import jakarta.jms.TextMessage

import no.nav.utbetaling.beregning.mq.MQ
import no.nav.utbetaling.beregning.trekk.Metrics
import no.nav.utbetaling.beregning.trekk.TrekkService
import no.nav.utbetaling.beregning.trekk.dto.Fagomraader
import no.nav.utbetaling.beregning.trekk.dto.Perioder
import no.nav.utbetaling.beregning.trekk.dto.Trekk

internal class TrekkConsumerTest :
    FunSpec({
        lateinit var fixture: ConsumerFixture

        beforeEach {
            fixture = ConsumerFixture()
        }

        afterEach {
            fixture.consumer.close()
        }

        test("lagrer bevis og behandler gyldig JSON før meldingen kvitteres") {
            val melding = fixture.message(gyldigMelding)
            val suksesser = Metrics.trekkOkCounter.get()
            val feil = Metrics.trekkFeiletCounter.get()
            val forventet =
                Trekk(
                    trekkvedtakId = 42L,
                    kodeTrekktype = "TEST",
                    debitorId = "testDebitor",
                    kodeTrekkAlternativ = "LOPM",
                    brukerId = "testUser",
                    tidsPktReg = LocalDateTime.of(2026, 11, 27, 12, 0),
                    fagomraader = Fagomraader(),
                    perioder = Perioder(),
                )

            fixture.deliver(melding)

            verify(exactly = 1) { fixture.service.lagreTrekkvedtakBevis(gyldigMelding) }
            verify(exactly = 1) { fixture.service.lagEllerOppdaterTrekkvedtak(forventet) }
            verify(exactly = 1) { melding.acknowledge() }
            verifyOrder {
                fixture.service.lagreTrekkvedtakBevis(gyldigMelding)
                fixture.service.lagEllerOppdaterTrekkvedtak(forventet)
                melding.acknowledge()
            }
            verify(exactly = 0) { fixture.errorContext.createProducer() }
            Metrics.trekkOkCounter.get() shouldBe suksesser + 1
            Metrics.trekkFeiletCounter.get() shouldBe feil
        }

        listOf(
            "malformed JSON" to "ugyldig json",
            "melding uten trekkfelt" to "{}",
            "melding med null i trekkfeltet" to """{"trekk":null}""",
            "melding uten obligatoriske trekkfeltverdier" to """{"trekk":{}}""",
        ).forEach { (description, body) ->
            test("$description sendes til feilkøen før meldingen kvitteres") {
                val melding = fixture.message(body)
                val suksesser = Metrics.trekkOkCounter.get()
                val feil = Metrics.trekkFeiletCounter.get()

                fixture.deliver(melding)

                verify(exactly = 1) { fixture.service.lagreTrekkvedtakBevis(body) }
                verify(exactly = 0) { fixture.service.lagEllerOppdaterTrekkvedtak(any()) }
                verify(exactly = 1) { fixture.errorContext.createTextMessage(body) }
                verify(exactly = 1) { fixture.producer.send(fixture.errorQueue, fixture.forwardedMessage) }
                verify(exactly = 1) { melding.acknowledge() }
                verifyOrder {
                    fixture.service.lagreTrekkvedtakBevis(body)
                    fixture.producer.send(fixture.errorQueue, fixture.forwardedMessage)
                    melding.acknowledge()
                }
                Metrics.trekkOkCounter.get() shouldBe suksesser
                Metrics.trekkFeiletCounter.get() shouldBe feil + 1
            }
        }

        test("sender originalmeldingen til feilkøen ved behandlingsfeil før meldingen kvitteres") {
            val melding = fixture.message(gyldigMelding)
            val suksesser = Metrics.trekkOkCounter.get()
            val feil = Metrics.trekkFeiletCounter.get()
            every { fixture.service.lagEllerOppdaterTrekkvedtak(any()) } throws
                IllegalStateException("Trekkdatabasen ikke tilgjengelig")

            fixture.deliver(melding)

            verify(exactly = 1) { fixture.service.lagreTrekkvedtakBevis(gyldigMelding) }
            verify(exactly = 1) { fixture.service.lagEllerOppdaterTrekkvedtak(any()) }
            verify(exactly = 1) { fixture.errorContext.createTextMessage(gyldigMelding) }
            verify(exactly = 1) { fixture.producer.send(fixture.errorQueue, fixture.forwardedMessage) }
            verify(exactly = 1) { melding.acknowledge() }
            verifyOrder {
                fixture.service.lagreTrekkvedtakBevis(gyldigMelding)
                fixture.service.lagEllerOppdaterTrekkvedtak(any())
                fixture.producer.send(fixture.errorQueue, fixture.forwardedMessage)
                melding.acknowledge()
            }
            Metrics.trekkOkCounter.get() shouldBe suksesser
            Metrics.trekkFeiletCounter.get() shouldBe feil + 1
        }

        test("sender meldingen til feilkøen uten å behandle trekkvedtaket når lagring av bevis feiler") {
            val melding = fixture.message(gyldigMelding)
            val suksesser = Metrics.trekkOkCounter.get()
            val feil = Metrics.trekkFeiletCounter.get()
            every { fixture.service.lagreTrekkvedtakBevis(any()) } throws
                IllegalStateException("Bevistabellen ikke tilgjengelig")

            fixture.deliver(melding)

            verify(exactly = 1) { fixture.service.lagreTrekkvedtakBevis(gyldigMelding) }
            verify(exactly = 0) { fixture.service.lagEllerOppdaterTrekkvedtak(any()) }
            verify(exactly = 1) { fixture.errorContext.createTextMessage(gyldigMelding) }
            verify(exactly = 1) { fixture.producer.send(fixture.errorQueue, fixture.forwardedMessage) }
            verify(exactly = 1) { melding.acknowledge() }
            verifyOrder {
                fixture.service.lagreTrekkvedtakBevis(gyldigMelding)
                fixture.producer.send(fixture.errorQueue, fixture.forwardedMessage)
                melding.acknowledge()
            }
            Metrics.trekkOkCounter.get() shouldBe suksesser
            Metrics.trekkFeiletCounter.get() shouldBe feil + 1
        }

        test("kaster exception uten å kvittere meldingen når sending til feilkøen feiler") {
            val meldingsinnhold = "ugyldig JSON"
            val melding = fixture.message(meldingsinnhold)
            val suksesser = Metrics.trekkOkCounter.get()
            val feil = Metrics.trekkFeiletCounter.get()
            val failure = JMSRuntimeException("Feilkø ikke tilgjengelig")
            every { fixture.producer.send(fixture.errorQueue, fixture.forwardedMessage) } throws failure

            shouldThrow<JMSRuntimeException> {
                fixture.deliver(melding)
            } shouldBe failure

            verify(exactly = 1) { fixture.producer.send(fixture.errorQueue, fixture.forwardedMessage) }
            verify(exactly = 0) { melding.acknowledge() }
            verify(exactly = 1) { fixture.service.lagreTrekkvedtakBevis(meldingsinnhold) }
            verify(exactly = 0) { fixture.service.lagEllerOppdaterTrekkvedtak(any()) }
            Metrics.trekkOkCounter.get() shouldBe suksesser
            Metrics.trekkFeiletCounter.get() shouldBe feil
        }

        test("avslutter uten å lagre bevis, behandle trekkvedtaket eller kvittere når meldingsinnholdet ikke kan leses") {
            val melding = fixture.message(gyldigMelding)
            val suksesser = Metrics.trekkOkCounter.get()
            val feil = Metrics.trekkFeiletCounter.get()
            val failure = JMSException("Kan ikke lese meldingsinnholdet")
            every { melding.getBody(String::class.java) } throws failure

            fixture.deliver(melding)

            verify(exactly = 0) { fixture.service.lagreTrekkvedtakBevis(any()) }
            verify(exactly = 0) { fixture.service.lagEllerOppdaterTrekkvedtak(any()) }
            verify(exactly = 0) { fixture.errorContext.createProducer() }
            verify(exactly = 0) { melding.acknowledge() }
            Metrics.trekkOkCounter.get() shouldBe suksesser
            Metrics.trekkFeiletCounter.get() shouldBe feil
        }
    })

private class ConsumerFixture {
    val service = mockk<TrekkService>(relaxUnitFun = true)
    val context = mockk<JMSContext>(relaxed = true)
    val errorContext = mockk<JMSContext>(relaxed = true)
    val jmsConsumer = mockk<JMSConsumer>(relaxed = true)
    val producer = mockk<JMSProducer>(relaxed = true)
    val forwardedMessage = mockk<TextMessage>(relaxed = true)
    val errorQueue = MQQueue("test.error")
    private val listener = slot<MessageListener>()
    val consumer: TrekkConsumer

    init {
        val listenerQueue = MQQueue("test.incoming")
        every { service.lagreTrekkvedtakBevis(any()) } returns 1
        every { context.createConsumer(listenerQueue) } returns jmsConsumer
        every { jmsConsumer.messageListener = capture(listener) } just Runs
        every { errorContext.createProducer() } returns producer
        every { errorContext.createTextMessage(any()) } returns forwardedMessage
        every { forwardedMessage.jmsMessageID } returns "ID:forwarded"
        val mq =
            object : MQ {
                override val context: JMSContext = this@ConsumerFixture.context

                override fun depth(queue: MQQueue): Int = error("Kø-dybde ikke brukt av TrekkConsumer")

                override fun <T : Any> transaction(block: (JMSContext) -> T): T = block(errorContext)

                override fun <T : Any> transacted(
                    ctx: JMSContext,
                    block: () -> T,
                ): T = block()
            }
        consumer = TrekkConsumer(service, mq, listenerQueue, errorQueue)
    }

    fun message(body: String): TextMessage =
        mockk<TextMessage>(relaxed = true) {
            every { getBody(String::class.java) } returns body
            every { jmsMessageID } returns "ID:incoming"
        }

    fun deliver(message: TextMessage) {
        listener.captured.onMessage(message)
    }
}

private val gyldigMelding =
    """
    {
      "trekk": {
        "trekkvedtakId": 42,
        "kodeTrekktype": "TEST",
        "debitorId": "testDebitor",
        "kodeTrekkAlternativ": "LOPM",
        "brukerId": "testUser",
        "tidsPktReg": "2026-11-27T12:00:00",
        "fagomraader": {},
        "perioder": {}
      }
    }
    """.trimIndent()
