package no.nav.beregning.task.cron

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class CronExpressionTest : FunSpec({
    test("daglig kl 02:00 gir neste forekomst") {
        val cron = CronExpression.create("0 0 2 * * ?")
        val fra = LocalDateTime.of(2026, 1, 1, 10, 0)
        cron.nextLocalDateTimeAfter(fra) shouldBe LocalDateTime.of(2026, 1, 2, 2, 0)
    }

    test("hvert minutt gir neste minutt") {
        val cron = CronExpression.create("0 * * * * ?")
        val fra = LocalDateTime.of(2026, 1, 1, 10, 30, 15)
        cron.nextLocalDateTimeAfter(fra) shouldBe LocalDateTime.of(2026, 1, 1, 10, 31, 0)
    }

    test("ugyldig uttrykk kaster") {
        try {
            CronExpression.create("tull")
            throw AssertionError("skulle kastet")
        } catch (e: IllegalArgumentException) {
            // forventet
        }
    }
})
