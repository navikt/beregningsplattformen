package no.nav.utbetaling.beregning.beregningsplattform.config

import kotlinx.serialization.Serializable

import com.typesafe.config.ConfigFactory
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.config.getAs
import io.ktor.server.config.withFallback

import no.nav.utbetaling.beregning.jdbc.JdbcConfig

private const val APPLICATION_CONF = "application"
private const val AZUREAD_CONF = "azureAd"
private const val MQ_CONF = "mq"
private const val POSTGRES_CONF = "postgres"

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

    val postgresProperties by lazy {
        config.property(POSTGRES_CONF).getAs<PostgresProperties>()
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
        val oppdragXmlQueue: String,
        val username: String,
        val password: String,
    )

    @Serializable
    data class PostgresProperties(
        val jdbcUrl: String? = null,
        val name: String,
        val host: String,
        val port: Int,
        val username: String,
        val password: String,
    ) {
        internal fun toJdbcConfig(): JdbcConfig =
            JdbcConfig(
                jdbcUrl = jdbcUrl ?: "jdbc:postgresql://$host:$port/$name",
                username = username,
                password = password,
            )
    }
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
