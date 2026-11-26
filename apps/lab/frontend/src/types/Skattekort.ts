import { z } from "zod";

export const trekkoder = {
	LOENN_FRA_HOVEDARBEIDSGIVER: "Lønn fra hovedarbeidsgiver",
	LOENN_FRA_BIARBEIDSGIVER: "Lønn fra biarbeidsgiver",
	LOENN_FRA_NAV: "Lønn fra Nav",
	PENSJON: "Pensjon",
	PENSJON_FRA_NAV: "Pensjon fra Nav",
	LOENN_TIL_UTENRIKSTJENESTEMANN: "Lønn til utenrikstjenestemann",
	LOENN_KUN_TRYGDEAVGIFT_TIL_UTENLANDSK_BORGER:
		"Lønn, kun trygdeavgift til utenlandsk borger",
	LOENN_KUN_TRYGDEAVGIFT_TIL_UTENLANDSK_BORGER_SOM_GRENSEGJENGER:
		"Lønn, kun trygdeavgift til utenlandsk borger som grensegjenger",
	UFOERETRYGD_FRA_NAV: "Uføretrygd fra Nav",
	UFOEREYTELSER_FRA_ANDRE: "Uføreytelser fra andre",
	INTRODUKSJONSSTOENAD: "Introduksjonsstønad",
} as const;

const faggrupper = [
	"ARBYT",
	"KORTTID",
	"ARBTIL",
	"BA",
	"BARNBRIL",
	"EFOG",
	"EFVM",
	"GH",
	"GS",
	"HELSEREF",
	"INNT",
	"IT26",
	"IT27",
	"KORONA",
	"KORONA3",
	"KREDDISP",
	"KREDREF",
	"KS",
	"KTPOST",
	"MSKATT",
	"NEGKRED",
	"NEGSKATT",
	"OMSTILL",
	"PEN",
	"PENPOST",
	"PREDATOR",
	"REFARBG",
	"REFUTG",
	"SKATOPPG",
	"SRNODU",
	"SRPOST",
	"SVALSKAT",
	"TB",
	"TBBT",
	"TBKRAV",
	"TILLST",
	"TREKKOPP",
	"UNG",
	"UTPOST",
	"YS",
] as const;

// Hold listen i samsvar med Tabellnummer i backendens trekkrutine-2026.
export const tabellnumre = [
	"0100",
	"0101",
	"6300",
	"6350",
	"6500",
	"6550",
	"6700",
	"7150",
	"7160",
	"7170",
	"7300",
	"7350",
	"7500",
	"7550",
	"7700",
	"8000",
	"8010",
	"8020",
	"8030",
	"8040",
	"8050",
	"8060",
	"8070",
	"8080",
	"8090",
	"8100",
	"8110",
	"8120",
	"8130",
	"8140",
	"8150",
	"8160",
	"8170",
	"8180",
	"8190",
	"8200",
	"8210",
	"8220",
	"8230",
	"8240",
	"8250",
	"8260",
	"8270",
	"8280",
	"8290",
	"8300",
	"8310",
	"8320",
	"8330",
	"8340",
	"8350",
	"8360",
	"8370",
	"8380",
	"8390",
	"8400",
	"9010",
	"9020",
	"9030",
	"9040",
	"9050",
	"9060",
	"9070",
	"9080",
	"9090",
	"9100",
	"9110",
	"9120",
	"9130",
	"9140",
	"9150",
	"9160",
	"9170",
	"9180",
	"9190",
	"9200",
	"9210",
	"9220",
	"9230",
	"9240",
	"9250",
	"9260",
	"9270",
	"9280",
	"9290",
	"9300",
	"9310",
	"9320",
	"9330",
	"9340",
	"9350",
	"9360",
	"9370",
	"9380",
	"9390",
	"9400",
] as const;
export const standardTabellnummer = "8010";

export function trekkodeForFaggruppe(faggruppe: string): string {
	if (["EFVM", "GH", "IT27", "PEN", "YS"].includes(faggruppe)) {
		return "PENSJON_FRA_NAV";
	}
	if (["INNT", "IT26", "UTPOST"].includes(faggruppe)) {
		return "UFOERETRYGD_FRA_NAV";
	}
	return "LOENN_FRA_NAV";
}

