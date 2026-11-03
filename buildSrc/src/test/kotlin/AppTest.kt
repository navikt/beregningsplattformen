import App.Companion.apper
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.writeText

class AppTest :
    FunSpec({
        lateinit var rot: Path

        beforeTest { rot = tempdir().toPath() }

        fun fil(sti: String) = rot.resolve(sti).createParentDirectories().writeText("")

        val kataloger =
            mapOf(
                ":apps:backend" to "apps/backend",
                ":apps:mottak" to "apps/mottak",
                ":libs:beregning" to "libs/beregning",
            )

        test("miljøer som starter med prod blir prodmiljøer, de andre testmiljøer, underkataloger ignoreres") {
            fil("apps/mottak/.nais/naiserator-prod.yaml")
            fil("apps/mottak/.nais/naiserator-prod-fss.yaml")
            fil("apps/mottak/.nais/naiserator-dev.yaml")
            fil("apps/mottak/.nais/test/naiserator-test.yaml")
            fil("apps/mottak/.nais/alerts.yaml")
            fil("apps/mottak/.nais/naiserator-dev.json")

            setOf(":apps:mottak").apper(rot, kataloger) shouldBe
                listOf(
                    App(
                        ":apps:mottak",
                        "apps/mottak",
                        "mottak",
                        testmiljøer = listOf(Miljø("dev", "apps/mottak/.nais/naiserator-dev.yaml")),
                        prodmiljøer =
                            listOf(
                                Miljø("prod", "apps/mottak/.nais/naiserator-prod.yaml"),
                                Miljø("prod-fss", "apps/mottak/.nais/naiserator-prod-fss.yaml"),
                            ),
                    ),
                )
        }

        test("navn er modulstien uten :apps: med bindestrek, katalog er relativ til roten") {
            fil("apps/lab/backend/.nais/naiserator-dev.yaml")

            setOf(":apps:lab:backend").apper(rot, mapOf(":apps:lab:backend" to "apps/lab/backend")) shouldBe
                listOf(
                    App(
                        ":apps:lab:backend",
                        "apps/lab/backend",
                        "lab-backend",
                        testmiljøer = listOf(Miljø("dev", "apps/lab/backend/.nais/naiserator-dev.yaml")),
                        prodmiljøer = emptyList(),
                    ),
                )
        }

        test("app uten naiserator-filer i .nais gir ingenting") {
            fil("apps/backend/build.gradle.kts")
            fil("apps/backend/.nais/Dockerfile")

            setOf(":apps:backend").apper(rot, kataloger).shouldBeEmpty()
        }

        test("moduler utenfor :apps tas ikke med") {
            fil("libs/beregning/.nais/naiserator-dev.yaml")

            setOf(":libs:beregning").apper(rot, kataloger).shouldBeEmpty()
        }

        test("somJson escaper anførselstegn og backslash, og gir tomme lister") {
            App("a", "b", "c", listOf(Miljø("dev", "sti/med\"fnutt\\x")), listOf(Miljø("prod", "p.yaml"))).somJson() shouldBe
                """{"modul":"a","katalog":"b","navn":"c","testmiljoer":[{"miljo":"dev","naiseratorPath":"sti/med\"fnutt\\x"}],""" +
                """"prodmiljoer":[{"miljo":"prod","naiseratorPath":"p.yaml"}]}"""
            App("a", "b", "c", emptyList(), emptyList()).somJson() shouldBe
                """{"modul":"a","katalog":"b","navn":"c","testmiljoer":[],"prodmiljoer":[]}"""
        }
    })
