package no.nav.beregning.task.trace

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.SpanBuilder
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import no.nav.beregning.task.TaskRecord

/**
 * 🟢 GRØNN SONE — manuelle OpenTelemetry-spans rundt task-utførelsen. Port fra motors
 * OpentelemetryUtil/JobbInfoSpanBuilder.
 *
 * Bruker GlobalOpenTelemetry, som Nais sin OTel-agent kobler opp i runtime. Uten agent er dette
 * en no-op, så biblioteket virker likt lokalt (ingen SDK-avhengighet nødvendig).
 *
 * Attributtene er kun teknisk metadata (taskId, type, status). Legg aldri payload eller
 * personopplysninger i span-attributter — de eksporteres til traceback-systemet.
 */
object TaskTracer {
    private val tracer: Tracer = GlobalOpenTelemetry.getTracer("beregningsplattformen-task")

    fun <V> span(record: TaskRecord, block: () -> V): V {
        val builder: SpanBuilder =
            tracer.spanBuilder("task ${record.type}")
                .setSpanKind(SpanKind.INTERNAL)
                .setAttribute("taskId", record.id)
                .setAttribute("taskType", record.type)
                .setAttribute("taskStatus", record.status.name)
                .setAttribute("antallForsok", record.antallForsok.toLong())

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
