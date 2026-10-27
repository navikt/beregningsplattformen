package no.nav.sokos.dare.poc.oppdrag

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

import kotlinx.serialization.Serializable

import no.nav.sokos.dare.poc.aktoer.AktoerId
import no.nav.sokos.dare.poc.avvent.Avvent
import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.beregning.isWeekday
import no.nav.sokos.dare.poc.structural.IdValueClass
import no.nav.sokos.dare.poc.trekk.FagomraadeKode

data class Oppdrag(
    val id: OppdragId? = null,
    val fagsystemId: String,
    val fagomraade: FagomraadeKode,
    val datoForfall: LocalDate? = null,
    val gjelderId: AktoerId? = null,
    val datoTom: LocalDate? = null,
    val bilagsType: String? = null,
    val bruker: String,
    val tidspktReg: LocalDateTime,
    val oppdragslinjekjeder: List<Oppdragslinjekjede> = emptyList(),
    val avventPerioder: List<Avvent> = emptyList(),
) {
    val alleOppdragslinjer: List<Oppdragslinje>
        get() = oppdragslinjekjeder.flatMap(Oppdragslinjekjede::ordnedeLinjer)

    fun medOppdragslinjer(linjer: List<Oppdragslinje>): Oppdrag {
        if (linjer.isEmpty()) {
            return copy(oppdragslinjekjeder = emptyList())
        }

        val kjedeOppdragId = requireNotNull(id) { "Oppdrag må ha ID før oppdragslinjer kan knyttes til kjeder" }
        return copy(
            oppdragslinjekjeder = Oppdragslinjekjede.fraOppdragslinjer(kjedeOppdragId, linjer),
        )
    }

    fun forDag(dato: LocalDate): BigDecimal =
        if (dato.isWeekday()) {
            oppdragslinjekjeder
                .flatMap { it.ordnedeLinjer() }
                .filter {
                    it.periode.contains(dato)
                }.sumOf { it.sats }
        } else {
            BigDecimal.ZERO
        }

    // TODO: Dette må fikses når vi implementerer kjeder uten overlapp
    fun vedtakSatsForDag(dato: LocalDate): BigDecimal =
        if (dato.isWeekday()) {
            oppdragslinjekjeder
                .flatMap { it.ordnedeLinjer() }
                .filter { it.periode.contains(dato) }
                .sumOf { it.vedtakssats ?: BigDecimal.ZERO }
        } else {
            BigDecimal.ZERO
        }

    fun trekkgrunnlag(periode: DatoIntervall): BigDecimal = periode.hverdager.sumOf { trekkgrunnlagForDag(it) }

    private fun trekkgrunnlagForDag(dato: LocalDate): BigDecimal =
        if (dato.isWeekday()) {
            oppdragslinjekjeder
                .flatMap { it.ordnedeLinjer() }
                .filter {
                    it.periode.contains(dato) && it.kodeKlasse.grunnlagTrekk
                }.sumOf { it.sats }
        } else {
            BigDecimal.ZERO
        }
}

@Serializable
@JvmInline
value class OppdragId(
    override val id: Long,
) : IdValueClass
