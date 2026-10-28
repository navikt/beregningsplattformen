package no.nav.beregning.taskv2

/**
 * 🔴 RØD SONE — registrerer task-steg ved å lese @TaskStepBeskrivelse via refleksjon.
 *
 * Uten Spring component scan oppgir konsumenten listen av AsyncTaskStep-instanser selv. Vi leser
 * annotasjonen rett fra klassen (ingen AopProxyUtils, siden det ikke finnes proxy-er her).
 * Validerer at hver type er unik.
 */
class TaskStegRegister(steg: List<AsyncTaskStep>) {

    private val beskrivelser: Map<String, TaskStepBeskrivelse>
    private val stegPerType: Map<String, AsyncTaskStep>

    init {
        val parMedBeskrivelse =
            steg.map { instans ->
                val annotasjon =
                    instans.javaClass.getAnnotation(TaskStepBeskrivelse::class.java)
                        ?: error("@TaskStepBeskrivelse mangler på ${instans.javaClass.name}")
                annotasjon to instans
            }

        val typer = parMedBeskrivelse.map { it.first.taskStepType }
        require(typer.size == typer.toSet().size) {
            "Flere task-steg har samme taskStepType: ${typer.groupingBy { it }.eachCount().filter { it.value > 1 }.keys}"
        }

        beskrivelser = parMedBeskrivelse.associate { it.first.taskStepType to it.first }
        stegPerType = parMedBeskrivelse.associate { it.first.taskStepType to it.second }
    }

    fun finnSteg(type: String): AsyncTaskStep =
        stegPerType[type] ?: error("Ukjent task-type: $type")

    fun finnBeskrivelse(type: String): TaskStepBeskrivelse =
        beskrivelser[type] ?: error("Ukjent task-type: $type")
}
