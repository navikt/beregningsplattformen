package no.nav.utbetaling.beregning.lab.beregning

import java.math.BigDecimal
import java.time.LocalDate

import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

import no.nav.sokos.dare.poc.aktoer.AktoerId
import no.nav.sokos.dare.poc.beregning.fordeler.SkatteKontekst
import no.nav.sokos.dare.poc.skattekort.domain.Forskuddstrekk
import no.nav.sokos.dare.poc.skattekort.domain.ForskuddstrekkType
import no.nav.sokos.dare.poc.skattekort.domain.Skattekort
import no.nav.utbetaling.beregning.lab.beregning.Feilmelding.feilResponse
import no.nav.utbetaling.beregning.oppdrag.Oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.Personidentifikator
import no.nav.utbetaling.beregning.tid.Periode
import no.nav.utbetaling.beregningsflyt.Beregningsflyt
import no.nav.utbetaling.beregningsflyt.Beregningsgrunnlag

internal const val API_V1 = "/api/v1"

internal fun Route.beregningApi() {
    route(API_V1) {
        post("beregn") {
            val beregningsRequest = call.receive<BeregningRequest>()
            val skattekort = beregningsRequest.skattekort
            val trekkvedtak = beregningsRequest.trekk.tilTrekkvedtak()

            val (oppdragstidslinjeDTO, oppdragstidslinje) =
                when (val oppdragstidslinjeresultat = Oppdragstidslinjer.fra(beregningsRequest.xml)) {
                    is Oppdragstidslinjeresultat.Ugyldig ->
                        return@post call.feilResponse(oppdragstidslinjeresultat.feil, oppdragstidslinjeresultat.status)
                    is Oppdragstidslinjeresultat.Gyldig -> oppdragstidslinjeresultat.oppdragstidslinjer
                }

            val beregningFom =
                beregningsRequest.fom ?: oppdragstidslinje.firstOrNull()?.first?.fom
                    ?: error("Oppdragstidslinje er tom, da må beregningsperiode settes")
            val beregningTom =
                beregningsRequest.tom ?: oppdragstidslinje
                    .lastOrNull()
                    ?.first
                    ?.let { it as? Periode.LukketPeriode }
                    ?.tom ?: error("Oppdragstidslinje er tom/ åpen ende, da må beregningsperiode settes")

            val grunnlag =
                Beregningsgrunnlag(
                    beregningsperiode = Periode.LukketPeriode(beregningFom, beregningTom),
                    oppdragstidslinje = oppdragstidslinje,
                    skatteKontekst = skattekort?.skatteKontekst() ?: SkatteKontekst(null, emptyMap()),
                    trekkVedtak = trekkvedtak,
                    overføres = LocalDate.now(),
                    relaterteBeregninger = emptyList(),
                    tilgjengeligFrikortbeløp = skattekort?.tilgjengeligFrikortbeloep() ?: BigDecimal.ZERO,
                )

            val (_, beregninger) = Beregningsflyt().beregn(grunnlag)

            call.respond(
                BeregningResponse(
                    oppdragstidslinje = oppdragstidslinjeDTO,
                    beregningstidslinje = BeregningstidslinjeDTO.fra(beregninger),
                ),
            )
        }
    }
}

private fun SkattekortDTO.skatteKontekst(): SkatteKontekst =
    SkatteKontekst(
        skattekort =
            Skattekort(
                aktoerId = AktoerId(0L),
                inntektsaar = inntektsaar,
                kilde = "SYNTETISK",
                identifikator = null,
                utstedtDato = null,
                forskuddsTrekk =
                    forskuddstrekk.map {
                        Forskuddstrekk(
                            typeForskuddsTrekk = it.type,
                            trekkKode = it.trekkode,
                            trekktabell = it.tabellnummer,
                            prosentsats = it.prosentsats,
                            frikortBelop = it.frikortbeloep,
                        )
                    },
            ),
        allokeringer =
            forskuddstrekk
                .mapNotNull { trekk ->
                    trekk.faggruppe?.let { trekk.trekkode to it }
                }.toMap(),
    )

// Motoren har én saldo for hele beregningen, ikke én saldo per forskuddstrekk.
private fun SkattekortDTO.tilgjengeligFrikortbeloep(): BigDecimal =
    forskuddstrekk
        .singleOrNull { it.type == ForskuddstrekkType.FRIKORT }
        ?.frikortbeloep
        ?.toBigDecimal() ?: BigDecimal.ZERO

private fun Oppdragstidslinje.skattekortGjelder(): Personidentifikator? {
    val gjelder = flatMap { (_, opplysninger) -> opplysninger.values.map { it.gjelder?.opplysning } }.distinct()
    return (gjelder.singleOrNull() as? Personidentifikator)?.takeIf { it.identifikator.toLongOrNull() != null }
}
