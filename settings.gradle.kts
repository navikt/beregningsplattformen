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
    "libs:beregningsflyt-verifikasjon",
    "libs:tid",
)