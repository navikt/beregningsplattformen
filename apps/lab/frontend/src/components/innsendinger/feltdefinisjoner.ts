import type {
	Attestant,
	Avvent,
	Enhet,
	Grad,
	LinjeTekst,
	Ompostering,
	OppdragsnivaaFelter,
	Refusjonsinfo,
	Tekst,
} from "./InnsendingerBuilder";
import type { LinjeSkjema } from "./skjema";

export type FeltDefinisjon<T> = {
	navn: keyof T & string;
	// XML-navnet, så feltene kan sammenlignes med XSD-en
	label: string;
	dato?: boolean;
	// Dato som sendes som tidspunkt ved midnatt, fritekst er fortsatt lov
	tidspunkt?: boolean;
	beskrivelse?: string;
	// Gir nedtrekksliste, men brukeren kan fortsatt skrive en egen verdi
	forslag?: Forslag[];
	// Må fylles ut for en gyldig innsending. Vises først i gruppen, uten at skjemaet krever det.
	pakrevd?: boolean;
};

// label vises i nedtrekkslista, value havner i XML-en
export type Forslag = { label: string; value: string };

const jaNei: Forslag[] = [
	{ label: "J", value: "J" },
	{ label: "N", value: "N" },
];

const utbetFrekvens: Forslag[] = [
	{ label: "DAG (Daglig)", value: "DAG" },
	{ label: "UKE (Ukentlig)", value: "UKE" },
	{ label: "MND (Månedlig)", value: "MND" },
	{ label: "14DG (Annenhver uke)", value: "14DG" },
	{ label: "ENG (Engangsutbetaling)", value: "ENG" },
];

// Gjelder bare oppdragsnivå, kodeEndringLinje har egne valg uten UENDR
const kodeEndring: Forslag[] = [
	{ label: "NY", value: "NY" },
	{ label: "ENDR (Endring)", value: "ENDR" },
	{ label: "UENDR (Uendret)", value: "UENDR" },
];

// Gjelder bare oppdrag-110, kodeStatusLinje har egne valg
const kodeStatus: Forslag[] = [
	{ label: "OPPH (Opphør)", value: "OPPH" },
	{ label: "HVIL", value: "HVIL" },
	{ label: "SPER", value: "SPER" },
	{ label: "OPNY (Opphør og ny)", value: "OPNY" },
];

const kodeArsak: Forslag[] = [
	{ label: "AVAV (Avventer avregning)", value: "AVAV" },
	{ label: "AVRF (Avventer refusjonskrav)", value: "AVRF" },
];

const typeEnhet: Forslag[] = [
	{ label: "BOS (Bostedsenhet)", value: "BOS" },
	{ label: "BEH (Behandlende enhet)", value: "BEH" },
];

const fradragTillegg: Forslag[] = [
	{ label: "F (Fradrag)", value: "F" },
	{ label: "T (Tillegg)", value: "T" },
];

const typeSats: Forslag[] = [
	{ label: "DAG (Dagsats)", value: "DAG" },
	{ label: "UKE (Ukessats)", value: "UKE" },
	{ label: "MND (Månedssats)", value: "MND" },
	{ label: "14D (14 dagers sats)", value: "14D" },
	{ label: "AAR (Årlig sats)", value: "AAR" },
	{ label: "ENG (Engangssats)", value: "ENG" },
];

const kodeEndringLinje: Forslag[] = [
	{ label: "NY", value: "NY" },
	{ label: "ENDR (Endring)", value: "ENDR" },
];

const kodeStatusLinje: Forslag[] = [
	{ label: "HVIL", value: "HVIL" },
	{ label: "SPER", value: "SPER" },
	{ label: "OPPH (Opphør)", value: "OPPH" },
	{ label: "REAK (Reaktiver)", value: "REAK" },
];

const kodeAksjon: Forslag[] = [
	{ label: "1 (Utbetaling)", value: "1" },
	{ label: "O (Utbetaling)", value: "O" },
	{ label: "3 (Simulering)", value: "3" },
	{ label: "S (Simulering)", value: "S" },
];

export const oppdragsfelter: FeltDefinisjon<OppdragsnivaaFelter>[] = [
	{
		navn: "kodeAksjon",
		label: "kodeAksjon",
		forslag: kodeAksjon,
		pakrevd: true,
	},
	{
		navn: "kodeEndring",
		label: "kodeEndring",
		forslag: kodeEndring,
		pakrevd: true,
	},
	{ navn: "kodeStatus", label: "kodeStatus", forslag: kodeStatus },
	{ navn: "datoStatusFom", label: "datoStatusFom", dato: true },
	{ navn: "utbetFrekvens", label: "utbetFrekvens", forslag: utbetFrekvens },
	{ navn: "datoForfall", label: "datoForfall", dato: true },
	{ navn: "stonadId", label: "stonadId" },
	{ navn: "oppdragGjelderId", label: "oppdragGjelderId", pakrevd: true },
	{
		navn: "datoOppdragGjelderFom",
		label: "datoOppdragGjelderFom",
		dato: true,
		pakrevd: true,
	},
	{ navn: "saksbehId", label: "saksbehId", pakrevd: true },
	{ navn: "bilagsType", label: "bilagsType (bilagstype-113)" },
];

