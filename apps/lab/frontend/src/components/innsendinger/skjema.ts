import {
	type Attestant,
	type Avvent,
	type Enhet,
	type Grad,
	type InnsendingBuilder,
	InnsendingerBuilder,
	type Kjedeidentifikator,
	LinjeBuilder,
	type LinjenivaaFelter,
	type LinjeTekst,
	type Ompostering,
	type Oppdragsidentifikator,
	type OppdragsnivaaFelter,
	type Refusjonsinfo,
	type Tekst,
} from "./InnsendingerBuilder";

// nokkel er bare for React-lister, og sendes aldri til builderen
export type MedNokkel<T> = T & { nokkel: string };

export type LinjeSkjema = MedNokkel<{
	felter: LinjenivaaFelter & { fom?: string; tom?: string };
	attestanter: MedNokkel<Attestant>[];
	linjeTekster: MedNokkel<LinjeTekst>[];
	grader: MedNokkel<Grad>[];
	refusjonsinfo: Refusjonsinfo;
}>;

export type KjedeSkjema = MedNokkel<{
	fortsetterKjedeId?: number;
	linjer: LinjeSkjema[];
}>;

export type InnsendingSkjema = MedNokkel<{
	fortsetterOppdragId?: number;
	felter: OppdragsnivaaFelter;
	enheter: MedNokkel<Enhet>[];
	avventer: MedNokkel<Avvent>[];
	tekster: MedNokkel<Tekst>[];
	ompostering: Ompostering;
	kjeder: KjedeSkjema[];
}>;

export type InnsendingResultat = {
    oppdragId?: number;
    fagsystemId?: string;
    xml?: string;
    feil?: string;
    kjedeforslag: Kjedeidentifikator[];
    kjedeIder: number[];
};

export type ByggResultat = {
	innsendinger: InnsendingResultat[];
	oppdragsforslag: Oppdragsidentifikator[];
};

export const nyNokkel = (): string => crypto.randomUUID();

export function nyInnsending(): InnsendingSkjema {
	return {
		nokkel: nyNokkel(),
		felter: { datoOppdragGjelderFom: "1970-01-01" },
		enheter: [],
		avventer: [],
		tekster: [],
		ompostering: {},
		kjeder: [],
	};
}

const medNyNokkel = <T extends object>(
	element: MedNokkel<T>,
): MedNokkel<T> => ({
	...structuredClone(element),
	nokkel: nyNokkel(),
});

// Ny innsending med utgangspunkt i forrige innsending på samme oppdrag: alle kjeder fortsettes med kopi av linjene
export function kopierInnsending(
	forrige: InnsendingSkjema,
	oppdragId: number,
	kjedeIder: number[],
): InnsendingSkjema {
	return {
		...medNyNokkel(forrige),
		fortsetterOppdragId: oppdragId,
		enheter: forrige.enheter.map(medNyNokkel),
		avventer: forrige.avventer.map(medNyNokkel),
		tekster: forrige.tekster.map(medNyNokkel),
		kjeder: forrige.kjeder.map((kjede, index) => ({
			nokkel: nyNokkel(),
			fortsetterKjedeId: kjedeIder[index],
			linjer: kjede.linjer.map((linje) => ({
				...medNyNokkel(linje),
				attestanter: linje.attestanter.map(medNyNokkel),
				linjeTekster: linje.linjeTekster.map(medNyNokkel),
				grader: linje.grader.map(medNyNokkel),
			})),
		})),
	};
}

export function nyKjede(fortsetterKjedeId?: number): KjedeSkjema {
	return { nokkel: nyNokkel(), fortsetterKjedeId, linjer: [] };
}

export function nyLinje(): LinjeSkjema {
	return {
		nokkel: nyNokkel(),
		felter: {},
		attestanter: [],
		linjeTekster: [],
		grader: [],
		refusjonsinfo: {},
	};
}

// Tomme tekstfelt skal bety "ikke satt", ellers lager builderen tomme tagger
function rens<T extends object>(verdier: T): Omit<T, "nokkel"> {
	return Object.fromEntries(
		Object.entries(verdier).filter(
			([navn, verdi]) => navn !== "nokkel" && verdi != null && verdi !== "",
		),
	) as Omit<T, "nokkel">;
}

