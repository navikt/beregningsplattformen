import java.net.URI
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository

class PubliserBibliotekPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.apply("maven-publish")

        target.group = "no.nav.utbetaling.beregningsplattformen"

        val versjon = target.providers.gradleProperty("bibliotekVersjon")
        val bruker = target.providers.environmentVariable("BIBLIOTEK_PUBLISERING_BRUKER")
        val token = target.providers.environmentVariable("BIBLIOTEK_PUBLISERING_TOKEN")

        versjon.orNull?.let { target.version = it }

        target.pluginManager.withPlugin("java") {
            target.extensions.configure(PublishingExtension::class.java) {
                publications.create("mavenJava", MavenPublication::class.java) {
                    from(target.components.getByName("java"))
                    artifactId = target.name
                }

                repositories.maven {
                    name = "GitHubPackages"
                    url = URI.create("https://maven.pkg.github.com/navikt/beregningsplattformen")
                    credentials {
                        // Tom streng gjør at Gradles egen validering slipper gjennom, slik at doFirst under gir feilmeldingen.
                        username = bruker.getOrElse("")
                        password = token.getOrElse("")
                    }
                }
            }

            // Kravene gjelder bare når noe faktisk publiseres, ikke ved vanlig bygging.
            target.tasks.withType(PublishToMavenRepository::class.java).configureEach {
                doFirst {
                    check(versjon.isPresent) { "bibliotekVersjon må settes (-PbibliotekVersjon=...)." }
                    check(bruker.isPresent) { "BIBLIOTEK_PUBLISERING_BRUKER må settes." }
                    check(token.isPresent) { "BIBLIOTEK_PUBLISERING_TOKEN må settes." }
                }
            }
        }
    }
}