export const avstemmingsfelter: FeltDefinisjon<OppdragsnivaaFelter>[] = [
	{ navn: "kodeKomponent", label: "kodeKomponent" },
	{ navn: "nokkelAvstemming", label: "nokkelAvstemming" },
	{ navn: "tidspktMelding", label: "tidspktMelding", tidspunkt: true },
];

export const omposteringsfelter: FeltDefinisjon<Ompostering>[] = [
	{ navn: "omPostering", label: "omPostering", forslag: jaNei },
	{ navn: "datoOmposterFom", label: "datoOmposterFom", dato: true },
	{ navn: "feilreg", label: "feilreg", forslag: jaNei },
	{ navn: "tidspktReg", label: "tidspktReg", tidspunkt: true },
	{ navn: "saksbehId", label: "saksbehId" },
];

export const avventfelter: FeltDefinisjon<Avvent>[] = [
	{ navn: "datoAvventFom", label: "datoAvventFom", dato: true },
	{ navn: "datoAvventTom", label: "datoAvventTom", dato: true },
	{ navn: "kodeArsak", label: "kodeArsak", forslag: kodeArsak },
	{ navn: "datoOverfores", label: "datoOverfores", dato: true },
	{ navn: "feilreg", label: "feilreg", forslag: jaNei },
];

export const enhetsfelter: FeltDefinisjon<Enhet>[] = [
	{ navn: "typeEnhet", label: "typeEnhet", forslag: typeEnhet },
	{ navn: "enhet", label: "enhet" },
	{ navn: "datoEnhetFom", label: "datoEnhetFom", dato: true },
];

export const tekstfelter: FeltDefinisjon<Tekst>[] = [
	{ navn: "tekstLnr", label: "tekstLnr" },
	{ navn: "kodeTekst", label: "kodeTekst" },
	{ navn: "tekst", label: "tekst" },
	{ navn: "datoTekstFom", label: "datoTekstFom", dato: true },
	{ navn: "datoTekstTom", label: "datoTekstTom", dato: true },
	{ navn: "feilreg", label: "feilreg", forslag: jaNei },
];

export const linjefelter: FeltDefinisjon<LinjeSkjema["felter"]>[] = [
	{ navn: "fom", label: "datoVedtakFom", dato: true, pakrevd: true },
	{ navn: "kodeKlassifik", label: "kodeKlassifik", pakrevd: true },
	{ navn: "sats", label: "sats", pakrevd: true },
	{ navn: "typeSats", label: "typeSats", forslag: typeSats, pakrevd: true },
	{
		navn: "kodeEndringLinje",
		label: "kodeEndringLinje",
		forslag: kodeEndringLinje,
		pakrevd: true,
	},
	{
		navn: "fradragTillegg",
		label: "fradragTillegg",
		forslag: fradragTillegg,
		pakrevd: true,
	},
	{ navn: "saksbehId", label: "saksbehId", pakrevd: true },
	{ navn: "tom", label: "datoVedtakTom", dato: true },
	{ navn: "vedtakssats", label: "vedtakssats (vedtakssats-157)" },
	{ navn: "utbetalesTilId", label: "utbetalesTilId" },
	{ navn: "datoUtbetalesTilIdFom", label: "datoUtbetalesTilIdFom", dato: true },
	{
		navn: "kodeStatusLinje",
		label: "kodeStatusLinje",
		forslag: kodeStatusLinje,
	},
	{ navn: "datoStatusFom", label: "datoStatusFom", dato: true },
	{ navn: "skyldnerId", label: "skyldnerId" },
	{ navn: "datoSkyldnerFom", label: "datoSkyldnerFom", dato: true },
	{ navn: "kid", label: "kid" },
	{ navn: "datoKidFom", label: "datoKidFom", dato: true },
	{ navn: "vedtakId", label: "vedtakId" },
	{ navn: "brukKjoreplan", label: "brukKjoreplan", forslag: jaNei },
	{ navn: "henvisning", label: "henvisning" },
	{ navn: "typeSoknad", label: "typeSoknad" },
	{ navn: "refFagsystemId", label: "refFagsystemId" },
];

export const refusjonsinfofelter: FeltDefinisjon<Refusjonsinfo>[] = [
	{ navn: "maksDato", label: "maksDato", dato: true },
	{ navn: "refunderesId", label: "refunderesId" },
	{ navn: "datoFom", label: "datoFom", dato: true },
];

export const linjetekstfelter: FeltDefinisjon<LinjeTekst>[] = [
	{ navn: "tekstLnr", label: "tekstLnr" },
	{ navn: "tekstKode", label: "tekstKode" },
	{ navn: "tekst", label: "tekst" },
	{ navn: "datoTekstFom", label: "datoTekstFom", dato: true },
	{ navn: "datoTekstTom", label: "datoTekstTom", dato: true },
	{ navn: "feilreg", label: "feilreg", forslag: jaNei },
];

export const gradfelter: FeltDefinisjon<Grad>[] = [
	{ navn: "typeGrad", label: "typeGrad" },
	{ navn: "grad", label: "grad" },
];

export const attestantfelter: FeltDefinisjon<Attestant>[] = [
	{ navn: "attestantId", label: "attestantId" },
	{ navn: "datoUgyldigFom", label: "datoUgyldigFom", dato: true },
];
