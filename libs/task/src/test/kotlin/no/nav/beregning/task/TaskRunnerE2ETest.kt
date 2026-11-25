package no.nav.beregning.task

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Ende-til-ende: en ekte TaskType registreres, enqueues, og vi verifiserer at TaskRunner plukker
 * den opp og kjører utfor(). Latch gjør ventingen deterministisk, ingen vilkårlig sleep.
 */
class TaskRunnerE2ETest : FunSpec({
    test("en registrert TaskType blir plukket opp og kjørt") {
        val ds = TestDatabase.nyDataSource()
        val jdbi = TestDatabase.jdbi(ds)

        val latch = CountDownLatch(1)
        val mottattPayload = AtomicReference<String>()

        val minTaskType = object : TaskType {
            override val type = "min-test-task"
            override val navn = "Min testtask"
            override val beskrivelse = "Setter payload og teller ned latchen"
            override fun konstruer(): Task =
                Task { input ->
                    mottattPayload.set(input.payload)
                    latch.countDown()
                }
        }

        val runner = TaskRunner(
            dataSource = ds,
            registry = TaskRegistry(listOf(minTaskType)),
            antallWorkere = 2,
            pollInterval = Duration.ofMillis(50),
            tomKoBackoff = Duration.ofMillis(50),
        )

        try {
            jdbi.useTransaction<Exception> {
                TaskRepository(it).leggTil(TaskInput(type = "min-test-task", payload = "hei"))
            }
            runner.start()

            latch.await(10, TimeUnit.SECONDS) shouldBe true
            mottattPayload.get() shouldBe "hei"
        } finally {
            runner.stop(Duration.ofSeconds(5))
        }

        // Tasken skal være markert FERDIG etter vellykket kjøring
        val status = jdbi.withHandle<String, Exception> {
            it.createQuery("SELECT status FROM JOBB WHERE type = 'min-test-task'")
                .mapTo(String::class.java).one()
        }
        status shouldBe "FERDIG"
    }
})
