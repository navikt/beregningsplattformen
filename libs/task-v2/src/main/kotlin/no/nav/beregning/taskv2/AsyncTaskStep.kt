package no.nav.beregning.taskv2

import no.nav.beregning.taskv2.domene.Task

/**
 * Ett task-steg. Portet fra prosessering, men kun doTask (ingen pre/post/onCompletion-livssyklus).
 *
 * En exception fra doTask markerer tasken som feilende. Kast RekjørSenereException for å utsette
 * uten å telle et ordinært feilforsøk.
 */
interface AsyncTaskStep {
    fun doTask(task: Task)
}
