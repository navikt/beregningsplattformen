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
            // 2d631f2f endrer libs/beregning/build.gradle.kts, men også buildSrc, som er global
            moduler.finn(siden = GitReferanse("2d631f2f~1"), til = GitReferanse("2d631f2f")) shouldBe
                setOf(":apps:lab:backend", ":libs:beregning")
        }

        test("endring bare i workflow-filer gir ingen moduler") {
            moduler.finn(siden = GitReferanse("68180f76~1"), til = GitReferanse("68180f76")).shouldBeEmpty()
        }

        test("samme referanse for siden og til gir ingen moduler") {
            moduler.finn(siden = GitReferanse("b5b891a6"), til = GitReferanse("b5b891a6")).shouldBeEmpty()
        }
    })
