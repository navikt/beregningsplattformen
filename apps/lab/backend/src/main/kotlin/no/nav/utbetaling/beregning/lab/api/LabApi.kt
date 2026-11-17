package no.nav.utbetaling.beregning.lab.api

import java.math.BigDecimal
import java.time.LocalDate
import java.util.Collections
import java.util.IdentityHashMap
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.sokos.dare.poc.beregning.fordeler.SkatteKontekst
import no.nav.utbetaling.beregning.lab.config.createApiError
import no.nav.utbetaling.beregning.lab.dto.OppdragstidslinjeDTO
import no.nav.utbetaling.beregning.lab.dto.toDTO
import no.nav.utbetaling.beregning.log.Log
import no.nav.utbetaling.beregning.oppdrag.Oppdrag
import no.nav.utbetaling.beregning.oppdrag.Oppdragstidslinje
import no.nav.utbetaling.beregning.tid.Periode
import no.nav.utbetaling.beregningsflyt.Beregningsflyt
import no.nav.utbetaling.beregningsflyt.Beregningsgrunnlag

const val API_V1 = "/api/v1"

fun Route.labApi() {
    route(API_V1) {
        class Oppdragstidslinjer(
            val oppdragstidslinjeDTO: OppdragstidslinjeDTO = OppdragstidslinjeDTO(kjeder = emptyList()),
            val oppdragstidslinje: Oppdragstidslinje = Oppdragstidslinje(),
        )

        fun Throwable.feilmelding(): String {
            val linjer = mutableListOf<String>()
            val sette = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())

            var gjeldende: Throwable? = this
            var nivå = 0
            while (gjeldende != null && sette.add(gjeldende)) {
                gjeldende.message?.takeIf { it.isNotBlank() }?.let { melding ->
                    linjer.add("\t".repeat(nivå) + melding)
                    nivå++
                }
                gjeldende = gjeldende.cause
            }

            return linjer.joinToString("\n")
        }

        suspend fun ApplicationCall.feilResponse(
            throwable: Throwable,
            status: HttpStatusCode,
            innsending: Int,
        ) = respond(
            status = status,
            message =
                createApiError(
                    status = status,
                    message = "Feil på innsending $innsending: ${throwable.feilmelding()}",
                    call = this,
                ),
        )

        post("tolk-oppdrag") {
            val beregningsRequest = call.receive<BeregningRequest>()

            val resultat =
                beregningsRequest.xml.foldIndexed(Oppdragstidslinjer()) { index, sammenslåttOppdragstidslinjer, xml ->
                    val innsending = index + 1

                    val oppdaterOppdragtidslinje =
                        when (val oppdrag = Oppdrag.fraXml(xml, utgangspunkt = sammenslåttOppdragstidslinjer.oppdragstidslinje)) {
                            is Oppdrag.UgyldigKombinasjon -> return@post call.feilResponse(oppdrag.feil, HttpStatusCode.Conflict, innsending)
                            is Oppdrag.UgyldigOppdrag -> return@post call.feilResponse(oppdrag.feil, HttpStatusCode.BadRequest, innsending)
                            is Oppdrag.Utbetaling -> oppdrag.oppdragstidslinje
                            is Oppdrag.Simulering -> oppdrag.oppdragstidslinje
                        }

                    val oppdatertOppdragstidslinjeDTO = sammenslåttOppdragstidslinjer.oppdragstidslinjeDTO + oppdaterOppdragtidslinje.toDTO(innsending)

                    Oppdragstidslinjer(
                        oppdragstidslinjeDTO = oppdatertOppdragstidslinjeDTO,
                        oppdragstidslinje = oppdaterOppdragtidslinje,
                    )
                }
            val oppdragstidslinje = resultat.oppdragstidslinje

            val grunnlag = Beregningsgrunnlag(
                beregningsperiode = Periode.LukketPeriode(beregningsRequest.fom ?: oppdragstidslinje.minOf { it.first.fom }, beregningsRequest.tom ?: LocalDate.now()),
                oppdragstidslinje = oppdragstidslinje,
                skatteKontekst = SkatteKontekst(null, emptyMap()),
                trekkVedtak = emptyList(),
                overføres = LocalDate.now(),
                relaterteBeregninger = emptyList(),
                tilgjengeligFrikortbeløp = BigDecimal.ZERO
            )
            val beregninger = Beregningsflyt().beregn(grunnlag)

            Log.info(
               " Beregninger: $beregninger"
            )

            call.respond(resultat.oppdragstidslinjeDTO)
        }
    }
}
