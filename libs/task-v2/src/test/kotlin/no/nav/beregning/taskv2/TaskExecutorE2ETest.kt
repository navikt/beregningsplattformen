package no.nav.beregning.taskv2

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.beregning.taskv2.domene.Task
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@TaskStepBeskrivelse(taskStepType = "e2e-task", beskrivelse = "E2E-test")
class E2ETaskStep(private val latch: CountDownLatch, private val fanget: AtomicReference<String>) : AsyncTaskStep {
    override fun doTask(task: Task) {
        fanget.set(task.payload)
        latch.countDown()
        System.out.println("E2ETaskStep kjørte med payload: ${task.payload}")
    }
}

class TaskExecutorE2ETest : FunSpec({
    test("en annotert AsyncTaskStep blir registrert, plukket og kjørt") {
        val ds = TestDatabase.nyDataSource()
        val jdbi = TestDatabase.jdbi(ds)
        val service = TaskService(jdbi)

        val latch = CountDownLatch(1)
        val fanget = AtomicReference<String>()
        val register = TaskStegRegister(listOf(E2ETaskStep(latch, fanget)))
        val maintenance = TaskMaintenance(service, jdbi)
        val aldriLeder = LeaderElection(electorPath = null, hostname = null) // vedlikehold hopper over

        val executor = TaskExecutor(
            taskService = service,
            register = register,
            maintenance = maintenance,
            leaderElection = aldriLeder,
            pollInterval = Duration.ofMillis(50),
            tomKoBackoff = Duration.ofMillis(50),
        )

        try {
            service.opprett(Task(payload = "hei-v2", type = "e2e-task"))
            executor.start()

            latch.await(10, TimeUnit.SECONDS) shouldBe true
            fanget.get() shouldBe "hei-v2"
        } finally {
            executor.stop(Duration.ofSeconds(5))
        }

        val status = jdbi.withHandle<String, Exception> {
            it.createQuery("SELECT status FROM task WHERE type = 'e2e-task'").mapTo(String::class.java).one()
        }
        status shouldBe "FERDIG"
    }
})
