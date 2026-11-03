import org.gradle.api.logging.Logger
import java.io.ByteArrayOutputStream
import java.io.File

internal data class GitReferanse(val referanse: String) {
    companion object {
        // https://docs.github.com/en/webhooks/webhook-events-and-payloads#push
        fun utgangspunktFraGitHubEvent(): GitReferanse? {
            val referanse = System.getenv("GITHUB_EVENT_PATH")
                ?.let { File(it) }
                ?.takeIf { it.exists() }
                ?.let { Regex("\"before\"\\s*:\\s*\"([0-9a-f]{40})\"").find(it.readText())?.groupValues?.get(1) }
                ?.takeIf { sha -> sha.any { it != '0' } }
            return referanse?.let { GitReferanse(it) }
        }
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
    private val globaleFiler =
        listOf(
            "settings.gradle.kts",
            "build.gradle.kts",
            "gradle.properties",
            "gradle/libs.versions.toml",
            "gradle/wrapper/",
            "gradlew",
            "gradlew.bat",
            "buildSrc/",
        )


    fun finn(
        før: GitReferanse = GitReferanse.utgangspunktFraGitHubEvent() ?: GitReferanse.OriginMain,
        nå: GitReferanse = GitReferanse.Head,
    ): Set<String> {
        val endredeFiler = finnEndredeFiler(før, nå)
        if (endredeFiler.isEmpty()) {
            logger.lifecycle("Ingen endrede filer funnet mot ${før.referanse}.")
            return emptySet()
        }

        if (endredeFiler.any { fil -> globaleFiler.any { fil == it || fil.startsWith(it) } }) {
            logger.lifecycle("Global fil er endret – alle moduler er påvirket.")
            return modulKataloger.keys.toSortedSet()
        }

        return utvidMedAvhengige(modulerForFiler(endredeFiler))
    }

    private fun finnEndredeFiler(før: GitReferanse, nå: GitReferanse): List<String> {
        val stamfar = git("merge-base", før.referanse, nå.referanse).trim()
        if (stamfar.isEmpty()) {
            error("Fant ikke felles stamfar mellom '${før.referanse}' og '${nå.referanse}'. Er base-ref'en hentet (git fetch)?")
        }
        return git("diff", "--name-only", stamfar, nå.referanse)
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
