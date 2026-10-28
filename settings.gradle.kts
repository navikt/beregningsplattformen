rootProject.name = "beregningsplattformen"

include(
    "apps:lab:backend",
)

include(
    "libs:http",
    "libs:kotlinx",
    "libs:log",
    "libs:oppdrag",
    "libs:beregning",
    "libs:tid",
    "libs:task",
    "libs:task-v2",
)
