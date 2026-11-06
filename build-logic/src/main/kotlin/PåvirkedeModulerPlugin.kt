import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency

/**
 *  Ved å kjøre `./gradlew -q paavirkedeModuler | tail -n1`
 *  Får man en JSON-struktur som viser hvilke moduler som er påvirket, grupper på hva slags typer det er.
 *  `--siden=<ref>` velger hva endringene regnes fra (standard origin/main),
 *  `--alle-apper` tar med alle apper selv om de ikke er påvirket av endringen.
 *
 *  `./gradlew -q appDeployConfig --app=<navn> --miljo=<miljø> | tail -n1` gir JSON for én app med ett miljø.
 */
class PåvirkedeModulerPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val påvirkedeModuler =
            target.tasks.register("paavirkedeModuler", PåvirkedeModulerTask::class.java)
        val appDeployConfig =
            target.tasks.register("appDeployConfig", AppDeployConfigTask::class.java)

        // Må vente til alle prosjekter er evaluert, siden plugins og avhengigheter ellers ikke er satt.
        target.gradle.projectsEvaluated {
            val moduler = target.subprojects.filter { it.buildFile.exists() }
            val kataloger = moduler.associate { it.path to it.projectDir.relativeTo(target.projectDir).path }
            val avhengigheter =
                moduler.associate { modul ->
                    modul.path to
                        modul.configurations
                            .flatMap { it.allDependencies.withType(ProjectDependency::class.java) }
                            .map { it.path }
                            .toSet()
                }
            val publiserbare = moduler.filter { it.pluginManager.hasPlugin("publiser-bibliotek") }.map { it.path }

            påvirkedeModuler.configure {
                prosjektRot.set(target.layout.projectDirectory)
                modulKataloger.set(kataloger)
                direkteAvhengigheter.set(avhengigheter)
                publiserteBiblioteker.set(publiserbare)
            }
            appDeployConfig.configure {
                prosjektRot.set(target.layout.projectDirectory)
                modulKataloger.set(kataloger)
            }
        }
    }
}
