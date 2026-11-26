import type { FellesOpplysninger } from "../components/FellesBeregningsfelt";
import { standardInnsending } from "../components/innsendinger/fraXml";
import type { InnsendingSkjema } from "../components/innsendinger/skjema";
import { nyttForskuddstrekk, type SkattekortUtkast } from "./Skattekort";
import type { BeregningResponse } from "./BeregningResponse";
import type { TrekkUtkast } from "./Trekk";

export interface LagretResultat {
	beregninger: NonNullable<
		BeregningResponse["beregningstidslinje"]
	>["beregninger"];
	melding: string;
}

export interface Beregningsskjema {
	innsendinger: InnsendingSkjema[];
	fellesOpplysninger: FellesOpplysninger;
	skattekortUtkast: SkattekortUtkast;
	trekkUtkast: TrekkUtkast[];
	lagretResultat?: LagretResultat;
}

export function nyttBeregningsskjema(): Beregningsskjema {
	return {
		innsendinger: [standardInnsending()],
		fellesOpplysninger: {
			fom: "",
			tom: "",
			fomGyldig: true,
			tomGyldig: true,
			beregningsTidspunkt: "",
		},
		skattekortUtkast: {
			aktiv: false,
			inntektsaar: String(new Date().getFullYear()),
			forskuddstrekk: nyttForskuddstrekk(),
		},
		trekkUtkast: [],
	};
}
