import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension

internal data class Miljø(
    val cluster: String,
    val naiseratorPath: String
)

internal data class App (
    val modul: String,
    val katalog: String,
    val navn: String,
    val testmiljøer: List<Miljø>,
    val prodmiljøer: List<Miljø>,
) {
    companion object {

        fun Set<String>.apper(rot: Path, modulKataloger: Map<String, String>) = filter { it.startsWith(":apps") }
            .mapNotNull { modul ->
                val modulKatalog = rot.resolve(modulKataloger.getValue(modul))
                val miljøer = miljøer(rot, modulKatalog)
                if (miljøer.isEmpty()) return@mapNotNull null
                val (prodmiljøer, testmiljøer) = miljøer.partition { it.cluster.startsWith("prod") }
                App(
                    modul = modul,
                    katalog = modulKatalog.relativTil(rot),
                    navn = modul.removePrefix(":apps:").replace(':', '-'),
                    testmiljøer = testmiljøer,
                    prodmiljøer = prodmiljøer,
                )
            }

        // Naiserator-filer heter <hva-som-helst>_<cluster>.yaml, for eksempel q1_dev-gcp.yaml.
        private fun miljøer(rot: Path, modulKatalog: Path): List<Miljø> {
            val naisKatalog = modulKatalog.resolve(".nais")
            if (!naisKatalog.exists()) return emptyList()
            return naisKatalog.listDirectoryEntries("*.yaml")
                .mapNotNull { naiseratorPath ->
                    val deler = naiseratorPath.nameWithoutExtension.split("_")
                    if (deler.size != 2 || deler.any { it.isBlank() }) return@mapNotNull null
                    Miljø(
                        cluster = deler[1],
                        naiseratorPath = naiseratorPath.relativTil(rot),
                    )
                }
                .sortedWith(compareBy({ it.cluster }, { it.naiseratorPath }))
        }

        private fun Path.relativTil(rot: Path) = rot.relativize(this).invariantSeparatorsPathString
    }
}
