package no.nav.sokos.dare.poc.beregning

import java.time.LocalDate
import java.time.LocalDateTime

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

import no.nav.sokos.dare.poc.aktoer.AktoerId
import no.nav.sokos.dare.poc.beregning.dataklasser.BeregningsStatus
import no.nav.sokos.dare.poc.beregning.dataklasser.BeregningsStatusflagg
import no.nav.sokos.dare.poc.beregning.ekstern.BeregningsKilde
import no.nav.sokos.dare.poc.structural.IdValueClass
import no.nav.sokos.dare.poc.trekk.FagomraadeKode

@Serializable
data class Beregning(
    val id: BeregningId? = null,
    // TODO: Dette er en peker til oppdrag z sin id. Det bør være klarere. Rename?
    val eksternRef: Long? = null,
    val periode: DatoIntervall,
    val konto: String,
    @Contextual val overfoeres: LocalDate?,
    val fagomraade: FagomraadeKode,
    val gjelder: AktoerId,
    @Contextual val beregnet: LocalDateTime,
    val delberegning: List<Delberegning>,
    val utbetalesTil: AktoerId? = null,
    val kid: String? = null,
    val status: BeregningsStatus = BeregningsStatus.BEREGNET,
    val statusflagg: List<BeregningsStatusflagg> = emptyList(),
    val gyldig: Boolean? = true,
    val kilde: BeregningsKilde = BeregningsKilde.POC,
    val ekstraInfo: Set<EkstraInfo> = emptySet(),
)

@Serializable
@JvmInline
value class BeregningId(
    override val id: Long,
) : IdValueClass
