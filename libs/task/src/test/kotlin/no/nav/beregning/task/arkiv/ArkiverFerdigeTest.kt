package no.nav.beregning.task.arkiv

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.beregning.task.TaskInput
import no.nav.beregning.task.TaskRepository
import no.nav.beregning.task.TestDatabase
import java.time.Duration

class ArkiverFerdigeTest : FunSpec({
    test("arkiverer kun gamle FERDIG-tasks og flytter historikken") {
        val jdbi = TestDatabase.jdbi(TestDatabase.nyDataSource())
        val id = jdbi.withHandle<Long, Exception> {
            TaskRepository(it).leggTil(TaskInput(type = "test", payload = "x"))
        }
        jdbi.useTransaction<Exception> { TaskRepository(it).markerFerdig(id) }
        // Gjør tasken gammel nok til å arkiveres
        jdbi.useHandle<Exception> {
            it.createUpdate("UPDATE JOBB SET sist_endret = now() - interval '10 days' WHERE id = :id")
                .bind("id", id).execute()
        }

        val antall = ArkiverFerdige(jdbi, eldreEnn = Duration.ofDays(7)).arkiver()
        antall shouldBe 1

        val iJobb = jdbi.withHandle<Int, Exception> {
            it.createQuery("SELECT count(*) FROM JOBB WHERE id = :id").bind("id", id).mapTo(Int::class.java).one()
        }
        val iArkiv = jdbi.withHandle<Int, Exception> {
            it.createQuery("SELECT count(*) FROM JOBB_ARKIV WHERE id = :id").bind("id", id).mapTo(Int::class.java).one()
        }
        val historikkArkiv = jdbi.withHandle<Int, Exception> {
            it.createQuery("SELECT count(*) FROM JOBB_HISTORIKK_ARKIV WHERE jobb_id = :id")
                .bind("id", id).mapTo(Int::class.java).one()
        }
        iJobb shouldBe 0
        iArkiv shouldBe 1
        historikkArkiv shouldBe 2 // KLAR + FERDIG
    }

    test("rører ikke ferske FERDIG-tasks") {
        val jdbi = TestDatabase.jdbi(TestDatabase.nyDataSource())
        val id = jdbi.withHandle<Long, Exception> {
            TaskRepository(it).leggTil(TaskInput(type = "test", payload = "x"))
        }
        jdbi.useTransaction<Exception> { TaskRepository(it).markerFerdig(id) }
        ArkiverFerdige(jdbi, eldreEnn = Duration.ofDays(7)).arkiver() shouldBe 0
    }
})
