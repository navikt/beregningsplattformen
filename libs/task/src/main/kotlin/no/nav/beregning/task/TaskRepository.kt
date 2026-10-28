package no.nav.beregning.task

import org.jdbi.v3.core.Handle
import java.time.LocalDateTime

/**
 * 🔴 RØD SONE — samtidighetskritisk kjerne. Les sjekklisten i leveransen.
 *
 * Persistering av tasks med JDBI, etter motors `JobbRepository`-mønster: rå SQL med bundne
 * parametere, eksplisitt row-mapping. Alle metoder opererer på en [Handle] levert av kalleren,
 * slik at enqueue kan skje i konsumentens egen transaksjon (utbox-garantien).
 *
 * Invariant: hver statusendring skriver BÅDE JOBB.status OG en rad i JOBB_HISTORIKK, i samme
 * transaksjon. Kalleren eier transaksjonsgrensen; denne klassen åpner aldri sin egen.
 */
class TaskRepository(private val handle: Handle) {

    /**
     * Legg en task i kø. Kjøres typisk i konsumentens transaksjon sammen med forretningsdata,
     * så en rollback der også ruller tilbake tasken (utbox).
     */
    fun leggTil(input: TaskInput): Long {
        val id =
            handle.createUpdate(
                """
                INSERT INTO JOBB (type, status, neste_kjoring, parameters, payload, prioritet)
                VALUES (:type, 'KLAR', :nesteKjoring, :parameters, :payload, :prioritet)
                """.trimIndent(),
            )
                .bind("type", input.type)
                .bind("nesteKjoring", input.nesteKjoring)
                .bind("parameters", input.parameters)
                .bind("payload", input.payload)
                .bind("prioritet", input.prioritet)
                .executeAndReturnGeneratedKeys("id")
                .mapTo(Long::class.java)
                .one()

        skrivHistorikk(id, TaskStatus.KLAR, null)
        return id
    }

    /**
     * Plukk inntil [antall] modne tasks og marker dem PLUKKET, atomisk. FOR UPDATE SKIP LOCKED gjør
     * at samtidige plukkere (på tvers av podder) hopper over rader andre holder, så ingen task
     * plukkes to ganger i samme øyeblikk.
     *
     * I motsetning til én-task-plukkingen er dette ment å kjøre i en KORT transaksjon som committer
     * med én gang: plukk → PLUKKET → commit. Selve utførelsen skjer etterpå, i egne transaksjoner.
     * Fordi låsen slippes ved commit, kan ikke Postgres lenger frigjøre en task automatisk hvis en
     * pod krasjer midt i utførelsen — [resettHengende] (reaper) tar det ansvaret i stedet. Derfor
     * settes plukket_tid her, så reaperen kan måle alder.
     *
     * Statusene i IN (...) er enum-konstanter fra egen kode, aldri brukerinput — trygt å
     * interpolere. Ikke bytt dette mot en variabel.
     */
    fun plukkBatch(antall: Int): List<TaskRecord> {
        val records =
            handle.createQuery(
                """
                SELECT id, type, status, payload, prioritet, antall_forsok, neste_kjoring, parameters
                FROM JOBB
                WHERE status IN ('KLAR', 'FEILET') AND neste_kjoring <= now()
                ORDER BY prioritet, neste_kjoring, id
                FOR UPDATE SKIP LOCKED
                LIMIT :antall
                """.trimIndent(),
            )
                .bind("antall", antall)
                .map { rs, _ ->
                    TaskRecord(
                        id = rs.getLong("id"),
                        type = rs.getString("type"),
                        status = TaskStatus.valueOf(rs.getString("status")),
                        payload = rs.getString("payload"),
                        prioritet = rs.getInt("prioritet"),
                        antallForsok = rs.getInt("antall_forsok"),
                        nesteKjoring = rs.getTimestamp("neste_kjoring").toLocalDateTime(),
                        parameters = rs.getString("parameters"),
                    )
                }
                .list()

        if (records.isEmpty()) return emptyList()

        // Marker alle PLUKKET + sett plukket_tid, i samme transaksjon som utvelgelsen.
        val ids = records.map { it.id }
        handle.createUpdate(
            "UPDATE JOBB SET status = 'PLUKKET', plukket_tid = now(), sist_endret = now() WHERE id IN (<ids>)",
        )
            .bindList("ids", ids)
            .execute()
        records.forEach { skrivHistorikk(it.id, TaskStatus.PLUKKET, null) }

        return records.map { it.copy(status = TaskStatus.PLUKKET) }
    }

