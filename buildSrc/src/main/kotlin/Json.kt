internal fun String.somJson() = "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

internal fun <T> Iterable<T>.somJsonArray(tilJson: (T) -> String) =
    joinToString(separator = ",", prefix = "[", postfix = "]") { tilJson(it) }

internal fun Iterable<String>.somJsonArray() = somJsonArray { it.somJson() }

internal fun jsonObjekt(vararg felter: Pair<String, String>) =
    felter.joinToString(separator = ",", prefix = "{", postfix = "}") { (navn, verdi) -> "${navn.somJson()}:$verdi" }

internal fun Miljø.somJson() =
    jsonObjekt(
        "cluster" to cluster.somJson(),
        "naiseratorPath" to naiseratorPath.somJson(),
    )

internal fun App.somJson() =
    jsonObjekt(
        "modul" to modul.somJson(),
        "katalog" to katalog.somJson(),
        "navn" to navn.somJson(),
        "testmiljoer" to testmiljøer.somJsonArray { it.somJson() },
        "prodmiljoer" to prodmiljøer.somJsonArray { it.somJson() },
    )
