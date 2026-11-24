import { z } from "zod";

const datoformat = "dd.mm.åååå";

const gyldigDato = (felt: "fra-dato" | "til-dato") =>
	z.iso.date({
		error: `Oppgi en gyldig ${felt} i formatet ${datoformat}.`,
	});

const beloep = (feilmelding: string) =>
	z.number({ error: feilmelding }).min(0, feilmelding);

const erDato = (verdi: string) => z.iso.date().safeParse(verdi).success;

function formaterDato(iso: string) {
	const [aar, maaned, dag] = iso.split("-");
	return `${dag}.${maaned}.${aar}`;
}

export const trekkPeriodeSchema = z.object({
	sats: beloep("Oppgi satsen som et tall på 0 eller mer."),
	satsType: z.enum(["D", "M"], {
		error: "Velg satstype: dagsats (D) eller månedssats (M).",
	}),
	fom: gyldigDato("fra-dato"),
	tom: gyldigDato("til-dato"),
});

export const trekkSchema = z
	.object({
		trekkType: z.string().trim().min(1, "Oppgi trekktypekode."),
		beregningsType: z.enum(["YTELPER", "UTBDATO"], {
			error: "Velg beregningstype.",
		}),
		skatteReduserende: z.boolean({
			error: "Velg om trekket reduserer skatt.",
		}),
		saldo: beloep(
			"Oppgi saldo som et beløp på 0 eller mer, eller la feltet stå tomt.",
		).nullable(),
		gyldigTom: z.iso
			.date({
				error: `Oppgi en gyldig dato for «Gyldig til» i formatet ${datoformat}, eller la feltet stå tomt.`,
			})
			.nullable(),
		trekkPerioder: z
			.array(trekkPeriodeSchema)
			.min(1, "Legg til minst én periode."),
	})
	.superRefine((trekk, ctx) => {
		// Sammenligning av ISO-datoer er bare trygg når begge datoene er gyldige.
		const medGyldigeDatoer = trekk.trekkPerioder
			.map((periode, indeks) => ({ ...periode, indeks }))
			.filter((periode) => erDato(periode.fom) && erDato(periode.tom));
		const perioder = medGyldigeDatoer.filter(
			(periode) => periode.fom <= periode.tom,
		);
		const gyldigTom =
			trekk.gyldigTom && erDato(trekk.gyldigTom) ? trekk.gyldigTom : null;

		for (const periode of medGyldigeDatoer) {
			if (periode.fom > periode.tom) {
				ctx.addIssue({
					code: "custom",
					path: ["trekkPerioder", periode.indeks, "tom"],
					message: "Til-datoen må være lik eller etter fra-datoen.",
				});
			}
		}

		for (const periode of perioder) {
			if (gyldigTom && periode.tom > gyldigTom) {
				ctx.addIssue({
					code: "custom",
					path: ["trekkPerioder", periode.indeks, "tom"],
					message: `Til-datoen kan ikke være etter datoen i «Gyldig til» for trekket (${formaterDato(gyldigTom)}).`,
				});
			}
		}

		// Inklusive datoer: en periode som starter samme dag som en annen slutter, overlapper.
		let senest: (typeof perioder)[number] | undefined;
		for (const periode of [...perioder].sort((a, b) =>
			a.fom.localeCompare(b.fom),
		)) {
			if (senest && periode.fom <= senest.tom) {
				ctx.addIssue({
					code: "custom",
					path: ["trekkPerioder", periode.indeks, "fom"],
					message: `Perioden overlapper periode ${senest.indeks + 1} i samme trekk.`,
				});
			}
			if (!senest || periode.tom > senest.tom) senest = periode;
		}
	});

export type TrekkDTO = z.infer<typeof trekkSchema>;

const nyId = () => crypto.randomUUID();

export interface TrekkPeriodeUtkast {
	id: string;
	sats: string;
	satsType: string;
	fom: string;
	fomGyldig: boolean;
	tom: string;
	tomGyldig: boolean;
}

