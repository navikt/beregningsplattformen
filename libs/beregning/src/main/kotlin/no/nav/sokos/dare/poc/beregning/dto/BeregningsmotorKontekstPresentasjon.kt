package no.nav.sokos.dare.poc.beregning.dto

import java.math.BigDecimal

import kotlinx.serialization.Serializable

import no.nav.sokos.dare.poc.BigDecimalJson
import no.nav.sokos.dare.poc.beregning.Beregning
import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.beregning.fordeler.Beregningsgrunnlag
import no.nav.sokos.dare.poc.beregning.fordeler.BeregningsmotorKontekst

@Serializable
data class BeregningsmotorKontekstPresentasjon(
    val periode: DatoIntervall,
    val beregninger: List<BeregningsPresentasjonV2>,
    val saldoBoetter: List<SaldoBoettePresentasjon>,
    val satsBoetter: List<SatsBoettePresentasjon>,
    val initielleSaldoBoetter: List<SaldoBoettePresentasjon> = emptyList(),
    val initielleSatsBoetter: List<SatsBoettePresentasjon> = emptyList(),
    val restFrikortBeloep: BigDecimalJson?,
) {
    constructor(kontekst: BeregningsmotorKontekst, beregninger: List<Beregning>? = null) : this(
        grunnlag = kontekst,
        beregninger = beregninger ?: kontekst.beregninger(),
    )

    constructor(beregninger: List<Beregning>, grunnlag: Beregningsgrunnlag) : this(
        periode = grunnlag.periode,
        beregninger = beregninger.map(::BeregningsPresentasjonV2),
        saldoBoetter =
            grunnlag.saldoBoetter.saldoer.map { (trekkvedtakId, beloep) ->
                SaldoBoettePresentasjon(
                    trekkvedtakId = trekkvedtakId.id,
                    opprinneligSaldo = finnOpprinneligSaldo(grunnlag, trekkvedtakId.id),
                    restSaldo = beloep,
                )
            },
        satsBoetter =
            grunnlag.satsBoetter.satser.map { (id, beloep) ->
                SatsBoettePresentasjon(
                    trekkperiodeId = id.id.id,
                    boette = id.boette,
                    opprinneligSats = finnOpprinneligSats(grunnlag, id.id.id, id.boette),
                    restSats = beloep,
                )
            },
        initielleSaldoBoetter =
            grunnlag.saldoBoetter.innkommendeSaldoer.map { (trekkvedtakId, beloep) ->
                SaldoBoettePresentasjon(
                    trekkvedtakId = trekkvedtakId.id,
                    opprinneligSaldo = finnOpprinneligSaldo(grunnlag, trekkvedtakId.id),
                    restSaldo = beloep,
                )
            },
        initielleSatsBoetter =
            grunnlag.satsBoetter.innkommendeSatser.map { (id, beloep) ->
                SatsBoettePresentasjon(
                    trekkperiodeId = id.id.id,
                    boette = id.boette,
                    opprinneligSats = finnOpprinneligSats(grunnlag, id.id.id, id.boette),
                    restSats = beloep,
                )
            },
        restFrikortBeloep = grunnlag.restFrikortBeloep,
    )

    companion object {
        private fun finnOpprinneligSaldo(
            grunnlag: Beregningsgrunnlag,
            trekkvedtakId: Long,
        ): BigDecimal? =
            grunnlag.segmenter
                .flatMap { it.delperioder }
                .flatMap { it.trekk }
                .firstOrNull { it.id.id == trekkvedtakId }
                ?.belopSaldo

        private fun finnOpprinneligSats(
            grunnlag: Beregningsgrunnlag,
            trekkperiodeId: Long,
            boette: String,
        ): BigDecimal? =
            grunnlag.segmenter
                .flatMap { it.delperioder }
                .flatMap { it.trekk }
                .firstOrNull { it.trekkperiode.id.id == trekkperiodeId && it.trekkperiode.boetter.contains(boette) }
                ?.trekkperiode
                ?.sats
    }
}

@Serializable
data class SaldoBoettePresentasjon(
    val trekkvedtakId: Long,
    val opprinneligSaldo: BigDecimalJson? = null,
    val restSaldo: BigDecimalJson,
)

@Serializable
data class SatsBoettePresentasjon(
    val trekkperiodeId: Long,
    val boette: String,
    val opprinneligSats: BigDecimalJson? = null,
    val restSats: BigDecimalJson,
)
