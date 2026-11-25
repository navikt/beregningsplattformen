package no.nav.beregning.task.trace

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.beregning.task.TaskRecord
import no.nav.beregning.task.TaskStatus
import java.time.LocalDateTime

class TaskTracerTest : FunSpec({
    val record = TaskRecord(
        id = 1,
        type = "test",
        status = TaskStatus.PLUKKET,
        payload = "x",
        prioritet = 30,
        antallForsok = 0,
        nesteKjoring = LocalDateTime.now(),
        parameters = null,
    )

    test("span kjører blokken og returnerer verdien, også uten OTel-agent (no-op)") {
        val resultat = TaskTracer.span(record) { 123 }
        resultat shouldBe 123
    }

    test("span propagerer exceptions") {
        try {
            TaskTracer.span(record) { throw IllegalStateException("boom") }
            throw AssertionError("skulle kastet")
        } catch (e: IllegalStateException) {
            e.message shouldBe "boom"
        }
    }
})
