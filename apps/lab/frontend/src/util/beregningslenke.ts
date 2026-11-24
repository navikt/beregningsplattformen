import LZString from "lz-string";
import type { Beregningsskjema } from "../types/Beregningsskjema";

const MAX_BASE64_LENGTH = 64 * 1024;
const MAX_JSON_LENGTH = 512 * 1024;

function enkodSkjema(skjema: Beregningsskjema): string {
	const json = JSON.stringify({ versjon: 1, skjema });
	if (json.length > MAX_JSON_LENGTH)
		throw new RangeError("Skjemaet er for stort.");
	const kodet = LZString.compressToBase64(json);
	if (kodet.length > MAX_BASE64_LENGTH)
		throw new RangeError("Lenken er for lang.");
	return kodet;
}

function dekodSkjema(kodet: string): Beregningsskjema | null {
	if (!kodet || kodet.length > MAX_BASE64_LENGTH) {
		return null;
	}
	try {
		const json = LZString.decompressFromBase64(kodet);
		if (!json || json.length > MAX_JSON_LENGTH) return null;
		const lagret: { versjon?: number; skjema?: Beregningsskjema } | null =
			JSON.parse(json, (felt, innhold: unknown) => {
				if (["__proto__", "constructor", "prototype"].includes(felt)) {
					throw new SyntaxError("Ugyldig skjemafelt.");
				}
				return innhold;
			});
		return lagret?.versjon === 1 &&
			lagret.skjema &&
			typeof lagret.skjema === "object" &&
			!Array.isArray(lagret.skjema)
			? lagret.skjema
			: null;
	} catch (feil) {
		if (
			!(
				feil instanceof SyntaxError ||
				feil instanceof TypeError ||
				feil instanceof RangeError
			)
		)
			throw feil;
		return null;
	}
}

export function lesSkjemaFraHash(
	hash: string,
):
	| { status: "tom" }
	| { status: "ugyldig" }
	| { status: "gjenopprettet"; skjema: Beregningsskjema } {
	const verdier = new URLSearchParams(hash.replace(/^#/, "")).getAll("form");
	if (verdier.length === 0) return { status: "tom" };
	if (verdier.length !== 1) return { status: "ugyldig" };
	const skjema = dekodSkjema(verdier[0]);
	return skjema ? { status: "gjenopprettet", skjema } : { status: "ugyldig" };
}

export function hashMedSkjema(
	hash: string,
	skjema: Beregningsskjema | null,
): string {
	const parametre = new URLSearchParams(hash.replace(/^#/, ""));
	if (skjema) {
		parametre.set("form", enkodSkjema(skjema));
	} else {
		parametre.delete("form");
	}
	return parametre.size > 0 ? `#${parametre}` : "";
}
