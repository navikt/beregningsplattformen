import org.gradle.api.logging.Logger
import java.io.ByteArrayOutputStream
import java.io.File

internal data class GitReferanse(val referanse: String) {
    companion object {
        val OriginMain = GitReferanse("origin/main")
        val Head = GitReferanse("HEAD")
    }
}

internal class PåvirkedeModuler(
    private val prosjektRot: File,
    private val modulKataloger: Map<String, String>,
    private val direkteAvhengigheter: Map<String, Set<String>>,
    private val logger: Logger,
) {
    // Filer som påvirker hele byggingen uansett hvilken modul de ligger i.
    // build-logic/ er ikke med, siden pluginene der ikke endrer hvordan modulene bygges eller testes.
    // Legges det til convention-plugins (for eksempel felles Kotlin-oppsett), må build-logic/ inn her.
    private val globaleFiler =
        listOf(
            "settings.gradle.kts",
            "build.gradle.kts",
            "gradle.properties",
            "gradle/libs.versions.toml",
            "gradle/wrapper/",
            "gradlew",
            "gradlew.bat",
        )


    fun finn(
        siden: GitReferanse,
        til: GitReferanse = GitReferanse.Head,
    ): Set<String> {
        val endredeFiler = finnEndredeFiler(siden, til)
        if (endredeFiler == null) {
            logger.warn("Fant ikke felles stamfar mellom '${siden.referanse}' og '${til.referanse}' (force-push eller manglende fetch?) – alle moduler er påvirket.")
            return modulKataloger.keys.toSortedSet()
        }
        if (endredeFiler.isEmpty()) {
            logger.lifecycle("Ingen endrede filer funnet siden ${siden.referanse}.")
            return emptySet()
        }

        val buildLogic = setOf(":build-logic").takeIf { endredeFiler.any { it.startsWith("build-logic/") } }.orEmpty()

        if (endredeFiler.any { fil -> globaleFiler.any { fil == it || fil.startsWith(it) } }) {
            logger.lifecycle("Global fil er endret – alle moduler er påvirket.")
            return (modulKataloger.keys + buildLogic).toSortedSet()
        }

        return (utvidMedAvhengige(modulerForFiler(endredeFiler)) + buildLogic).toSortedSet()
    }

    /** Null når det ikke finnes noen felles stamfar, for eksempel fordi `siden` ikke finnes etter en force-push. */
    private fun finnEndredeFiler(siden: GitReferanse, til: GitReferanse): List<String>? {
        val stamfar = gitEllerNull("merge-base", siden.referanse, til.referanse)?.trim()
        if (stamfar.isNullOrEmpty()) {
            return null
        }
        return git("diff", "--name-only", stamfar, til.referanse)
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    /** Modulene som eier minst én av de endrede filene. */
    private fun modulerForFiler(filer: List<String>): Set<String> {
        // Lengst matchende katalog vinner, slik at apps/lab/backend slår apps/lab.
        val sortertEtterDybde = modulKataloger.entries.sortedByDescending { it.value.length }
        val treff = mutableSetOf<String>()
        for (fil in filer) {
            val modul =
                sortertEtterDybde.firstOrNull { (_, katalog) ->
                    fil == katalog || fil.startsWith("$katalog/")
                }
            if (modul != null) {
                treff.add(modul.key)
            }
        }
        return treff
    }

    /** Utvider settet med alle moduler som direkte eller indirekte avhenger av en påvirket modul. */
    private fun utvidMedAvhengige(startmoduler: Set<String>): Set<String> {
        val resultat = startmoduler.toMutableSet()
        var endret = true
        while (endret) {
            endret = false
            for ((modul, avhengerAv) in direkteAvhengigheter) {
                if (modul !in resultat && avhengerAv.any { it in resultat }) {
                    resultat.add(modul)
                    endret = true
                }
            }
        }
        return resultat.toSortedSet()
    }

    private fun gitEllerNull(vararg args: String): String? =
        runCatching { git(*args) }
            .onFailure { logger.info(it.message) }
            .getOrNull()

    private fun git(vararg args: String): String {
        val ut = ByteArrayOutputStream()
        val feil = ByteArrayOutputStream()
        val prosess =
            ProcessBuilder(listOf("git", "-c", "core.quotepath=false") + args)
                .directory(prosjektRot)
                .start()
        prosess.inputStream.copyTo(ut)
        prosess.errorStream.copyTo(feil)
        val kode = prosess.waitFor()
        if (kode != 0) {
            error("git ${args.joinToString(" ")} feilet (exit $kode): ${feil.toString(Charsets.UTF_8).trim()}")
        }
        return ut.toString(Charsets.UTF_8)
    }
}