export function faggrupperForTrekkode(trekkode: string) {
	return faggrupper.filter((gruppe) => trekkodeForFaggruppe(gruppe) === trekkode);
}

const prosent = z
	.number({ error: "Oppgi en prosentsats mellom 0 og 100." })
	.min(0, "Prosentsatsen kan ikke være negativ.")
	.max(100, "Prosentsatsen kan ikke være over 100.")
	.refine(
		(verdi) => Math.abs(verdi * 100 - Math.round(verdi * 100)) < 0.000001,
		"Bruk høyst to desimaler.",
	);
const trekkode = z.custom<keyof typeof trekkoder>(
	(verdi) => typeof verdi === "string" && Object.hasOwn(trekkoder, verdi),
	{ message: "Velg en trekkode." },
);

const forskuddstrekkSchema = z.discriminatedUnion("type", [
	z.object({
		type: z.literal("PROSENTKORT"),
		trekkode,
		prosentsats: prosent,
	}),
	z.object({
		type: z.literal("TABELLKORT"),
		trekkode,
		prosentsats: prosent,
		tabellnummer: z.enum(tabellnumre, { error: "Velg et tabellnummer." }),
		faggruppe: z.enum(faggrupper, {
			error: "Velg faggruppen som skal bruke tabelltrekk.",
		}),
	}),
	z.object({
		type: z.literal("FRIKORT"),
		trekkode,
		frikortbeloep: z
			.number({ error: "Oppgi frikortbeløpet i hele kroner." })
			.int("Oppgi frikortbeløpet i hele kroner.")
			.min(0, "Frikortbeløpet kan ikke være negativt.")
			.max(2147483647, "Frikortbeløpet kan ikke være over 2147483647 kroner."),
	}),
]);

export const skattekortSchema = z
	.object({
		inntektsaar: z
			.number({ error: "Oppgi et inntektsår mellom 1900 og 2100." })
			.int("Oppgi inntektsåret med fire sifre.")
			.min(1900, "Inntektsåret må være minst 1900.")
			.max(2100, "Inntektsåret kan ikke være etter 2100."),
		forskuddstrekk: z.tuple([forskuddstrekkSchema]),
	})
	.superRefine((kort, ctx) => {
		const trekk = kort.forskuddstrekk[0];
		if (
			trekk.type === "TABELLKORT" &&
			trekkodeForFaggruppe(trekk.faggruppe) !== trekk.trekkode
		) {
			ctx.addIssue({
				code: "custom",
				path: ["forskuddstrekk", 0, "faggruppe"],
				message: "Faggruppen må tilhøre trekkoden.",
			});
		}
	});

export type Skattekort = z.infer<typeof skattekortSchema>;
export interface ForskuddstrekkUtkast {
	type: string;
	trekkode: string;
	prosentsats: string;
	tabellnummer: string;
	frikortbeloep: string;
	faggruppe: string;
}
export interface SkattekortUtkast {
	aktiv: boolean;
	inntektsaar: string;
	forskuddstrekk: ForskuddstrekkUtkast;
}

export function nyttForskuddstrekk(): ForskuddstrekkUtkast {
	return {
		type: "PROSENTKORT",
		trekkode: "LOENN_FRA_NAV",
		prosentsats: "30",
		tabellnummer: standardTabellnummer,
		frikortbeloep: "",
		faggruppe: "ARBYT",
	};
}

function lesTall(verdi: string, desimaler = false) {
	const normalisert = verdi.trim().replace(",", ".");
	return (desimaler ? /^\d+(?:\.\d{1,2})?$/ : /^\d+$/).test(normalisert)
		? Number(normalisert)
		: Number.NaN;
}

export function lesSkattekort(utkast: SkattekortUtkast) {
	const trekk = utkast.forskuddstrekk;
	return skattekortSchema.parse({
		inntektsaar: lesTall(utkast.inntektsaar),
		forskuddstrekk: [
			{
				type: trekk.type,
				trekkode: trekk.trekkode,
				...(trekk.type === "FRIKORT"
					? { frikortbeloep: lesTall(trekk.frikortbeloep) }
					: { prosentsats: lesTall(trekk.prosentsats, true) }),
				...(trekk.type === "TABELLKORT"
					? { tabellnummer: trekk.tabellnummer.trim(), faggruppe: trekk.faggruppe }
					: {}),
			},
		],
	});
}
