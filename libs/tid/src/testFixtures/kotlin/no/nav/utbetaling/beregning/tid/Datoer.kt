package no.nav.utbetaling.beregning.tid

import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import java.time.YearMonth

import no.nav.utbetaling.beregning.tid.Periode.LukketPeriode
import no.nav.utbetaling.beregning.tid.Periode.LukketPeriode.Companion.til

private const val DEFAULT_ÅR = 2024 // Starter på en mandag

private fun måned(måned: Int): LukketPeriode = YearMonth.of(DEFAULT_ÅR, måned).let { it.atDay(1) til it.atEndOfMonth() }

val januar: LukketPeriode = måned(1)

val februar: LukketPeriode = måned(2)

val mars: LukketPeriode = måned(3)

val april: LukketPeriode = måned(4)

val mai: LukketPeriode = måned(5)

val juni: LukketPeriode = måned(6)

val juli: LukketPeriode = måned(7)

val august: LukketPeriode = måned(8)

val september: LukketPeriode = måned(9)

val oktober: LukketPeriode = måned(10)

val november: LukketPeriode = måned(11)

val desember: LukketPeriode = måned(12)

val Int.januar get() = this.januar(DEFAULT_ÅR)
val Int.februar get() = this.februar(DEFAULT_ÅR)
val Int.mars get() = this.mars(DEFAULT_ÅR)
val Int.april get() = this.april(DEFAULT_ÅR)
val Int.mai get() = this.mai(DEFAULT_ÅR)
val Int.juni get() = this.juni(DEFAULT_ÅR)
val Int.juli get() = this.juli(DEFAULT_ÅR)
val Int.august get() = this.august(DEFAULT_ÅR)
val Int.september get() = this.september(DEFAULT_ÅR)
val Int.oktober get() = this.oktober(DEFAULT_ÅR)
val Int.november get() = this.november(DEFAULT_ÅR)
val Int.desember get() = this.desember(DEFAULT_ÅR)

fun Int.januar(year: Int): LocalDate = LocalDate.of(year, 1, this)

fun Int.februar(year: Int): LocalDate = LocalDate.of(year, 2, this)

fun Int.mars(year: Int): LocalDate = LocalDate.of(year, 3, this)

fun Int.april(year: Int): LocalDate = LocalDate.of(year, 4, this)

fun Int.mai(year: Int): LocalDate = LocalDate.of(year, 5, this)

fun Int.juni(year: Int): LocalDate = LocalDate.of(year, 6, this)

fun Int.juli(year: Int): LocalDate = LocalDate.of(year, 7, this)

fun Int.august(year: Int): LocalDate = LocalDate.of(year, 8, this)

fun Int.september(year: Int): LocalDate = LocalDate.of(year, 9, this)

fun Int.oktober(year: Int): LocalDate = LocalDate.of(year, 10, this)

fun Int.november(year: Int): LocalDate = LocalDate.of(year, 11, this)

fun Int.desember(year: Int): LocalDate = LocalDate.of(year, 12, this)

private fun DayOfWeek.checkDayOfWeek(dato: LocalDate) =
    dato.also {
        check(this == dato.dayOfWeek) { "Forventet at $dato skulle være $this, men var ${dato.dayOfWeek}" }
    }

fun mandag(dato: LocalDate) = MONDAY.checkDayOfWeek(dato)

fun tirsdag(dato: LocalDate) = TUESDAY.checkDayOfWeek(dato)

fun onsdag(dato: LocalDate) = WEDNESDAY.checkDayOfWeek(dato)

fun torsdag(dato: LocalDate) = THURSDAY.checkDayOfWeek(dato)

fun fredag(dato: LocalDate) = FRIDAY.checkDayOfWeek(dato)

fun lørdag(dato: LocalDate) = SATURDAY.checkDayOfWeek(dato)

fun søndag(dato: LocalDate) = SUNDAY.checkDayOfWeek(dato)
