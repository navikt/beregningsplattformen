import App.Companion.app
import App.Companion.apper
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

abstract class AppDeployConfigTask : DefaultTask() {
    @get:Input
    abstract val modulKataloger: MapProperty<String, String>

    @get:Internal
    abstract val prosjektRot: DirectoryProperty

    @get:Input
    @get:Option(option = "app", description = "Navnet på appen som skal deployes. Må ikke være ordrett, men være unikt nok for å finne bare en app.")
    abstract val app: Property<String>

    @get:Input
    @get:Option(option = "miljo", description = "Miljøet du skal deploye til. Må ikke være ordrett, men være unikt nok for å finne bare et miljø.")
    abstract val miljø: Property<String>

    @TaskAction
    fun kjør() {
        val modulkataloger = modulKataloger.get()
        val rot = prosjektRot.get().asFile.toPath()

        val funnet = modulkataloger.keys.app(
            rot = rot,
            modulKataloger = modulkataloger,
            navn = app.get(),
            miljø = miljø.get(),
        ) ?: throw GradleException(
            "Fant ingen unik app og ett unikt miljø for --app=${app.get()} og --miljo=${miljø.get()}. Tilgjengelige:\n" +
                modulkataloger.keys.apper(rot, modulkataloger).joinToString("\n") { app ->
                    "  ${app.navn}: " + (app.testmiljøer + app.prodmiljøer).joinToString { it.naiseratorPath }
                },
        )

        println(funnet.somJson())
    }
}
