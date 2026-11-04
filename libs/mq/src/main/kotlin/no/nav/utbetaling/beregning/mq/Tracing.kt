package no.nav.utbetaling.beregning.mq

import java.util.Collections

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanBuilder
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.TextMapGetter
import io.opentelemetry.context.propagation.TextMapPropagator
import io.opentelemetry.context.propagation.TextMapSetter
import jakarta.jms.Message

import no.nav.utbetaling.beregning.log.logger

val traceLog = logger("trace")

/**
 * Leser og skriver W3C trace context (traceparent/tracestate) som JMS string properties.
 * Samme format som OpenTelemetry javaagent sin JMS-instrumentering bruker (helved setter `traceparent` slik).
 */
internal object JmsMessageGetter : TextMapGetter<Message> {
    override fun keys(carrier: Message): Iterable<String> = carrier.propertyNames.toList().map { it.toString() }

    override fun get(
        carrier: Message?,
        key: String,
    ): String? = carrier?.runCatching { getStringProperty(key) }?.getOrNull()
}

internal object JmsMessageSetter : TextMapSetter<Message> {
    override fun set(
        carrier: Message?,
        key: String,
        value: String,
    ) {
        carrier?.setStringProperty(key, value)
    }
}

object Tracing {
    val tracer: Tracer = GlobalOpenTelemetry.getTracer("mq-tracer")

    private fun propagator(): TextMapPropagator = GlobalOpenTelemetry.getPropagators().textMapPropagator

    /**
     * Henter trace context fra en mottatt MQ-melding. Mangler `traceparent` returneres [parent] uendret.
     */
    fun extract(
        message: Message,
        parent: Context = Context.current(),
        propagator: TextMapPropagator = propagator(),
    ): Context = propagator.extract(parent, message, JmsMessageGetter)

    /**
     * Skriver trace context fra [context] inn på en MQ-melding før sending.
     */
    fun inject(
        message: Message,
        context: Context = Context.current(),
        propagator: TextMapPropagator = propagator(),
    ) {
        propagator.inject(context, message, JmsMessageSetter)
    }

    fun getTraceparent(): String? {
        val ctx = Span.current().spanContext
        if (!ctx.isValid) return null
        val sampled = if (ctx.traceFlags.isSampled) "01" else "00"
        return "00-${ctx.traceId}-${ctx.spanId}-$sampled"
    }

    fun getTraceparent(ctx: SpanContext): String? {
        if (!ctx.isValid) return null
        val sampled = if (ctx.traceFlags.isSampled) "01" else "00"
        return "00-${ctx.traceId}-${ctx.spanId}-$sampled"
    }

    /**
     * Start a new span and append it to the context
     */
    fun startSpan(
        name: String,
        spanBuilder: (SpanBuilder) -> SpanBuilder,
        block: (Span) -> Unit,
    ) {
        val span = spanBuilder(tracer.spanBuilder(name)).startSpan()
        try {
            span.makeCurrent().use {
                block(span)
            }
        } catch (e: Exception) {
            span.setStatus(StatusCode.ERROR, "Error: ${e.message}")
            throw e
        } finally {
            span.end()
        }
    }

    /**
     * When passing spans across execution boundaries, use context propagation
     */
    fun propagateSpan(): Context {
        val span = Span.current()
        return Context.current().with(span)
    }

    fun getCurrentTraceId(): String? {
        val traceId = Span.current().spanContext.traceId
        return traceId
    }

    private val traceContextGetter =
        object : TextMapGetter<String> {
            override fun keys(carrier: String): Iterable<String> = Collections.singletonList("traceparent")

            override fun get(
                carrier: String?,
                key: String,
            ): String? = if ("traceparent" == key) carrier else null
        }

    /**
     * Gjenoppretter trace context fra en lagret traceparent, f.eks. når en avvist melding sendes tilbake senere.
     */
    fun contextFromTraceparent(traceparent: String): Context = propagator().extract(Context.current(), traceparent, traceContextGetter)
}
