package no.nav.beregning.taskv2

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.beregning.taskv2.domene.Status
import no.nav.beregning.taskv2.domene.Task
import org.jdbi.v3.core.Jdbi

class TaskServiceTest : FunSpec({
    lateinit var jdbi: Jdbi
    lateinit var service: TaskService

    beforeTest {
        jdbi = TestDatabase.jdbi(TestDatabase.nyDataSource())
        service = TaskService(jdbi)
    }

    fun status(id: Long) =
        jdbi.withHandle<String, Exception> {
            it.createQuery("SELECT status FROM task WHERE id = :id").bind("id", id).mapTo(String::class.java).one()
        }

    fun antallLogg(id: Long) =
        jdbi.withHandle<Int, Exception> {
            it.createQuery("SELECT count(*) FROM task_logg WHERE task_id = :id").bind("id", id).mapTo(Int::class.java).one()
        }

    test("opprett lagrer UBEHANDLET og skriver task_logg") {
        val task = service.opprett(Task(payload = "x", type = "test"))
        status(task.id) shouldBe "UBEHANDLET"
        antallLogg(task.id) shouldBe 1
        task.versjon shouldBe 0
    }

    test("plukk øker versjon og logger PLUKKET") {
        val task = service.opprett(Task(payload = "x", type = "test"))
        val plukket = service.plukk(task)
        plukket.status shouldBe Status.PLUKKET
        plukket.versjon shouldBe 1
        status(task.id) shouldBe "PLUKKET"
        antallLogg(task.id) shouldBe 2
    }

    test("optimistisk lås: andre plukk med utdatert versjon kaster") {
        val task = service.opprett(Task(payload = "x", type = "test"))
        service.plukk(task) // versjon 0 -> 1
        // task-objektet har fortsatt versjon 0; nytt forsøk skal tape kappløpet
        shouldThrow<OptimistiskLasException> { service.plukk(task) }
    }

    test("full lykkelig flyt UBEHANDLET -> PLUKKET -> BEHANDLER -> FERDIG") {
        var task = service.opprett(Task(payload = "x", type = "test"))
        task = service.plukk(task)
        task = service.behandle(task)
        task = service.ferdigstill(task)
        task.status shouldBe Status.FERDIG
        status(task.id) shouldBe "FERDIG"
        antallLogg(task.id) shouldBe 4 // UBEHANDLET, PLUKKET, BEHANDLER, FERDIG
    }

    test("feilet under maks gir KLAR_TIL_PLUKK, over maks gir FEILET") {
        var task = service.opprett(Task(payload = "x", type = "test"))
        task = service.plukk(task)
        val etterFeil = service.feilet(task, "boom", tidligereAntallFeil = 0, maxAntallFeil = 3, settTilManuellOppfolgning = false, nyTriggerTid = task.triggerTid)
        etterFeil.status shouldBe Status.KLAR_TIL_PLUKK

        var task2 = service.opprett(Task(payload = "y", type = "test"))
        task2 = service.plukk(task2)
        val etterMaks = service.feilet(task2, "boom", tidligereAntallFeil = 3, maxAntallFeil = 3, settTilManuellOppfolgning = false, nyTriggerTid = task2.triggerTid)
        etterMaks.status shouldBe Status.FEILET
    }

    test("feilet over maks med manuell oppfølging gir MANUELL_OPPFOLGING") {
        var task = service.opprett(Task(payload = "x", type = "test"))
        task = service.plukk(task)
        val resultat = service.feilet(task, "boom", tidligereAntallFeil = 3, maxAntallFeil = 3, settTilManuellOppfolgning = true, nyTriggerTid = task.triggerTid)
        resultat.status shouldBe Status.MANUELL_OPPFOLGING
    }
})
