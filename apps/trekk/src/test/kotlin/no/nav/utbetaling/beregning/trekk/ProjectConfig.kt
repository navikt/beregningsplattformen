package no.nav.utbetaling.beregning.trekk

import io.kotest.core.config.AbstractProjectConfig
import io.ktor.server.config.ApplicationConfig

import no.nav.utbetaling.beregning.trekk.config.PropertiesConfig

class ProjectConfig : AbstractProjectConfig() {
    override suspend fun beforeProject() {
        PropertiesConfig.load(ApplicationConfig("application-test.conf"))
    }
}
