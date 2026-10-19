package no.nav.utbetaling.beregning.utils

import java.net.InetAddress
import java.net.ServerSocket

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.LoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import org.slf4j.LoggerFactory
import org.slf4j.MDC

class LogTest :
    FunSpec({
        test("skal kode loggmeldinger som JSON med forventede felt") {
            val encoder = LogJsonEncoder()

            val json = encoder.encode(event("hello")).toString(Charsets.UTF_8)

            json shouldContain "\"message\":\"hello\""
            json shouldContain "\"logger_name\":\"test.Logger\""
            json shouldContain "\"thread_name\":\"main\""
            json shouldContain "\"level\":\"INFO\""
            json shouldContain "\"level_value\":20000"
            json shouldEndWith "\n"
        }

        test("skal escape spesialtegn i meldinger") {
            val json = LogJsonEncoder().encode(event("line1\nline2\ttab\"quote\\path\r")).toString(Charsets.UTF_8)

            json shouldContain "line1\\nline2\\ttab\\\"quote\\\\path\\r"
        }

        test("skal inkludere MDC fra tidspunktet hendelsen ble opprettet") {
            val previousMdc = MDC.getCopyOfContextMap()
            try {
                MDC.put("callId", "abc-123")
                val loggingEvent = event("x")
                MDC.clear()

                val json = LogJsonEncoder().encode(loggingEvent).toString(Charsets.UTF_8)

                json shouldContain "\"callId\":\"abc-123\""
            } finally {
                MDC.clear()
                previousMdc?.let(MDC::setContextMap)
            }
        }

        test("skal inkludere egendefinerte felt") {
            val encoder =
                LogJsonEncoder().apply {
                    customFields = """{"app":"lab"}"""
                }

            val json = encoder.encode(event("x")).toString(Charsets.UTF_8)

            json shouldContain "\"app\":\"lab\""
        }

        test("skal skrive logghendelser til en TCP-socket") {
            ServerSocket(0, 1, InetAddress.getLoopbackAddress()).use { server ->
                server.soTimeout = 5_000
                val appender =
                    LogTcpAppender().apply {
                        context = LoggerFactory.getILoggerFactory() as LoggerContext
                        destination = "localhost:${server.localPort}"
                        encoder = LogJsonEncoder().apply { start() }
                    }
                try {
                    appender.start()
                    server.accept().use { client ->
                        client.soTimeout = 5_000
                        appender.doAppend(event("tcp-test"))

                        val received = client.getInputStream().bufferedReader().readLine()

                        received shouldContain "\"message\":\"tcp-test\""
                    }
                } finally {
                    appender.stop()
                }
            }
        }

        test("skal logge feilsted offentlig og unntaket i sikkerloggen") {
            val app = LoggerFactory.getLogger("appLog") as Logger
            val secure = LoggerFactory.getLogger("secureLog") as Logger
            val previousAppLevel = app.level
            val previousSecureLevel = secure.level
            val previousMdc = MDC.getCopyOfContextMap()
            val appender = capture(app)
            val secureAppender = capture(secure)
            val error = IllegalStateException("boom")
            try {
                app.level = Level.ERROR
                secure.level = Level.ERROR
                Log.error("failed", error)

                val publicEvent = appender.list.single()
                publicEvent.formattedMessage shouldBe "failed"
                publicEvent.throwableProxy.shouldBeNull()
                publicEvent.mdcPropertyMap["location"]?.substringBefore(":") shouldBe "LogTest.kt"
                val secureEvent = secureAppender.list.single()
                secureEvent.throwableProxy.className shouldBe IllegalStateException::class.java.name
                secureEvent.throwableProxy.message shouldBe "boom"
            } finally {
                app.detachAppender(appender)
                secure.detachAppender(secureAppender)
                appender.stop()
                secureAppender.stop()
                app.level = previousAppLevel
                secure.level = previousSecureLevel
                MDC.clear()
                previousMdc?.let(MDC::setContextMap)
            }
        }

        test("skal fjerne feilsted fra MDC etter logging") {
            val previousMdc = MDC.getCopyOfContextMap()
            try {
                MDC.remove("location")

                Log.error("failed", RuntimeException())

                MDC.get("location").shouldBeNull()
            } finally {
                MDC.clear()
                previousMdc?.let(MDC::setContextMap)
            }
        }
    })

private fun event(message: String): ILoggingEvent =
    LoggingEvent(
        "test.Logger",
        LoggerFactory.getLogger("test.Logger") as Logger,
        Level.INFO,
        message,
        null,
        null,
    ).apply {
        threadName = "main"
        prepareForDeferredProcessing()
    }

private fun capture(logger: Logger): ListAppender<ILoggingEvent> =
    object : ListAppender<ILoggingEvent>() {
        override fun append(event: ILoggingEvent) {
            event.prepareForDeferredProcessing()
            super.append(event)
        }
    }.also {
        it.start()
        logger.addAppender(it)
    }
