import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.gradle.api.logging.Logging
import java.io.File

class PåvirkedeModulerTest :
    FunSpec({
        val prosjektRot = File("..").canonicalFile
        val moduler =
            PåvirkedeModuler(
                prosjektRot = prosjektRot,
                modulKataloger =
                    mapOf(
                        ":apps:lab:backend" to "apps/lab/backend",
                        ":libs:beregning" to "libs/beregning",
                    ),
                direkteAvhengigheter = mapOf(":apps:lab:backend" to setOf(":libs:beregning")),
                logger = Logging.getLogger(PåvirkedeModulerTest::class.java)
            )

        test("endring i én modul gir modulen") {
            // b5b891a6 endrer bare apps/lab/backend/src/main/.../OppdragstidslinjeDTO.kt
            moduler.finn(siden = GitReferanse("b5b891a6~1"), til = GitReferanse("b5b891a6")) shouldBe
                setOf(":apps:lab:backend")
        }

        test("endring i bibliotek gir også moduler som avhenger av det") {
            // 2d631f2f endrer libs/beregning/build.gradle.kts (og buildSrc, som ikke lenger finnes)
            moduler.finn(siden = GitReferanse("2d631f2f~1"), til = GitReferanse("2d631f2f")) shouldBe
                setOf(":apps:lab:backend", ":libs:beregning")
        }

        test("endring bare i workflow-filer gir ingen moduler") {
            moduler.finn(siden = GitReferanse("68180f76~1"), til = GitReferanse("68180f76")).shouldBeEmpty()
        }

        test("samme referanse for siden og til gir ingen moduler") {
            moduler.finn(siden = GitReferanse("b5b891a6"), til = GitReferanse("b5b891a6")).shouldBeEmpty()
        }

        test("endring i build-logic gir alle moduler og :build-logic") {
            // 62a97b8b endrer bare build-logic/src/main/kotlin/PåvirkedeModuler.kt
            moduler.finn(siden = GitReferanse("62a97b8b~1"), til = GitReferanse("62a97b8b")) shouldBe
                setOf(":apps:lab:backend", ":build-logic", ":libs:beregning")
        }

        test("endring i annen global fil gir alle moduler, men ikke :build-logic") {
            // bb787b2a endrer bare settings.gradle.kts
            moduler.finn(siden = GitReferanse("bb787b2a~1"), til = GitReferanse("bb787b2a")) shouldBe
                setOf(":apps:lab:backend", ":libs:beregning")
        }
    })
