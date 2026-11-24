import type { FellesOpplysninger } from "../components/FellesBeregningsfelt";
import { standardInnsending } from "../components/innsendinger/fraXml";
import type { InnsendingSkjema } from "../components/innsendinger/skjema";
import { nyttForskuddstrekk, type SkattekortUtkast } from "./Skattekort";
import type { TrekkUtkast } from "./Trekk";

export interface Beregningsskjema {
	innsendinger: InnsendingSkjema[];
	fellesOpplysninger: FellesOpplysninger;
	skattekortUtkast: SkattekortUtkast;
	trekkUtkast: TrekkUtkast[];
}

export function nyttBeregningsskjema(): Beregningsskjema {
	return {
		innsendinger: [standardInnsending()],
		fellesOpplysninger: { fom: "", tom: "", fomGyldig: true, tomGyldig: true },
		skattekortUtkast: {
			aktiv: false,
			inntektsaar: String(new Date().getFullYear()),
			forskuddstrekk: nyttForskuddstrekk(),
		},
		trekkUtkast: [],
	};
}
