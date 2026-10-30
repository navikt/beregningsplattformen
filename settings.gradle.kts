pluginManagement {
    repositories {
        maven {
            name = "GitHubPackagesKtlint"
            url = uri("https://maven.pkg.github.com/navikt/ktlint-gradle")
            credentials {
                username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR") ?: "token"
                password = providers.gradleProperty("gpr.token").orNull ?: System.getenv("GITHUB_TOKEN")
            }
        }
        gradlePluginPortal()
    }
}

rootProject.name = "beregningsplattformen"

include(
    "apps:mottak",
    "apps:lab:backend",
)

include(
    "libs:http",
    "libs:kotlinx",
    "libs:log",
    "libs:mq",
    "libs:oppdrag",
    "libs:beregning",
    "libs:beregningsflyt",
    "libs:beregningsflyt-verifikasjon",
    "libs:tid",
)