package no.nav.beregning.taskv2

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.beregning.taskv2.domene.Task
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Et realistisk task-steg slik en konsument ville skrevet det: ingen konstruktør-argumenter,
 * bare @TaskStepBeskrivelse og doTask. Sideeffekten (hva den gjorde) observeres via et delt,
 * trådsikkert register, siden steget kjører på en worker-tråd.
 */
@TaskStepBeskrivelse(taskStepType = "send-melding", beskrivelse = "Sender en melding")
class SendMeldingStep : AsyncTaskStep {
    override fun doTask(task: Task) {
        Kjoringer.registrer(task.type, task.payload)
    }
}

object Kjoringer {
    private val utforte = ConcurrentHashMap<String, String>()
    val latch = CountDownLatch(1)

    fun registrer(type: String, payload: String) {
        utforte[type] = payload
        latch.countDown()
    }

    fun payloadFor(type: String): String? = utforte[type]
}

class PlukkOgKjorStepTest : FunSpec({
    test("en task med en AsyncTaskStep blir plukket opp og kjørt") {
        val ds = TestDatabase.nyDataSource()
        val jdbi = TestDatabase.jdbi(ds)
        val service = TaskService(jdbi)

        val register = TaskStegRegister(listOf(SendMeldingStep()))
        val executor = TaskExecutor(
            taskService = service,
            register = register,
            maintenance = TaskMaintenance(service, jdbi),
            leaderElection = LeaderElection(electorPath = null, hostname = null),
            pollInterval = Duration.ofMillis(50),
            tomKoBackoff = Duration.ofMillis(50),
        )

        try {
            // 1. Lag en task av den registrerte typen
            service.opprett(Task(payload = "ordre-123", type = "send-melding"))

            // 2. Start motoren
            executor.start()

            // 3. Vent til steget faktisk kjørte (deterministisk, ingen sleep)
            Kjoringer.latch.await(10, TimeUnit.SECONDS) shouldBe true
        } finally {
            executor.stop(Duration.ofSeconds(5))
        }

        // 4. Steget kjørte med riktig payload
        Kjoringer.payloadFor("send-melding") shouldBe "ordre-123"

        // 5. Tasken endte FERDIG
        val status = jdbi.withHandle<String, Exception> {
            it.createQuery("SELECT status FROM task WHERE type = 'send-melding'")
                .mapTo(String::class.java).one()
        }
        status shouldBe "FERDIG"
    }
})
