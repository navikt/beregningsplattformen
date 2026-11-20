function escapeXml(verdi: string): string {
	return verdi
		.replace(/&/g, "&amp;")
		.replace(/</g, "&lt;")
		.replace(/>/g, "&gt;")
		.replace(/"/g, "&quot;")
		.replace(/'/g, "&apos;");
}

function valgfriTag(tag: string, verdi: string | null | undefined): string {
	return verdi != null ? `<${tag}>${escapeXml(verdi)}</${tag}>` : "";
}

function tolkDato(verdi: string): Date | undefined {
	const millis = Date.parse(verdi);
	return Number.isNaN(millis) ? undefined : new Date(millis);
}

function xmlDato(verdi: string | undefined): string | undefined {
	if (verdi == null) return undefined;
	const dato = tolkDato(verdi);
	if (dato == null) return verdi;
	const år = dato.getFullYear();
	const måned = String(dato.getMonth() + 1).padStart(2, "0");
	const dag = String(dato.getDate()).padStart(2, "0");
	return `${år}-${måned}-${dag}+01:00`;
}

function xmlTidspunkt(verdi: string | undefined): string | undefined {
	if (verdi == null) return undefined;
	const tidspunkt = tolkDato(verdi);
	if (tidspunkt == null) return verdi;
	const to = (tall: number) => String(tall).padStart(2, "0");
	const dato = `${tidspunkt.getFullYear()}-${to(tidspunkt.getMonth() + 1)}-${to(tidspunkt.getDate())}`;
	const klokke = `${to(tidspunkt.getHours())}.${to(tidspunkt.getMinutes())}.${to(tidspunkt.getSeconds())}`;
	const mikrosekunder = String(tidspunkt.getMilliseconds() * 1000).padStart(
		6,
		"0",
	);
	return `${dato}-${klokke}.${mikrosekunder}`;
}

function listeXml<T>(elementer: T[], tilXml: (element: T) => string): string {
	return elementer.map((element) => enLinje(tilXml(element))).join("");
}

// Fjerner linjeskift og innrykk fra malene. Formatering for visning gjøres i GUI-et.
function enLinje(xml: string): string {
	return xml
		.split("\n")
		.map((linje) => linje.trim())
		.join("");
}

export type Oppdragsidentifikator = {
	readonly id: number;
};

function fagsystemId(oppdrag: Oppdragsidentifikator): string {
	return `FAGID-${oppdrag.id}`;
}

function sammeOppdrag(
	a: Oppdragsidentifikator,
	b: Oppdragsidentifikator,
): boolean {
	return a.id === b.id;
}

export type Kjedeidentifikator = {
	readonly id: number;
	readonly oppdragsidentifikator: Oppdragsidentifikator;
};

export type LinjenivaaFelter = {
	kodeEndringLinje?: string;
	kodeStatusLinje?: string;
	datoStatusFom?: string;
	vedtakId?: string;
	sats?: string;
	typeSats?: string;
	kodeKlassifik?: string;
	fradragTillegg?: string;
	skyldnerId?: string;
	datoSkyldnerFom?: string;
	kid?: string;
	datoKidFom?: string;
	brukKjoreplan?: string;
	saksbehId?: string;
	utbetalesTilId?: string;
	datoUtbetalesTilIdFom?: string;
	henvisning?: string;
	typeSoknad?: string;
	refFagsystemId?: string;
	vedtakssats?: string;
};

export type Attestant = {
	attestantId?: string;
	datoUgyldigFom?: string;
};

export type Refusjonsinfo = {
	maksDato?: string;
	refunderesId?: string;
	datoFom?: string;
};

export type LinjeTekst = {
	tekstLnr?: string;
	tekstKode?: string;
	tekst?: string;
	datoTekstFom?: string;
	datoTekstTom?: string;
	feilreg?: string;
};

export type Grad = {
	typeGrad?: string;
	grad?: string;
};

export class LinjeBuilder {
	private readonly felter: LinjenivaaFelter = {};
	private readonly attestanter: Attestant[] = [];
	private readonly linjeTekster: LinjeTekst[] = [];
	private readonly grader: Grad[] = [];
	private refusjonsinfo?: Refusjonsinfo;

	constructor(
		readonly fom?: string,
		readonly tom?: string,
	) {}

	med(felter: Partial<LinjenivaaFelter>): LinjeBuilder {
		Object.assign(this.felter, felter);
		return this;
	}

	medAttestant(attestant: Attestant): LinjeBuilder {
		this.attestanter.push(attestant);
		return this;
	}

	medRefusjonsinfo(refusjonsinfo: Refusjonsinfo): LinjeBuilder {
		this.refusjonsinfo = refusjonsinfo;
		return this;
	}

	medLinjeTekst(linjeTekst: LinjeTekst): LinjeBuilder {
		this.linjeTekster.push(linjeTekst);
		return this;
	}

	medGrad(grad: Grad): LinjeBuilder {
		this.grader.push(grad);
		return this;
	}

	private refusjonsinfoXml(): string {
		if (this.refusjonsinfo == null) return "";
		return enLinje(`
        <refusjonsinfo-156>
            ${valgfriTag("maksDato", xmlDato(this.refusjonsinfo.maksDato))}
            ${valgfriTag("refunderesId", this.refusjonsinfo.refunderesId)}
            ${valgfriTag("datoFom", xmlDato(this.refusjonsinfo.datoFom))}
        </refusjonsinfo-156>
        `);
	}

	private linjeTeksterXml(): string {
		return listeXml(
			this.linjeTekster,
			(linjeTekst) => `
            <linje-tekst-158>
                ${valgfriTag("tekstLnr", linjeTekst.tekstLnr)}
                ${valgfriTag("tekstKode", linjeTekst.tekstKode)}
                ${valgfriTag("tekst", linjeTekst.tekst)}
                ${valgfriTag("datoTekstFom", xmlDato(linjeTekst.datoTekstFom))}
                ${valgfriTag("datoTekstTom", xmlDato(linjeTekst.datoTekstTom))}
                ${valgfriTag("feilreg", linjeTekst.feilreg)}
            </linje-tekst-158>
        `,
		);
	}

	private graderXml(): string {
		return listeXml(
			this.grader.filter((grad) =>
				[grad.typeGrad, grad.grad].some((v) => v != null),
			),
			(grad) => `
            <grad-170>
                ${valgfriTag("typeGrad", grad.typeGrad)}
                ${valgfriTag("grad", grad.grad)}
            </grad-170>
        `,
		);
	}

	private vedtakssatsXml(): string {
		if (this.felter.vedtakssats == null) return "";
		return enLinje(`
        <vedtakssats-157>
            <vedtakssats>${escapeXml(this.felter.vedtakssats)}</vedtakssats>
        </vedtakssats-157>
        `);
	}

	private attestanterXml(): string {
		return listeXml(
			this.attestanter.filter((attestant) =>
				[attestant.attestantId, attestant.datoUgyldigFom].some(
					(v) => v != null,
				),
			),
			(attestant) => `
            <attestant-180>
                ${valgfriTag("attestantId", attestant.attestantId)}
                ${valgfriTag("datoUgyldigFom", xmlDato(attestant.datoUgyldigFom))}
            </attestant-180>
        `,
		);
	}

	xml({
		delytelseId,
		refDelytelseId,
	}: {
		delytelseId: string;
		refDelytelseId?: string;
	}): string {
		return enLinje(`
        <oppdrags-linje-150>
            ${valgfriTag("kodeEndringLinje", this.felter.kodeEndringLinje)}
            ${valgfriTag("kodeStatusLinje", this.felter.kodeStatusLinje)}
            ${valgfriTag("datoStatusFom", xmlDato(this.felter.datoStatusFom))}
            ${valgfriTag("vedtakId", this.felter.vedtakId)}
            <delytelseId>${delytelseId}</delytelseId>
            ${valgfriTag("kodeKlassifik", this.felter.kodeKlassifik)}
            ${valgfriTag("datoVedtakFom", xmlDato(this.fom))}
            ${valgfriTag("datoVedtakTom", xmlDato(this.tom))}
            ${valgfriTag("sats", this.felter.sats?.toString())}
            ${valgfriTag("fradragTillegg", this.felter.fradragTillegg)}
            ${valgfriTag("typeSats", this.felter.typeSats)}
            ${valgfriTag("skyldnerId", this.felter.skyldnerId)}
            ${valgfriTag("datoSkyldnerFom", xmlDato(this.felter.datoSkyldnerFom))}
            ${valgfriTag("kid", this.felter.kid)}
            ${valgfriTag("datoKidFom", xmlDato(this.felter.datoKidFom))}
            ${valgfriTag("brukKjoreplan", this.felter.brukKjoreplan)}
            ${valgfriTag("saksbehId", this.felter.saksbehId)}
            ${valgfriTag("utbetalesTilId", this.felter.utbetalesTilId)}
            ${valgfriTag("datoUtbetalesTilIdFom", xmlDato(this.felter.datoUtbetalesTilIdFom))}
            ${valgfriTag("henvisning", this.felter.henvisning)}
            ${valgfriTag("typeSoknad", this.felter.typeSoknad)}
            ${valgfriTag("refFagsystemId", this.felter.refFagsystemId)}
            ${valgfriTag("refDelytelseId", refDelytelseId)}
            ${this.refusjonsinfoXml()}
            ${this.vedtakssatsXml()}
            ${this.linjeTeksterXml()}
            ${this.graderXml()}
            ${this.attestanterXml()}
        </oppdrags-linje-150>
        `);
	}
}

export class KjedeBuilder {
	private readonly linjer: LinjeBuilder[] = [];

	constructor(readonly kjedeidentifikator: Kjedeidentifikator) {}

	medLinje(linje: LinjeBuilder): KjedeBuilder {
		this.linjer.push(linje);
		return this;
	}

	antallLinjer(): number {
		return this.linjer.length;
	}

	private sorterteLinjer(): LinjeBuilder[] {
		return [...this.linjer].sort((a, b) => {
			const fomA = a.fom != null ? tolkDato(a.fom)?.getTime() : undefined;
			const fomB = b.fom != null ? tolkDato(b.fom)?.getTime() : undefined;
			return fomA != null && fomB != null
				? fomA - fomB
				: (a.fom ?? "").localeCompare(b.fom ?? "");
		});
	}

	// forrigeLinjenummer > 0 betyr at kjeden fortsetter fra en tidligere innsending
	xml(forrigeLinjenummer = 0): string {
		const id = this.kjedeidentifikator.id;
		return this.sorterteLinjer()
			.map((linje, index) => {
				const nummer = forrigeLinjenummer + index + 1;
				return linje.xml({
					delytelseId: `kjede-${id}-linje-${nummer}`,
					refDelytelseId:
						nummer > 1 ? `kjede-${id}-linje-${nummer - 1}` : undefined,
				});
			})
			.join("");
	}
}

export type OppdragsnivaaFelter = {
	kodeFagomraade?: string;
	kodeAksjon?: string;
	kodeEndring?: string;
	kodeStatus?: string;
	datoStatusFom?: string;
	utbetFrekvens?: string;
	datoForfall?: string;
	stonadId?: string;
	oppdragGjelderId?: string;
	datoOppdragGjelderFom?: string;
	saksbehId?: string;
	kodeKomponent?: string;
	nokkelAvstemming?: string;
	tidspktMelding?: string;
	bilagsType?: string;
};

export type Enhet = {
	typeEnhet?: string;
	enhet?: string;
	datoEnhetFom?: string;
};

export type Avvent = {
	datoAvventFom?: string;
	datoAvventTom?: string;
	kodeArsak?: string;
	datoOverfores?: string;
	feilreg?: string;
};

export type Ompostering = {
	omPostering?: string;
	datoOmposterFom?: string;
	feilreg?: string;
	tidspktReg?: string;
	saksbehId?: string;
};

export type Tekst = {
	tekstLnr?: string;
	kodeTekst?: string;
	tekst?: string;
	datoTekstFom?: string;
	datoTekstTom?: string;
	feilreg?: string;
};

// Ikke eksportert, så metoden under kan bare kalles fra denne filen
const brukKjedeIderEtter = Symbol("brukKjedeIderEtter");

export class InnsendingBuilder {
	private readonly felter: OppdragsnivaaFelter = {};
	private readonly enheter: Enhet[] = [];
	private readonly avventer: Avvent[] = [];
	private readonly tekster: Tekst[] = [];
	private ompostering?: Ompostering;
	private readonly kjeder: KjedeBuilder[] = [];
	private nesteKjedeId = 1;

	constructor(readonly oppdragsidentifikator: Oppdragsidentifikator) {}

	get fagsystemId(): string {
		return fagsystemId(this.oppdragsidentifikator);
	}

	nyKjede(): KjedeBuilder {
		return new KjedeBuilder({
			id: this.nesteKjedeId++,
			oppdragsidentifikator: this.oppdragsidentifikator,
		});
	}

	fortsettKjede(kjedeidentifikator: Kjedeidentifikator): KjedeBuilder {
		if (
			!sammeOppdrag(
				kjedeidentifikator.oppdragsidentifikator,
				this.oppdragsidentifikator,
			)
		) {
			throw new Error(
				`Kjede ${kjedeidentifikator.id} tilhører et annet oppdrag enn denne innsendingen`,
			);
		}
		this[brukKjedeIderEtter](kjedeidentifikator.id);
		return new KjedeBuilder({
			id: kjedeidentifikator.id,
			oppdragsidentifikator: this.oppdragsidentifikator,
		});
	}

	// Sørger for at nyKjede() ikke gir ut id-er som allerede er brukt
	[brukKjedeIderEtter](id: number): void {
		this.nesteKjedeId = Math.max(this.nesteKjedeId, id + 1);
	}

	medKjede(kjede: KjedeBuilder): InnsendingBuilder {
		if (
			kjede.kjedeidentifikator.oppdragsidentifikator !==
			this.oppdragsidentifikator
		) {
			throw new Error(
				`Kjede ${kjede.kjedeidentifikator.id} hører ikke til denne innsendingen`,
			);
		}
		this.kjeder.push(kjede);
		return this;
	}

	medEnhet(enhet: Enhet): InnsendingBuilder {
		this.enheter.push(enhet);
		return this;
	}

	medAvvent(avvent: Avvent): InnsendingBuilder {
		this.avventer.push(avvent);
		return this;
	}

	medOmpostering(ompostering: Ompostering): InnsendingBuilder {
		this.ompostering = ompostering;
		return this;
	}

	medTekst(tekst: Tekst): InnsendingBuilder {
		this.tekster.push(tekst);
		return this;
	}

	med(felter: Partial<OppdragsnivaaFelter>): InnsendingBuilder {
		Object.assign(this.felter, felter);
		return this;
	}

	kjedeidentifikatorer(): Kjedeidentifikator[] {
		return this.kjeder.map((k) => k.kjedeidentifikator);
	}

	private avstemmingXml(): string {
		const { kodeKomponent, nokkelAvstemming, tidspktMelding } = this.felter;
		if (
			[kodeKomponent, nokkelAvstemming, tidspktMelding].every((v) => v == null)
		)
			return "";

		return enLinje(`
        <avstemming-115>
            ${valgfriTag("kodeKomponent", kodeKomponent)}
            ${valgfriTag("nokkelAvstemming", nokkelAvstemming)}
            ${valgfriTag("tidspktMelding", xmlTidspunkt(tidspktMelding))}
        </avstemming-115>
        `);
	}

	private bilagstypeXml(): string {
		if (this.felter.bilagsType == null) return "";
		return enLinje(`
        <bilagstype-113>
            <bilagsType>${escapeXml(this.felter.bilagsType)}</bilagsType>
        </bilagstype-113>
        `);
	}

	private enheterXml(): string {
		return listeXml(
			this.enheter.filter((enhet) =>
				[enhet.typeEnhet, enhet.enhet, enhet.datoEnhetFom].some(
					(v) => v != null,
				),
			),
			(enhet) => `
            <oppdrags-enhet-120>
                ${valgfriTag("typeEnhet", enhet.typeEnhet)}
                ${valgfriTag("enhet", enhet.enhet)}
                ${valgfriTag("datoEnhetFom", xmlDato(enhet.datoEnhetFom))}
            </oppdrags-enhet-120>
        `,
		);
	}

	private avventerXml(): string {
		return listeXml(
			this.avventer.filter((avvent) =>
				[
					avvent.datoAvventFom,
					avvent.datoAvventTom,
					avvent.kodeArsak,
					avvent.datoOverfores,
					avvent.feilreg,
				].some((v) => v != null),
			),
			(avvent) => `
            <avvent-118>
                ${valgfriTag("datoAvventFom", xmlDato(avvent.datoAvventFom))}
                ${valgfriTag("datoAvventTom", xmlDato(avvent.datoAvventTom))}
                ${valgfriTag("kodeArsak", avvent.kodeArsak)}
                ${valgfriTag("datoOverfores", xmlDato(avvent.datoOverfores))}
                ${valgfriTag("feilreg", avvent.feilreg)}
            </avvent-118>
        `,
		);
	}

	private omposteringXml(): string {
		if (this.ompostering == null) return "";
		return enLinje(`
        <ompostering-116>
            ${valgfriTag("omPostering", this.ompostering.omPostering)}
            ${valgfriTag("datoOmposterFom", xmlDato(this.ompostering.datoOmposterFom))}
            ${valgfriTag("feilreg", this.ompostering.feilreg)}
            ${valgfriTag("tidspktReg", xmlTidspunkt(this.ompostering.tidspktReg))}
            ${valgfriTag("saksbehId", this.ompostering.saksbehId)}
        </ompostering-116>
        `);
	}

	private teksterXml(): string {
		return listeXml(
			this.tekster.filter((tekst) =>
				[
					tekst.tekstLnr,
					tekst.kodeTekst,
					tekst.tekst,
					tekst.datoTekstFom,
					tekst.datoTekstTom,
					tekst.feilreg,
				].some((v) => v != null),
			),
			(tekst) => `
            <tekst-140>
                ${valgfriTag("tekstLnr", tekst.tekstLnr)}
                ${valgfriTag("kodeTekst", tekst.kodeTekst)}
                ${valgfriTag("tekst", tekst.tekst)}
                ${valgfriTag("datoTekstFom", xmlDato(tekst.datoTekstFom))}
                ${valgfriTag("datoTekstTom", xmlDato(tekst.datoTekstTom))}
                ${valgfriTag("feilreg", tekst.feilreg)}
            </tekst-140>
        `,
		);
	}

	// sisteLinjenummer holder siste linjenummer per kjede fra tidligere innsendinger, og oppdateres her
	private kjederXml(sisteLinjenummer: Map<number, number>): string {
		return this.kjeder
			.map((kjede) => {
				const id = kjede.kjedeidentifikator.id;
				const forrige = sisteLinjenummer.get(id) ?? 0;
				sisteLinjenummer.set(id, forrige + kjede.antallLinjer());
				return kjede.xml(forrige);
			})
			.join("");
	}

	xml(sisteLinjenummer: Map<number, number> = new Map()): string {
		return enLinje(`
        <oppdrag>
            <oppdrag-110>
                ${valgfriTag("kodeAksjon", this.felter.kodeAksjon)}
                ${valgfriTag("kodeEndring", this.felter.kodeEndring)}
                ${valgfriTag("kodeStatus", this.felter.kodeStatus)}
                ${valgfriTag("datoStatusFom", xmlDato(this.felter.datoStatusFom))}
                ${valgfriTag("kodeFagomraade", this.felter.kodeFagomraade)}
                <fagsystemId>${this.fagsystemId}</fagsystemId>
                ${valgfriTag("utbetFrekvens", this.felter.utbetFrekvens)}
                ${valgfriTag("datoForfall", xmlDato(this.felter.datoForfall))}
                ${valgfriTag("stonadId", this.felter.stonadId)}
                ${valgfriTag("oppdragGjelderId", this.felter.oppdragGjelderId)}
                ${valgfriTag("datoOppdragGjelderFom", xmlDato(this.felter.datoOppdragGjelderFom))}
                ${valgfriTag("saksbehId", this.felter.saksbehId)}
                ${this.bilagstypeXml()}
                ${this.avstemmingXml()}
                ${this.omposteringXml()}
                ${this.avventerXml()}
                ${this.enheterXml()}
                ${this.teksterXml()}
                ${this.kjederXml(sisteLinjenummer)}
            </oppdrag-110>
        </oppdrag>
        `);
	}
}

export class InnsendingerBuilder {
	private readonly innsendinger: InnsendingBuilder[] = [];
	private nesteOppdragId = 1;

	nyttOppdrag(): InnsendingBuilder {
		return new InnsendingBuilder({ id: this.nesteOppdragId++ });
	}

	fortsettOppdrag(
		oppdragsidentifikator: Oppdragsidentifikator,
	): InnsendingBuilder {
		if (
			!this.innsendinger.some((i) =>
				sammeOppdrag(i.oppdragsidentifikator, oppdragsidentifikator),
			)
		) {
			throw new Error(
				`Oppdrag ${fagsystemId(oppdragsidentifikator)} finnes ikke i en tidligere innsending`,
			);
		}
		return new InnsendingBuilder({ id: oppdragsidentifikator.id });
	}

	nyInnsending(innsendingBuilder: InnsendingBuilder): InnsendingerBuilder {
		const oppdrag = innsendingBuilder.oppdragsidentifikator;
		// Sørger for at nyttOppdrag() ikke gir ut id-er som allerede er brukt
		this.nesteOppdragId = Math.max(this.nesteOppdragId, oppdrag.id + 1);
		for (const kjede of this.kjeder(oppdrag)) {
			innsendingBuilder[brukKjedeIderEtter](kjede.id);
		}
		this.innsendinger.push(innsendingBuilder);
		return this;
	}

	build(): string[] {
		const sisteLinjenummer = new Map<number, Map<number, number>>();
		return this.innsendinger.map((innsending) => {
			const { id } = innsending.oppdragsidentifikator;
			const perKjede = sisteLinjenummer.get(id) ?? new Map();
			sisteLinjenummer.set(id, perKjede);
			return innsending.xml(perKjede);
		});
	}

	kjeder(oppdragsidentifikator: Oppdragsidentifikator): Kjedeidentifikator[] {
		const unike = new Map<number, Kjedeidentifikator>();
		for (const kjede of this.innsendinger.flatMap((i) =>
			i.kjedeidentifikatorer(),
		)) {
			if (!sammeOppdrag(kjede.oppdragsidentifikator, oppdragsidentifikator))
				continue;
			// delete + set flytter id-en bakerst, så rekkefølgen følger siste forekomst
			unike.delete(kjede.id);
			unike.set(kjede.id, kjede);
		}
		return [...unike.values()].reverse();
	}

	oppdragidentifikatorer(): Oppdragsidentifikator[] {
		const unike = new Map<number, Oppdragsidentifikator>();
		for (const { oppdragsidentifikator } of this.innsendinger) {
			// delete + set flytter id-en bakerst, så rekkefølgen følger siste forekomst
			unike.delete(oppdragsidentifikator.id);
			unike.set(oppdragsidentifikator.id, oppdragsidentifikator);
		}
		return [...unike.values()].reverse();
	}
}
