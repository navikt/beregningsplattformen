package no.nav.utbetaling.beregning.tid

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import no.nav.utbetaling.beregning.tid.Periode.LukketPeriode
import no.nav.utbetaling.beregning.tid.Periode.LukketPeriode.Companion.forrigeDag
import no.nav.utbetaling.beregning.tid.Periode.LukketPeriode.Companion.grupperSammenhengendePerioder
import no.nav.utbetaling.beregning.tid.Periode.LukketPeriode.Companion.grupperSammenhengendePerioderMedHensynTilHelg
import no.nav.utbetaling.beregning.tid.Periode.LukketPeriode.Companion.nesteDag
import no.nav.utbetaling.beregning.tid.Periode.LukketPeriode.Companion.somLukketPeriode
import no.nav.utbetaling.beregning.tid.Periode.LukketPeriode.Companion.til
import no.nav.utbetaling.beregning.tid.Periode.ÅpenPeriode
import java.time.LocalDate

class PeriodeTest :
    FunSpec({
        val torsdag = torsdag(4.januar)
        val fredag = fredag(5.januar)
        val lørdag = lørdag(6.januar)
        val søndag = søndag(7.januar)
        val mandag = mandag(8.januar)
        val onsdag = onsdag(10.januar)

        context("LukketPeriode") {
            test("skal ikke godta at fom er etter tom") {
                shouldThrow<IllegalArgumentException> { 10.januar til 5.januar }
            }

            test("skal godta en periode på én dag") {
                val periode = 5.januar.somLukketPeriode()

                periode.fom shouldBe 5.januar
                periode.tom shouldBe 5.januar
                periode.toList() shouldContainExactly listOf(5.januar)
            }

            test("skal iterere over hver dag i perioden") {
                (5.januar til 8.januar).toList() shouldContainExactly listOf(5.januar, 6.januar, 7.januar, 8.januar)
            }

            test("skal la start og endInclusive følge fom og tom") {
                val periode = 5.januar til 8.januar

                periode.start shouldBe 5.januar
                periode.endInclusive shouldBe 8.januar
            }

            test("skal bare inneholde datoer innenfor fom og tom") {
                val periode = 5.januar til 8.januar

                (4.januar in periode) shouldBe false
                (5.januar in periode) shouldBe true
                (8.januar in periode) shouldBe true
                (9.januar in periode) shouldBe false
            }
        }

        context("ÅpenPeriode") {
            test("skal ikke ha noen sluttdato") {
                (LocalDate.MAX in ÅpenPeriode(5.januar)) shouldBe true
            }

            test("skal inneholde alle datoer fra og med fom") {
                val periode = ÅpenPeriode(5.januar)

                (4.januar in periode) shouldBe false
                (5.januar in periode) shouldBe true
                (5.januar(2074) in periode) shouldBe true
            }
        }

        context("contains(Periode)") {
            test("skal inneholde en lukket periode som ligger inni en lukket periode") {
                ((5.januar til 8.januar) in (1.januar til 10.januar)) shouldBe true
            }

            test("skal ikke inneholde en lukket periode som strekker seg forbi tom") {
                ((5.januar til 11.januar) in (1.januar til 10.januar)) shouldBe false
            }

            test("skal aldri la en lukket periode inneholde en åpen periode") {
                (ÅpenPeriode(5.januar) in (1.januar til 10.januar)) shouldBe false
            }

            test("skal la en åpen periode inneholde en lukket periode som starter etter fom") {
                ((5.januar til 8.januar) in ÅpenPeriode(1.januar)) shouldBe true
                ((1.januar til 8.januar) in ÅpenPeriode(5.januar)) shouldBe false
            }

            test("skal la en åpen periode inneholde en åpen periode som starter senere") {
                (ÅpenPeriode(5.januar) in ÅpenPeriode(1.januar)) shouldBe true
                (ÅpenPeriode(1.januar) in ÅpenPeriode(5.januar)) shouldBe false
            }
        }

        context("overlapperMed") {
            test("skal se overlapp mellom to lukkede perioder") {
                (1.januar til 10.januar).overlapperMed(5.januar til 15.januar) shouldBe true
                (5.januar til 15.januar).overlapperMed(1.januar til 10.januar) shouldBe true
            }

            test("skal ikke se overlapp når lukkede perioder ligger kant i kant") {
                (1.januar til 10.januar).overlapperMed(11.januar til 15.januar) shouldBe false
            }

            test("skal se overlapp mellom lukket og åpen periode") {
                (1.januar til 10.januar).overlapperMed(ÅpenPeriode(10.januar)) shouldBe true
                (1.januar til 10.januar).overlapperMed(ÅpenPeriode(11.januar)) shouldBe false
                ÅpenPeriode(11.januar).overlapperMed(1.januar til 10.januar) shouldBe false
            }

            test("skal alltid se overlapp mellom to åpne perioder") {
                ÅpenPeriode(1.januar).overlapperMed(ÅpenPeriode(20.januar)) shouldBe true
            }
        }

        context("overlappendePeriode") {
            test("skal gi null når periodene ikke overlapper") {
                (1.januar til 5.januar).overlappendePeriode(20.januar til 25.januar).shouldBeNull()
            }

            test("skal gi null når en åpen periode starter etter en lukket periode") {
                ÅpenPeriode(20.januar).overlappendePeriode(1.januar til 10.januar).shouldBeNull()
            }

            test("skal gi snittet av to lukkede perioder") {
                (1.januar til 10.januar).overlappendePeriode(5.januar til 15.januar) shouldBe (5.januar til 10.januar)
            }

            test("skal gi en lukket periode når bare én av periodene er åpen") {
                (1.januar til 10.januar).overlappendePeriode(ÅpenPeriode(5.januar)) shouldBe (5.januar til 10.januar)
                ÅpenPeriode(5.januar).overlappendePeriode(1.januar til 10.januar) shouldBe (5.januar til 10.januar)
            }

            test("skal gi en åpen periode fra seneste fom når begge periodene er åpne") {
                ÅpenPeriode(1.januar).overlappendePeriode(ÅpenPeriode(20.januar)) shouldBe ÅpenPeriode(20.januar)
            }
        }

        context("omsluttende") {
            test("skal dekke oppholdet mellom to lukkede perioder") {
                (1.januar til 5.januar).omsluttende(20.januar til 25.januar) shouldBe (1.januar til 25.januar)
            }

            test("skal beholde den seneste tom når periodene overlapper") {
                (1.januar til 10.januar).omsluttende(5.januar til 25.januar) shouldBe (1.januar til 25.januar)
            }

            test("skal bli åpen når en av periodene er åpen") {
                (1.januar til 5.januar).omsluttende(ÅpenPeriode(20.januar)) shouldBe ÅpenPeriode(1.januar)
            }

            test("skal starte på tidligste fom når begge periodene er åpne") {
                ÅpenPeriode(20.januar).omsluttende(ÅpenPeriode(5.januar)) shouldBe ÅpenPeriode(5.januar)
            }
        }

        context("inneholderNoenAv") {
            test("skal være sann når minst én dato ligger i perioden") {
                (5.januar til 8.januar).inneholderNoenAv(listOf(1.januar, 7.januar)) shouldBe true
                ÅpenPeriode(5.januar).inneholderNoenAv(listOf(1.januar, 7.januar)) shouldBe true
            }

            test("skal være usann når ingen datoer ligger i perioden") {
                (5.januar til 8.januar).inneholderNoenAv(listOf(1.januar, 20.januar)) shouldBe false
                (5.januar til 8.januar).inneholderNoenAv(emptyList()) shouldBe false
            }
        }

        context("grupperSammenhengendePerioder") {
            test("skal slå sammen perioder som ligger kant i kant eller overlapper") {
                val perioder = listOf(1.januar til 5.januar, 6.januar til 10.januar, 8.januar til 20.januar)

                perioder.grupperSammenhengendePerioder() shouldContainExactly listOf(1.januar til 20.januar)
            }

            test("skal beholde oppholdet mellom to perioder") {
                val perioder = listOf(1.januar til 5.januar, 7.januar til 10.januar)

                perioder.grupperSammenhengendePerioder() shouldContainExactly listOf(1.januar til 5.januar, 7.januar til 10.januar)
            }

            test("skal takle usortert rekkefølge") {
                val perioder = listOf(6.januar til 9.januar, 1.januar til 5.januar)

                perioder.grupperSammenhengendePerioder() shouldContainExactly listOf(1.januar til 9.januar)
            }

            test("skal absorbere en periode som ligger inni en annen") {
                val perioder = listOf(1.januar til 30.januar, 5.januar til 10.januar)

                perioder.grupperSammenhengendePerioder() shouldContainExactly listOf(1.januar til 30.januar)
            }

            test("skal gi en tom liste når det ikke finnes perioder") {
                emptyList<LukketPeriode>().grupperSammenhengendePerioder() shouldContainExactly emptyList()
            }

            test("skal ikke slå sammen over en helg") {
                val perioder = listOf(fredag.somLukketPeriode(), mandag.somLukketPeriode())

                perioder.grupperSammenhengendePerioder() shouldContainExactly listOf(fredag.somLukketPeriode(), mandag.somLukketPeriode())
            }
        }

        context("grupperSammenhengendePerioderMedHensynTilHelg") {
            test("skal slå sammen fra fredag til mandag") {
                val perioder = listOf(fredag.somLukketPeriode(), mandag.somLukketPeriode())

                perioder.grupperSammenhengendePerioderMedHensynTilHelg() shouldContainExactly listOf(fredag til mandag)
            }

            test("skal slå sammen fra fredag til søndag") {
                val perioder = listOf(fredag.somLukketPeriode(), søndag.somLukketPeriode())

                perioder.grupperSammenhengendePerioderMedHensynTilHelg() shouldContainExactly listOf(fredag til søndag)
            }

            test("skal slå sammen fra lørdag til mandag") {
                val perioder = listOf(lørdag.somLukketPeriode(), mandag.somLukketPeriode())

                perioder.grupperSammenhengendePerioderMedHensynTilHelg() shouldContainExactly listOf(lørdag til mandag)
            }

            test("skal ikke slå sammen over et opphold som ikke er en helg") {
                val perioder = listOf(torsdag.somLukketPeriode(), lørdag.somLukketPeriode())

                perioder.grupperSammenhengendePerioderMedHensynTilHelg() shouldContainExactly
                    listOf(torsdag.somLukketPeriode(), lørdag.somLukketPeriode())
            }

            test("skal slå sammen perioder som ligger kant i kant midt i uka") {
                val perioder = listOf(mandag.somLukketPeriode(), mandag.nesteDag.somLukketPeriode())

                perioder.grupperSammenhengendePerioderMedHensynTilHelg() shouldContainExactly listOf(mandag til mandag.nesteDag)
            }

            test("skal slå sammen overlappende perioder") {
                val perioder = listOf(1.januar til 10.januar, 5.januar til 15.januar)

                perioder.grupperSammenhengendePerioderMedHensynTilHelg() shouldContainExactly listOf(1.januar til 15.januar)
            }

            test("skal ikke slå sammen fra mandag til onsdag") {
                val perioder = listOf(mandag.somLukketPeriode(), onsdag.somLukketPeriode())

                perioder.grupperSammenhengendePerioderMedHensynTilHelg() shouldContainExactly
                    listOf(mandag.somLukketPeriode(), onsdag.somLukketPeriode())
            }
        }

        context("datohjelpere") {
            test("skal gi neste og forrige dag") {
                5.januar.nesteDag shouldBe 6.januar
                5.januar.forrigeDag shouldBe 4.januar
            }

            test("skal krysse månedsskifte") {
                31.januar.nesteDag shouldBe 1.februar
                1.februar.forrigeDag shouldBe 31.januar
            }
        }

        context("toString") {
            test("skal skrive ut en lukket periode med begge datoene") {
                (5.januar til 8.januar).toString() shouldBe "05-01-2024 til 08-01-2024"
            }

            test("skal skrive ut en åpen periode uten tom") {
                ÅpenPeriode(5.januar).toString() shouldBe "Fra og med 05-01-2024"
            }
        }
    })
