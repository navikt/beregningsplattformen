package no.nav.utbetaling.beregning.oppdrag.intern

import java.time.LocalDate.EPOCH

import no.nav.utbetaling.beregning.oppdrag.Ompostering
import no.nav.utbetaling.beregning.oppdrag.Omposteringskode
import no.nav.utbetaling.beregning.oppdrag.Opplysning
import no.nav.utbetaling.beregning.oppdrag.SaksbehandlerId
import no.nav.utbetaling.beregning.oppdrag.xml.Oppdrag110
import no.nav.utbetaling.beregning.tid.Periode.ÅpenPeriode
import no.nav.utbetaling.beregning.tid.Tidslinje

internal object Omposteringer {
    fun fra(oppdrag110: Oppdrag110): Tidslinje<Endring<Ompostering>> {
        val ompostering116 = oppdrag110.ompostering116 ?: return Endring.Companion.ingenEndringer()
        val ompostering =
            Ompostering(
                kode = Omposteringskode(ompostering116.omPostering),
                registrert = Tidspunkt.fra(ompostering116.tidspktReg),
            )
        val saksbehandlerId = ompostering116.saksbehId.mapVerdi { SaksbehandlerId(it) } ?: error("Mangler saksbehId på ompostering")
        return Tidslinje
            .Builder<Endring<Ompostering>>()
            .periode(
                ÅpenPeriode(ompostering116.datoOmposterFom ?: EPOCH),
                Endring.fra(ompostering116.feilreg, Opplysning(saksbehandlerId, ompostering)),
            ).build()
    }
}
