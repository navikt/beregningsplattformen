package no.nav.beregning.task

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.jdbi.v3.core.Jdbi
import java.time.Duration
import java.time.LocalDateTime

/**
 * 🔴 RØD SONE-tester — verifiserer oppførselen til plukking, statusmaskin og utbox-garantien.
 * Vi tester IKKE at SKIP LOCKED virker (det stoler vi på), men at vår bruk av den gir rett tilstand.
 */
class TaskRepositoryTest : FunSpec({
    lateinit var jdbi: Jdbi

    beforeTest {
        jdbi = TestDatabase.jdbi(TestDatabase.nyDataSource())
    }

    fun antallHistorikk(id: Long): Int =
        jdbi.withHandle<Int, Exception> {
            it.createQuery("SELECT count(*) FROM JOBB_HISTORIKK WHERE jobb_id = :id")
                .bind("id", id).mapTo(Int::class.java).one()
        }

    fun status(id: Long): String =
        jdbi.withHandle<String, Exception> {
            it.createQuery("SELECT status FROM JOBB WHERE id = :id")
                .bind("id", id).mapTo(String::class.java).one()
        }

    test("leggTil skriver KLAR i både JOBB og JOBB_HISTORIKK") {
        val id = jdbi.withHandle<Long, Exception> {
            TaskRepository(it).leggTil(TaskInput(type = "test", payload = "<xml/>"))
        }
        status(id) shouldBe "KLAR"
        antallHistorikk(id) shouldBe 1
    }

    test("utbox: rollback i kallers transaksjon ruller også tilbake tasken") {
        runCatching {
            jdbi.useTransaction<Exception> {
                TaskRepository(it).leggTil(TaskInput(type = "test", payload = "x"))
                throw RuntimeException("konsument feilet")
            }
        }
        val antall = jdbi.withHandle<Int, Exception> {
            it.createQuery("SELECT count(*) FROM JOBB").mapTo(Int::class.java).one()
        }
        antall shouldBe 0
    }

    test("plukkBatch markerer PLUKKET, setter plukket_tid og skriver historikk") {
        val id = jdbi.withHandle<Long, Exception> {
            TaskRepository(it).leggTil(TaskInput(type = "test", payload = "x"))
        }
        val plukket = jdbi.inTransaction<List<TaskRecord>, Exception> { TaskRepository(it).plukkBatch(20) }
        plukket.map { it.id } shouldBe listOf(id)
        status(id) shouldBe "PLUKKET"
        antallHistorikk(id) shouldBe 2
    }

    test("plukkBatch respekterer grensen og prioritet") {
        jdbi.useTransaction<Exception> {
            val repo = TaskRepository(it)
            repo.leggTil(TaskInput(type = "a", payload = "x", prioritet = Prioritet.LAV))
            repo.leggTil(TaskInput(type = "b", payload = "x", prioritet = Prioritet.KRITISK))
            repo.leggTil(TaskInput(type = "c", payload = "x", prioritet = Prioritet.NORMAL))
        }
        val plukket = jdbi.inTransaction<List<TaskRecord>, Exception> { TaskRepository(it).plukkBatch(2) }
        // KRITISK (10) før NORMAL (30), LAV (40) faller utenfor grensen på 2
        plukket.map { it.type } shouldBe listOf("b", "c")
    }

    test("plukkBatch hopper over tasks med neste_kjoring i fremtiden") {
        jdbi.withHandle<Long, Exception> {
            TaskRepository(it).leggTil(
                TaskInput(type = "test", payload = "x", nesteKjoring = LocalDateTime.now().plusHours(1)),
            )
        }
        jdbi.inTransaction<List<TaskRecord>, Exception> { TaskRepository(it).plukkBatch(20) }.shouldBeEmpty()
    }

    test("reaper resetter PLUKKET-tasks eldre enn timeout tilbake til KLAR") {
        val id = jdbi.withHandle<Long, Exception> {
            TaskRepository(it).leggTil(TaskInput(type = "test", payload = "x"))
        }
        jdbi.inTransaction<List<TaskRecord>, Exception> { TaskRepository(it).plukkBatch(20) }
        status(id) shouldBe "PLUKKET"
        // Flytt plukket_tid langt bakover så tasken ser hengende ut
        jdbi.useHandle<Exception> {
            it.createUpdate("UPDATE JOBB SET plukket_tid = now() - interval '1 hour' WHERE id = :id")
                .bind("id", id).execute()
        }
        val antall = jdbi.inTransaction<Int, Exception> { TaskRepository(it).resettHengende(Duration.ofMinutes(30)) }
        antall shouldBe 1
        status(id) shouldBe "KLAR"
    }

    test("reaper rører ikke ferske PLUKKET-tasks") {
        jdbi.withHandle<Long, Exception> {
            TaskRepository(it).leggTil(TaskInput(type = "test", payload = "x"))
        }
        jdbi.inTransaction<List<TaskRecord>, Exception> { TaskRepository(it).plukkBatch(20) }
        val antall = jdbi.inTransaction<Int, Exception> { TaskRepository(it).resettHengende(Duration.ofMinutes(30)) }
        antall shouldBe 0
    }

    test("markerFeilet øker antall_forsok, setter FEILET og skyver neste_kjoring frem") {
        val id = jdbi.withHandle<Long, Exception> {
            TaskRepository(it).leggTil(TaskInput(type = "test", payload = "x"))
        }
        jdbi.useTransaction<Exception> {
            TaskRepository(it).markerFeilet(id, maxRetries = 3, backoff = Duration.ofMinutes(5), feilmelding = "boom")
        }
        status(id) shouldBe "FEILET"
        val forsok = jdbi.withHandle<Int, Exception> {
            it.createQuery("SELECT antall_forsok FROM JOBB WHERE id = :id").bind("id", id).mapTo(Int::class.java).one()
        }
        forsok shouldBe 1
        // Feilet task er ikke moden nå (backoff), plukkes ikke umiddelbart
        jdbi.inTransaction<List<TaskRecord>, Exception> { TaskRepository(it).plukkBatch(20) }.shouldBeEmpty()
    }

    test("etter maks forsøk skyves neste_kjoring langt frem (permanent feilet)") {
        val id = jdbi.withHandle<Long, Exception> {
            TaskRepository(it).leggTil(TaskInput(type = "test", payload = "x"))
        }
        repeat(3) {
            jdbi.useTransaction<Exception> {
                TaskRepository(it).markerFeilet(id, maxRetries = 3, backoff = Duration.ZERO, feilmelding = "boom")
            }
        }
        // Selv med null backoff skal den ikke plukkes etter maks forsøk
        jdbi.inTransaction<List<TaskRecord>, Exception> { TaskRepository(it).plukkBatch(20) }.shouldBeEmpty()
    }
})
