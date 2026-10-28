package no.nav.beregning.task

/**
 * Register over task-typer konsumenten har aktivert. Motor: `JobbType`.
 */
class TaskRegistry(typer: List<TaskType>) {
    private val byType: Map<String, TaskType> = typer.associateBy { it.type }

    fun parse(type: String): TaskType =
        byType[type] ?: error("Ukjent task-type: $type. Registrer den i TaskRegistry.")

    fun cronTyper(): List<TaskType> = byType.values.filter { it.cron != null }

    fun alle(): Collection<TaskType> = byType.values
}