export interface TrekkUtkast {
	id: string;
	trekkType: string;
	beregningsType: string;
	skatteReduserende: boolean;
	saldo: string;
	gyldigTom: string;
	gyldigTomGyldig: boolean;
	trekkPerioder: TrekkPeriodeUtkast[];
}

export function nyPeriode(): TrekkPeriodeUtkast {
	return {
		id: nyId(),
		sats: "",
		satsType: "D",
		fom: "",
		fomGyldig: true,
		tom: "",
		tomGyldig: true,
	};
}

export function nyttTrekk(): TrekkUtkast {
	return {
		id: nyId(),
		trekkType: "",
		beregningsType: "YTELPER",
		skatteReduserende: false,
		saldo: "",
		gyldigTom: "",
		gyldigTomGyldig: true,
		trekkPerioder: [nyPeriode()],
	};
}

export interface TrekkFeil {
	/** Id til feltet i skjemaet, brukes som lenke fra feiloppsummeringen. */
	felt: string;
	melding: string;
}

export type TrekkResultat =
	| { ok: true; trekk: TrekkDTO[] }
	| { ok: false; feil: TrekkFeil[] };

export const trekkTrekkFeltId = (trekkId: string, felt: string) =>
	`trekk-${trekkId}-${felt}`;
export const trekkPeriodeFeltId = (
	trekkId: string,
	periodeId: string,
	felt: string,
) => `trekk-${trekkId}-periode-${periodeId}-${felt}`;

function lesBeloep(verdi: string) {
	const normalisert = verdi.trim().replace(",", ".");
	return /^-?\d+(?:\.\d+)?$/.test(normalisert)
		? Number(normalisert)
		: Number.NaN;
}

function lesDato(verdi: string, gyldig: boolean) {
	return gyldig ? verdi : "ugyldig";
}

export function lesTrekk(utkast: TrekkUtkast[]): TrekkResultat {
	const feil: TrekkFeil[] = [];
	const trekk: TrekkDTO[] = [];

	utkast.forEach((enkeltTrekk, trekkIndeks) => {
		const resultat = trekkSchema.safeParse({
			trekkType: enkeltTrekk.trekkType,
			beregningsType: enkeltTrekk.beregningsType,
			skatteReduserende: enkeltTrekk.skatteReduserende,
			saldo:
				enkeltTrekk.saldo.trim() === "" ? null : lesBeloep(enkeltTrekk.saldo),
			gyldigTom:
				enkeltTrekk.gyldigTom === "" && enkeltTrekk.gyldigTomGyldig
					? null
					: lesDato(enkeltTrekk.gyldigTom, enkeltTrekk.gyldigTomGyldig),
			trekkPerioder: enkeltTrekk.trekkPerioder.map((periode) => ({
				sats: lesBeloep(periode.sats),
				satsType: periode.satsType,
				fom: lesDato(periode.fom, periode.fomGyldig),
				tom: lesDato(periode.tom, periode.tomGyldig),
			})),
		});
		if (resultat.success) {
			trekk.push(resultat.data);
			return;
		}
		for (const issue of resultat.error.issues) {
			const [forste, periodeIndeks, felt] = issue.path;
			const periode =
				forste === "trekkPerioder" && typeof periodeIndeks === "number"
					? enkeltTrekk.trekkPerioder[periodeIndeks]
					: undefined;
			const prefiks = periode
				? `Trekk ${trekkIndeks + 1}, periode ${Number(periodeIndeks) + 1}: `
				: `Trekk ${trekkIndeks + 1}: `;
			feil.push({
				felt: periode
					? trekkPeriodeFeltId(enkeltTrekk.id, periode.id, String(felt))
					: trekkTrekkFeltId(
							enkeltTrekk.id,
							forste === "trekkPerioder" ? "perioder" : String(forste),
						),
				melding: prefiks + issue.message,
			});
		}
	});

	return feil.length > 0 ? { ok: false, feil } : { ok: true, trekk };
}
