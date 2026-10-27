package no.nav.sokos.dare.poc.beregning.dto

import java.math.BigDecimal

import kotlinx.serialization.Serializable

import no.nav.sokos.dare.poc.BigDecimalJson
import no.nav.sokos.dare.poc.beregning.Beregning
import no.nav.sokos.dare.poc.beregning.DatoIntervall
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
        periode = kontekst.periode,
        beregninger =
            if (beregninger == null) {
                kontekst.beregninger().map(::BeregningsPresentasjonV2)
            } else {
                beregninger.map(::BeregningsPresentasjonV2)
            },
        saldoBoetter =
            kontekst.saldoBoetter.saldoer.map { (trekkvedtakId, beloep) ->
                SaldoBoettePresentasjon(
                    trekkvedtakId = trekkvedtakId.id,
                    opprinneligSaldo = finnOpprinneligSaldo(kontekst, trekkvedtakId.id),
                    restSaldo = beloep,
                )
            },
        satsBoetter =
            kontekst.satsBoetter.satser.map { (id, beloep) ->
                SatsBoettePresentasjon(
                    trekkperiodeId = id.id.id,
                    boette = id.boette,
                    opprinneligSats = finnOpprinneligSats(kontekst, id.id.id, id.boette),
                    restSats = beloep,
                )
            },
        initielleSaldoBoetter =
            kontekst.saldoBoetter.innkommendeSaldoer.map { (trekkvedtakId, beloep) ->
                SaldoBoettePresentasjon(
                    trekkvedtakId = trekkvedtakId.id,
                    opprinneligSaldo = finnOpprinneligSaldo(kontekst, trekkvedtakId.id),
                    restSaldo = beloep,
                )
            },
        initielleSatsBoetter =
            kontekst.satsBoetter.innkommendeSatser.map { (id, beloep) ->
                SatsBoettePresentasjon(
                    trekkperiodeId = id.id.id,
                    boette = id.boette,
                    opprinneligSats = finnOpprinneligSats(kontekst, id.id.id, id.boette),
                    restSats = beloep,
                )
            },
        restFrikortBeloep = kontekst.restFrikortBeloep,
    )

    companion object {
        private fun finnOpprinneligSaldo(
            kontekst: BeregningsmotorKontekst,
            trekkvedtakId: Long,
        ): BigDecimal? =
            kontekst.segmenter
                .flatMap { it.delperioder }
                .flatMap { it.trekk }
                .firstOrNull { it.id.id == trekkvedtakId }
                ?.belopSaldo

        private fun finnOpprinneligSats(
            kontekst: BeregningsmotorKontekst,
            trekkperiodeId: Long,
            boette: String,
        ): BigDecimal? =
            kontekst.segmenter
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
