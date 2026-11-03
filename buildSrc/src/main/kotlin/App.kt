import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension

internal data class Miljø(
    val miljø: String,
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
                val (prodmiljøer, testmiljøer) = miljøer.partition { it.miljø.startsWith("prod") }
                App(
                    modul = modul,
                    katalog = modulKatalog.relativTil(rot),
                    navn = modul.removePrefix(":apps:").replace(':', '-'),
                    testmiljøer = testmiljøer,
                    prodmiljøer = prodmiljøer,
                )
            }

        private fun miljøer(rot: Path, modulKatalog: Path): List<Miljø> {
            val naisKatalog = modulKatalog.resolve(".nais")
            if (!naisKatalog.exists()) return emptyList()
            return naisKatalog.listDirectoryEntries("naiserator-*.yaml")
                .map { naiseratorPath ->
                    Miljø(
                        miljø = naiseratorPath.nameWithoutExtension.removePrefix("naiserator-"),
                        naiseratorPath = naiseratorPath.relativTil(rot),
                    )
                }
                .sortedBy { it.miljø }
        }

        private fun Path.relativTil(rot: Path) = rot.relativize(this).invariantSeparatorsPathString
    }
}
