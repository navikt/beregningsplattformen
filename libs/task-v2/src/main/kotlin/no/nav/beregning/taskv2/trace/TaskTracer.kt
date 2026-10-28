package no.nav.beregning.taskv2.trace

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.SpanBuilder
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import no.nav.beregning.taskv2.domene.Task

/**
 * 🟢 GRØNN SONE — manuelle OpenTelemetry-spans rundt task-utførelsen. Bruker GlobalOpenTelemetry,
 * som Nais OTel-agent kobler opp i runtime; no-op lokalt. Attributtene er teknisk metadata, aldri
 * payload eller personopplysninger.
 */
object TaskTracer {
    private val tracer: Tracer = GlobalOpenTelemetry.getTracer("beregningsplattformen-taskv2")

    fun <V> span(task: Task, block: () -> V): V {
        val builder: SpanBuilder =
            tracer.spanBuilder("task ${task.type}")
                .setSpanKind(SpanKind.INTERNAL)
                .setAttribute("taskId", task.id)
                .setAttribute("taskType", task.type)
                .setAttribute("taskStatus", task.status.name)

        val span = builder.startSpan()
        try {
            span.makeCurrent().use {
                return block()
            }
        } catch (e: Throwable) {
            span.recordException(e)
            span.setStatus(StatusCode.ERROR, e.javaClass.simpleName)
            throw e
        } finally {
            span.end()
        }
    }
}
