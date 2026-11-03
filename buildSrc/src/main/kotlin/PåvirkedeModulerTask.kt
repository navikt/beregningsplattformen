import App.Companion.apper
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

abstract class PåvirkedeModulerTask : DefaultTask() {
    @get:Input
    abstract val modulKataloger: MapProperty<String, String>

    @get:Input
    abstract val direkteAvhengigheter: MapProperty<String, Set<String>>

    @get:Input
    abstract val publiserteBiblioteker: SetProperty<String>

    @get:Internal
    abstract val prosjektRot: DirectoryProperty

    @TaskAction
    fun kjør() {
        val prosjektRotFil = prosjektRot.get().asFile
        val modulkataloger = modulKataloger.get()

        val allePåvirkedeModuler = PåvirkedeModuler(
            prosjektRot = prosjektRotFil,
            modulKataloger = modulkataloger,
            direkteAvhengigheter = direkteAvhengigheter.get(),
            logger = logger
        ).finn()

        // Om det er en scheduled run, så vil man redeploye alle apper selv om de ikke er påvirket av endringer.
        val utgangspunktForApper = when (System.getenv("GITHUB_EVENT_NAME") == "schedule") {
            true -> modulkataloger.keys
            false -> allePåvirkedeModuler
        }

        val apper = utgangspunktForApper.apper(
            rot = prosjektRotFil.toPath(),
            modulKataloger = modulkataloger
        )
        val publiserteBiblioteker = allePåvirkedeModuler.filter { it in publiserteBiblioteker.get() }.toSet()

        val resten = allePåvirkedeModuler - apper.map { it.modul }.toSet() - publiserteBiblioteker

        println(
            jsonObjekt(
                "apper" to apper.somJsonArray { it.somJson() },
                "publiserteBiblioteker" to publiserteBiblioteker.somJsonArray(),
                "resten" to resten.somJsonArray(),
                "alle" to allePåvirkedeModuler.somJsonArray()
            )
        )
    }
}
