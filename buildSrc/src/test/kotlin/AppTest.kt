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

        test("clustere som starter med prod blir prodmiljøer, de andre testmiljøer, underkataloger ignoreres") {
            fil("apps/mottak/.nais/app_prod-gcp.yaml")
            fil("apps/mottak/.nais/app_prod-fss.yaml")
            fil("apps/mottak/.nais/app_dev-gcp.yaml")
            fil("apps/mottak/.nais/test/app_test-gcp.yaml")
            fil("apps/mottak/.nais/app_dev-fss.json")

            setOf(":apps:mottak").apper(rot, kataloger) shouldBe
                listOf(
                    App(
                        ":apps:mottak",
                        "apps/mottak",
                        "mottak",
                        testmiljøer = listOf(Miljø("dev-gcp", "apps/mottak/.nais/app_dev-gcp.yaml")),
                        prodmiljøer =
                            listOf(
                                Miljø("prod-fss", "apps/mottak/.nais/app_prod-fss.yaml"),
                                Miljø("prod-gcp", "apps/mottak/.nais/app_prod-gcp.yaml"),
                            ),
                    ),
                )
        }

        test("bare filer med nøyaktig én _ og ikke-blanke deler er naiserator-filer") {
            fil("apps/mottak/.nais/q1_dev-fss.yaml")
            fil("apps/mottak/.nais/q2_dev-fss.yaml")
            fil("apps/mottak/.nais/naiserator-dev.yaml")
            fil("apps/mottak/.nais/a_b_dev-gcp.yaml")
            fil("apps/mottak/.nais/_dev-gcp.yaml")
            fil("apps/mottak/.nais/app_.yaml")
            fil("apps/mottak/.nais/ _dev-gcp.yaml")

            setOf(":apps:mottak").apper(rot, kataloger).single().testmiljøer shouldBe
                listOf(
                    Miljø("dev-fss", "apps/mottak/.nais/q1_dev-fss.yaml"),
                    Miljø("dev-fss", "apps/mottak/.nais/q2_dev-fss.yaml"),
                )
        }

        test("navn er modulstien uten :apps: med bindestrek, katalog er relativ til roten") {
            fil("apps/lab/backend/.nais/lab-backend_dev-gcp.yaml")

            setOf(":apps:lab:backend").apper(rot, mapOf(":apps:lab:backend" to "apps/lab/backend")) shouldBe
                listOf(
                    App(
                        ":apps:lab:backend",
                        "apps/lab/backend",
                        "lab-backend",
                        testmiljøer = listOf(Miljø("dev-gcp", "apps/lab/backend/.nais/lab-backend_dev-gcp.yaml")),
                        prodmiljøer = emptyList(),
                    ),
                )
        }

        test("app uten naiserator-filer i .nais gir ingenting") {
            fil("apps/backend/build.gradle.kts")
            fil("apps/backend/.nais/Dockerfile")
            fil("apps/backend/.nais/alerts.yaml")

            setOf(":apps:backend").apper(rot, kataloger).shouldBeEmpty()
        }

        test("moduler utenfor :apps tas ikke med") {
            fil("libs/beregning/.nais/app_dev-gcp.yaml")

            setOf(":libs:beregning").apper(rot, kataloger).shouldBeEmpty()
        }

        test("somJson escaper anførselstegn og backslash, og gir tomme lister") {
            App("a", "b", "c", listOf(Miljø("dev-gcp", "sti/med\"fnutt\\x")), listOf(Miljø("prod-gcp", "p.yaml"))).somJson() shouldBe
                """{"modul":"a","katalog":"b","navn":"c","testmiljoer":[{"cluster":"dev-gcp","naiseratorPath":"sti/med\"fnutt\\x"}],""" +
                """"prodmiljoer":[{"cluster":"prod-gcp","naiseratorPath":"p.yaml"}]}"""
            App("a", "b", "c", emptyList(), emptyList()).somJson() shouldBe
                """{"modul":"a","katalog":"b","navn":"c","testmiljoer":[],"prodmiljoer":[]}"""
        }
    })
