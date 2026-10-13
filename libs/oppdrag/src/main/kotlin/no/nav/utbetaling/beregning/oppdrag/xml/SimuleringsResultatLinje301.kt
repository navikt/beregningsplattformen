package no.nav.utbetaling.beregning.oppdrag.xml

import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate

internal data class SimuleringsResultatLinje301(
    val fagsystemId: String,
    val oppdragGjelderId: String,
    val bostedsEnhet: String,
    val utbetFrekvens: String,
    val vedtakId: String,
    val delytelseId: String,
    val utbetalesTilId: String,
    val gironr: String? = null,
    val behandlEnhet: String,
    val datoKjores: LocalDate,
    val datoBeregnFom: LocalDate,
    val datoBeregnTom: LocalDate,
    val kontostreng: String,
    val kodeBeh: String,
    val kontotekst: String,
    val belop: BigDecimal,
    val fradragTillegg: TfradragTillegg,
    val grad: BigInteger? = null,
    val saksbehId: String,
)