    /**
     * Reaper: tasks som har stått PLUKKET lenger enn [timeout] antas å tilhøre en krasjet pod
     * (låsen ble sluppet ved commit, så ingen kjører dem lenger). Sett dem tilbake til KLAR så de
     * plukkes på nytt. Returnerer antall resatte tasks.
     *
     * Timeouten MÅ være romslig lengre enn den tregeste task-utførelsen, ellers resetter reaperen
     * tasks som faktisk kjører → dobbeltkjøring.
     */
    fun resettHengende(timeout: java.time.Duration): Int {
        val grense = LocalDateTime.now().minus(timeout)
        val resatte =
            handle.createQuery(
                """
                SELECT id FROM JOBB
                WHERE status = 'PLUKKET' AND plukket_tid < :grense
                FOR UPDATE SKIP LOCKED
                """.trimIndent(),
            )
                .bind("grense", grense)
                .mapTo(Long::class.java)
                .list()

        if (resatte.isEmpty()) return 0

        handle.createUpdate(
            "UPDATE JOBB SET status = 'KLAR', plukket_tid = NULL, sist_endret = now() WHERE id IN (<ids>)",
        )
            .bindList("ids", resatte)
            .execute()
        resatte.forEach { skrivHistorikk(it, TaskStatus.KLAR, "Reaper: resatt fra hengende PLUKKET") }
        return resatte.size
    }

    /** Marker en vellykket task FERDIG. */
    fun markerFerdig(id: Long) = settStatus(id, TaskStatus.FERDIG, null)

    /**
     * Reskedulér en cron-task: marker den FERDIG (slik at kjøringen arkiveres), og legg inn en ny
     * KLAR task av samme type med neste_kjoring satt til cron-mønsterets neste tidspunkt. Dette
     * speiler motor, der en cron-jobb planlegger neste kjøring av seg selv ved fullføring.
     */
    fun reskedulerCron(record: TaskRecord, nesteKjoring: LocalDateTime): Long {
        settStatus(record.id, TaskStatus.FERDIG, null)
        return leggTil(record.toInput().copy(nesteKjoring = nesteKjoring))
    }

    /**
     * Registrer et mislykket forsøk. Teller opp antall_forsok. Hvis vi har nådd maks, settes
     * tasken FEILET permanent; ellers settes den tilbake til FEILET med neste_kjoring skjøvet
     * frem (backoff), slik at plukkespørringen tar den igjen når tiden er moden.
     */
    fun markerFeilet(id: Long, maxRetries: Int, backoff: java.time.Duration, feilmelding: String?) {
        val antallForsok =
            handle.createQuery("SELECT antall_forsok FROM JOBB WHERE id = :id")
                .bind("id", id)
                .mapTo(Int::class.java)
                .one() + 1

        val nesteKjoring =
            if (antallForsok >= maxRetries) {
                // Permanent feilet: skyv langt frem så plukkespørringen ikke tar den igjen.
                LocalDateTime.now().plusYears(100)
            } else {
                LocalDateTime.now().plus(backoff)
            }

        handle.createUpdate(
            """
            UPDATE JOBB
            SET status = 'FEILET', antall_forsok = :antallForsok, neste_kjoring = :nesteKjoring, sist_endret = now()
            WHERE id = :id
            """.trimIndent(),
        )
            .bind("antallForsok", antallForsok)
            .bind("nesteKjoring", nesteKjoring)
            .bind("id", id)
            .execute()

        skrivHistorikk(id, TaskStatus.FEILET, feilmelding)
    }

    private fun settStatus(id: Long, status: TaskStatus, feilmelding: String?) {
        handle.createUpdate(
            "UPDATE JOBB SET status = :status, sist_endret = now() WHERE id = :id",
        )
            .bind("status", status.name)
            .bind("id", id)
            .execute()
        skrivHistorikk(id, status, feilmelding)
    }

    private fun skrivHistorikk(jobbId: Long, status: TaskStatus, feilmelding: String?) {
        handle.createUpdate(
            """
            INSERT INTO JOBB_HISTORIKK (jobb_id, status, feilmelding)
            VALUES (:jobbId, :status, :feilmelding)
            """.trimIndent(),
        )
            .bind("jobbId", jobbId)
            .bind("status", status.name)
            .bind("feilmelding", feilmelding)
            .execute()
    }
}
