package no.nav.sokos.dare.poc

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.TreeMap

import kotlin.system.measureTimeMillis
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.assertions.nondeterministic.eventuallyConfig
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult

import no.nav.sokos.dare.poc.aktoer.AktoerId
import no.nav.sokos.dare.poc.beregning.Dagbeloep
import no.nav.sokos.dare.poc.beregning.EkstraInfo
import no.nav.sokos.dare.poc.beregning.dataklasser.BeregningsStatus
import no.nav.sokos.dare.poc.beregning.fordeler.FordelingInput
import no.nav.sokos.dare.poc.oppdrag.Oppdrag
import no.nav.sokos.dare.poc.oppdrag.OppdragConverter.toOppdrag
import no.nav.sokos.dare.poc.oppdrag.OppdragConverter.toOppdragslinjer
import no.nav.sokos.dare.poc.oppdrag.OppdragId
import no.nav.sokos.dare.poc.oppdrag.YtelseKlasseKode
import no.nav.sokos.dare.poc.oppdrag.YtelseKlasseKodeRef
import no.nav.sokos.dare.poc.oppdrag25.Oppdrag110
import no.nav.sokos.dare.poc.util.xmlMapper

object TestUtil {
    val klassekodeLookup: Map<YtelseKlasseKodeRef, YtelseKlasseKode>
        get() = KlassekodeTestdata.klassekodeLookup

    fun oppdragFraFil(fileName: String): Oppdrag =
        xmlMapper.readValue<Oppdrag110>(readFile(fileName)).let { oppdrag110 ->
            val oppdragId = OppdragId(0)
            val aktoerIdByOffNr = mutableMapOf(oppdrag110.oppdragGjelderId to AktoerId(1))

            fun aktoerIdFor(offNr: String): AktoerId = aktoerIdByOffNr.getOrPut(offNr) { AktoerId(aktoerIdByOffNr.size + 1L) }

            val linjer =
                oppdrag110
                    .toOppdragslinjer(klassekodeLookup)
                    .zip(oppdrag110.oppdragsLinje150)
                    .map { (oppdragslinje, xmlLinje) ->
                        oppdragslinje.copy(
                            utbetalesTilId = xmlLinje.utbetalesTilId?.let(::aktoerIdFor),
                            skyldnerId = xmlLinje.skyldnerId?.let(::aktoerIdFor),
                        )
                    }

            oppdrag110.toOppdrag(AktoerId(1), oppdragId).medOppdragslinjer(linjer)
        }

    fun readFile(fileName: String): String {
        val stream =
            this::class.java.classLoader.getResourceAsStream(fileName)
                ?: throw IllegalStateException("File '$fileName' could not be read")
        return stream.bufferedReader().use { it.readText() }
    }

    /**
     * Konfigurasjon for 'eventually' i kotest, som brukes for å vente på asynkrone hendelser i tester.
     */
    val eventuallyConfiguration =
        eventuallyConfig {
            initialDelay = Duration.ZERO
            interval = 100.milliseconds
            retries = 3
        }

    fun listOfDagbeloep(
        dato: LocalDate,
        vararg vals: Long,
    ) = vals.mapIndexed { i, l -> Dagbeloep(dato = dato.plusDays(i.toLong()), beloep = BigDecimal.valueOf(l)) }

    fun mapOfDateValue(
        dato: LocalDate,
        vararg vals: Long,
    ) = vals.mapIndexed { i, l -> dato.plusDays(i.toLong()) to BigDecimal.valueOf(l) }.toMap(TreeMap())

    fun listOfBD(vararg vals: Long) = vals.map { BigDecimal.valueOf(it) }

    fun beSameValue(expected: BigDecimal) =
        Matcher<BigDecimal> { actual ->
            MatcherResult(
                actual.compareTo(expected) == 0,
                { "Expected $actual to be equal to $expected" },
                { "Expected $actual to be not equal to $expected" },
            )
        }

    fun beSameValues(expected: List<BigDecimal>) =
        Matcher<List<BigDecimal>> { actual ->
            val same = (actual.size == expected.size) && actual.zip(expected).all { (a, b) -> a.compareTo(b) == 0 }
            MatcherResult(
                same,
                { "Expected $actual to be the equal to $expected" },
                { "Expected $actual to be not equal to $expected" },
            )
        }

    fun <K, V> beSameMap(expected: Map<K, V>) =
        Matcher<Map<K, V>> { actual ->
            MatcherResult(
                actual == expected,
                { "Expected $actual to be equal to $expected" },
                { "Expected $actual to be not equal to $expected" },
            )
        }

    inline fun <T> timeLimit(
        limitMs: Long,
        thunk: () -> T,
    ): T {
        var result: T? = null
        val elapsedMs =
            measureTimeMillis {
                result = thunk()
            }

        check(elapsedMs <= limitMs) {
            "Expected execution within ${limitMs}ms, but took ${elapsedMs}ms"
        }
        return result!!
    }
}

fun testFordelingInput(
    konto: String = "",
    gjelder: AktoerId = AktoerId(1),
    beregnet: LocalDateTime = LocalDateTime.now(),
    utbetalesTil: AktoerId? = null,
    status: BeregningsStatus = BeregningsStatus.BEREGNET,
    ekstraInfo: MutableList<EkstraInfo> = mutableListOf(),
    overfoeringsdato: LocalDate? = LocalDate.of(2025, 4, 1),
) = FordelingInput(konto, gjelder, beregnet, utbetalesTil, status, ekstraInfo, overfoeringsdato)
