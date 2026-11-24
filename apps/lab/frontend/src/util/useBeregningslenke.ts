import {
	type SetStateAction,
	useCallback,
	useEffect,
	useRef,
	useState,
} from "react";
import {
	type Beregningsskjema,
	nyttBeregningsskjema,
} from "../types/Beregningsskjema";
import { hashMedSkjema, lesSkjemaFraHash } from "./beregningslenke";

export function useBeregningslenke() {
	const startUrl = useRef(new URL(window.location.href));
	const sistLesteHash = useRef(startUrl.current.hash);
	const [tilstand, settTilstand] = useState(() => {
		const lest = lesSkjemaFraHash(startUrl.current.hash);
		return {
			skjema:
				lest.status === "gjenopprettet" ? lest.skjema : nyttBeregningsskjema(),
			nullstilt: lest.status === "ugyldig",
			lagringsfeil: false,
			gjenoppretting: 0,
		};
	});
	const gjeldendeSkjema = useRef(tilstand.skjema);

	const lagre = useCallback((skjema: Beregningsskjema | null) => {
		const url = new URL(window.location.href);
		if (
			url.origin !== startUrl.current.origin ||
			url.pathname !== startUrl.current.pathname
		)
			return false;
		try {
			url.hash = hashMedSkjema(url.hash, skjema);
			if (url.href !== window.location.href) {
				window.history.replaceState(window.history.state, "", url);
			}
			sistLesteHash.current = url.hash;
			return false;
		} catch (feil) {
			if (
				!(feil instanceof RangeError) &&
				!(feil instanceof DOMException && feil.name === "SecurityError")
			)
				throw feil;
			return true;
		}
	}, []);

	const settSkjema = useCallback(
		(oppdater: SetStateAction<Beregningsskjema>) => {
			const skjema =
				typeof oppdater === "function"
					? oppdater(gjeldendeSkjema.current)
					: oppdater;
			gjeldendeSkjema.current = skjema;
			const lagringsfeil = lagre(skjema);
			settTilstand((forrige) => ({ ...forrige, skjema, lagringsfeil }));
		},
		[lagre],
	);

	useEffect(() => {
		const gjenopprett = () => {
			const url = new URL(window.location.href);
			if (
				url.origin !== startUrl.current.origin ||
				url.pathname !== startUrl.current.pathname ||
				url.hash === sistLesteHash.current
			)
				return;
			const lest = lesSkjemaFraHash(url.hash);
			if (lest.status === "tom") {
				lagre(gjeldendeSkjema.current);
				return;
			}
			const skjema =
				lest.status === "gjenopprettet" ? lest.skjema : nyttBeregningsskjema();
			gjeldendeSkjema.current = skjema;
			const lagringsfeil = lagre(skjema);
			sistLesteHash.current = new URL(window.location.href).hash;
			settTilstand((forrige) => ({
				skjema,
				lagringsfeil,
				nullstilt: lest.status === "ugyldig",
				gjenoppretting: forrige.gjenoppretting + 1,
			}));
		};
		if (new URLSearchParams(startUrl.current.hash.slice(1)).has("form")) {
			const lagringsfeil = lagre(gjeldendeSkjema.current);
			settTilstand((forrige) =>
				forrige.lagringsfeil === lagringsfeil
					? forrige
					: { ...forrige, lagringsfeil },
			);
		}
		window.addEventListener("popstate", gjenopprett);
		window.addEventListener("hashchange", gjenopprett);
		return () => {
			window.removeEventListener("popstate", gjenopprett);
			window.removeEventListener("hashchange", gjenopprett);
		};
	}, [lagre]);

	return {
		...tilstand,
		settSkjema,
		nullstillSkjema: () => {
			const skjema = nyttBeregningsskjema();
			gjeldendeSkjema.current = skjema;
			const lagringsfeil = lagre(null);
			settTilstand((forrige) => ({
				skjema,
				nullstilt: false,
				lagringsfeil,
				gjenoppretting: forrige.gjenoppretting + 1,
			}));
		},
		bekreftNullstilling: () =>
			settTilstand((forrige) => ({ ...forrige, nullstilt: false })),
	};
}
