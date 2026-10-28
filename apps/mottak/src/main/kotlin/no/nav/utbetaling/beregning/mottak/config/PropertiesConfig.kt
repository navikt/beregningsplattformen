package no.nav.utbetaling.beregning.mottak.config

import kotlinx.serialization.Serializable

import com.typesafe.config.ConfigFactory
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.config.getAs
import io.ktor.server.config.withFallback

private const val APPLICATION_CONF = "application"
private const val AZUREAD_CONF = "azureAd"
private const val MQ_CONF = "mq"

object PropertiesConfig {
    lateinit var config: ApplicationConfig
        private set

    fun load(applicationConfig: ApplicationConfig) {
        if (!::config.isInitialized) {
            config = applicationConfig
        }
    }

    val applicationProperties by lazy {
        config.property(APPLICATION_CONF).getAs<ApplicationProperties>()
    }

    val azureAdProperties by lazy {
        config.property(AZUREAD_CONF).getAs<AzureAdProperties>()
    }

    val mqProperties by lazy {
        config.property(MQ_CONF).getAs<MQProperties>()
    }

    @Serializable
    data class ApplicationProperties(
        val profile: Profile,
        val appName: String,
        val namespace: String,
    ) {
        val isLocal = profile == Profile.LOCAL
        val isTest = profile == Profile.TEST
    }

    @Serializable
    data class AzureAdProperties(
        val clientId: String,
        val wellKnownUrl: String,
    )

    @Serializable
    data class MQProperties(
        val hostname: String,
        val port: Int,
        val manager: String,
        val channel: String,
        val queue: String,
        val username: String,
        val password: String,
    )
}

fun ApplicationConfig.loadEnv(): ApplicationConfig {
    val hoconConfig = HoconApplicationConfig(ConfigFactory.load())
    val environmentName = System.getenv("NAIS_CLUSTER_NAME") ?: System.getProperty("NAIS_CLUSTER_NAME")
    val environment = environmentName?.lowercase()?.substringBefore("-") ?: "local"
    val environmentConfig = ApplicationConfig("application-$environment.conf")
    return environmentConfig overriding this overriding hoconConfig
}

infix fun ApplicationConfig.overriding(other: ApplicationConfig): ApplicationConfig = this.withFallback(other)

enum class Profile {
    LOCAL,
    TEST,
    DEV,
    PROD,
}
