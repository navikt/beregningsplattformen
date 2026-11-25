pluginManagement {
    includeBuild("build-logic")
}

rootProject.name = "beregningsplattformen"

include(
    "apps:beregningsplattform",
    "apps:beregningsplattform:skattekort",
    "apps:trekk",
    "apps:lab:backend",
)

include(
    "libs:http",
    "libs:jdbc",
    "libs:kotlinx",
    "libs:log",
    "libs:mq",
    "libs:oppdrag",
    "libs:beregning",
    "libs:beregningsflyt",
    "libs:tid",
    "libs:task",
)