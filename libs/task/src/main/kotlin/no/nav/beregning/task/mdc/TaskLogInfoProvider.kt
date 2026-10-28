package no.nav.beregning.task.mdc

import no.nav.beregning.task.TaskInput

/** Ekstra loggkontekst (MDC) en konsument vil legge på mens en task kjøres. Motor: LogInformasjon. */
class LogInformasjon(val felterMedVerdi: Map<String, String>)

/**
 * Lar konsumenten berike logger med egen kontekst (f.eks. en forretningsnøkkel) mens en task kjører.
 * Motor: JobbLogInfoProvider. Returner null for ingen ekstra felter.
 *
 * Merk: legg aldri personopplysninger eller payload i MDC. Verdiene havner i alle logglinjer.
 */
fun interface TaskLogInfoProvider {
    fun hentInformasjon(input: TaskInput): LogInformasjon?
}

/** Standard: ingen ekstra loggkontekst. Motor: NoExtraLogInfoProvider. */
object NoExtraLogInfoProvider : TaskLogInfoProvider {
    override fun hentInformasjon(input: TaskInput): LogInformasjon? = null
}
