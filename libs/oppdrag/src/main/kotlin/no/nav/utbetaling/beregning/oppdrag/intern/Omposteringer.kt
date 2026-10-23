package no.nav.utbetaling.beregning.oppdrag.intern

import java.time.LocalDate.EPOCH

import no.nav.utbetaling.beregning.oppdrag.Ompostering
import no.nav.utbetaling.beregning.oppdrag.Opplysning
import no.nav.utbetaling.beregning.oppdrag.SaksbehandlerId
import no.nav.utbetaling.beregning.oppdrag.xml.Oppdrag110
import no.nav.utbetaling.beregning.tid.Periode.ÅpenPeriode
import no.nav.utbetaling.beregning.tid.Tidslinje

internal object Omposteringer {
    fun fra(oppdrag110: Oppdrag110): Tidslinje<FeilregistrerbarOpplysning<Ompostering>> {
        val builder = Tidslinje.Builder<FeilregistrerbarOpplysning<Ompostering>>()
        val ompostering116 = oppdrag110.ompostering116 ?: return builder.build()
        // TODO: Å sette n blir det samme som å ikke opplyse om noe..? Om man skal fjerne noe må man bruke feilreg: j, ikke omPosering n ?
        if (ompostering116.omPostering.lowercase() == "n") return builder.build()
        check(ompostering116.omPostering.lowercase() == "j") { "Ukjent verdi i ompostering-116.omPostering ${ompostering116.omPostering}" }

        val ompostering =
            Ompostering(
                registrert = Tidspunkt.fra(ompostering116.tidspktReg),
            )
        val saksbehandlerId = ompostering116.saksbehId.mapVerdi { SaksbehandlerId(it) } ?: error("Mangler saksbehId på ompostering")
        return Tidslinje
            .Builder<FeilregistrerbarOpplysning<Ompostering>>()
            .periode(
                ÅpenPeriode(ompostering116.datoOmposterFom ?: EPOCH),
                FeilregistrerbarOpplysning.fra(ompostering116.feilreg, Opplysning(saksbehandlerId, ompostering)),
            ).build()
    }
}