function harVerdi(verdier: object): boolean {
	return Object.keys(rens(verdier)).length > 0;
}

function lagLinje(skjema: LinjeSkjema): LinjeBuilder {
	const { fom, tom, ...felter } = rens(skjema.felter);
	const linje = new LinjeBuilder(fom, tom).med(felter);
	for (const attestant of skjema.attestanter)
		linje.medAttestant(rens(attestant));
	for (const tekst of skjema.linjeTekster) linje.medLinjeTekst(rens(tekst));
	for (const grad of skjema.grader) linje.medGrad(rens(grad));
	if (harVerdi(skjema.refusjonsinfo))
		linje.medRefusjonsinfo(rens(skjema.refusjonsinfo));
	return linje;
}

function fyllInnsending(
	innsending: InnsendingBuilder,
	skjema: InnsendingSkjema,
	kjedeforslag: Kjedeidentifikator[],
): number[] {
	innsending.med(rens(skjema.felter));
	for (const enhet of skjema.enheter) innsending.medEnhet(rens(enhet));
	for (const avvent of skjema.avventer) innsending.medAvvent(rens(avvent));
	for (const tekst of skjema.tekster) innsending.medTekst(rens(tekst));
	if (harVerdi(skjema.ompostering))
		innsending.medOmpostering(rens(skjema.ompostering));

	return skjema.kjeder.map((kjedeSkjema) => {
		const kjede =
			kjedeSkjema.fortsetterKjedeId == null
				? innsending.nyKjede()
				: innsending.fortsettKjede(
						finnKjede(kjedeforslag, kjedeSkjema.fortsetterKjedeId),
					);
		for (const linje of kjedeSkjema.linjer) kjede.medLinje(lagLinje(linje));
		innsending.medKjede(kjede);
		return kjede.kjedeidentifikator.id;
	});
}

function finnKjede(
	kjedeforslag: Kjedeidentifikator[],
	id: number,
): Kjedeidentifikator {
	const kjede = kjedeforslag.find((k) => k.id === id);
	if (kjede == null) {
		throw new Error(
			`Kjede ${id} finnes ikke for dette oppdraget i en tidligere innsending`,
		);
	}
	return kjede;
}

const feilmelding = (feil: unknown): string =>
	feil instanceof Error ? feil.message : String(feil);

// Bygger alle innsendingene i rekkefølge, slik at forslagene for en innsending bare kommer fra de foregående
export function bygg(skjemaer: InnsendingSkjema[]): ByggResultat {
	const innsendinger = new InnsendingerBuilder();
	// Indeksen til innsendingen i innsendinger.build(), eller undefined hvis oppdraget ikke fantes
	const xmlIndekser: (number | undefined)[] = [];
	let antall = 0;

	const resultater = skjemaer.map((skjema): InnsendingResultat => {
		let innsending: InnsendingBuilder;
		try {
			innsending =
				skjema.fortsetterOppdragId == null
					? innsendinger.nyttOppdrag()
					: innsendinger.fortsettOppdrag({ id: skjema.fortsetterOppdragId });
		} catch (feil) {
			xmlIndekser.push(undefined);
			return { kjedeforslag: [], kjedeIder: [], feil: feilmelding(feil)};
		}
		const oppdragId = innsending.oppdragsidentifikator.id;
		const { fagsystemId } = innsending;
		const kjedeforslag = innsendinger.kjeder(innsending.oppdragsidentifikator);
		innsendinger.nyInnsending(innsending);
		xmlIndekser.push(antall++);
		try {
			const kjedeIder = fyllInnsending(innsending, skjema, kjedeforslag);
			return { oppdragId, fagsystemId, kjedeforslag, kjedeIder };
		} catch (feil) {
			return {
				oppdragId,
				fagsystemId,
				kjedeforslag,
				kjedeIder: [],
				feil: feilmelding(feil),
			};
		}
	});

	const xmler = innsendinger.build();
	return {
		innsendinger: resultater.map((resultat, index) => {
			const xmlIndeks = xmlIndekser[index];
			return resultat.feil || xmlIndeks == null
				? resultat
				: { ...resultat, xml: xmler[xmlIndeks] };
		}),
		oppdragsforslag: innsendinger.oppdragidentifikatorer(),
	};
}
