package no.nav.beregning.taskv2.domene

import java.time.LocalDateTime

/** Append-only logg over statusoverganger for en task. Portet fra prosessering. */
data class TaskLogg(
    val id: Long = 0L,
    val taskId: Long,
    val type: Loggtype,
    val node: String = hentNodeNavn(),
    val endretAv: String = BRUKERNAVN_NAR_SIKKERHETSKONTEKST_IKKE_FINNES,
    val melding: String? = null,
    val opprettetTid: LocalDateTime = LocalDateTime.now(),
) {
    override fun toString(): String = "TaskLogg(id=$id, type=$type, opprettetTid=$opprettetTid)"

    companion object {
        const val BRUKERNAVN_NAR_SIKKERHETSKONTEKST_IKKE_FINNES = "VL"

        private fun hentNodeNavn(): String = System.getenv("HOSTNAME") ?: "node1"
    }
}
