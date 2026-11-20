import { oppdragsXmlTypedTemplate } from "../../data/oppdragsXmlTypedTemplate";
import {
	type InnsendingSkjema,
	type LinjeSkjema,
	type MedNokkel,
	nyInnsending,
	nyKjede,
	nyLinje,
	nyNokkel,
} from "./skjema";

type XmlNode = { navn: string; tekst: string; barn: XmlNode[] };

// Builderen lager disse selv
const generert = new Set([
	"fagsystemId",
	"delytelseId",
	"refDelytelseId",
	"refFagsystemId",
]);

// Enkel parser for oppdrags-XML: bare elementer og tekst, ingen attributter eller CDATA
function lesXml(xml: string): XmlNode {
	const rot: XmlNode = { navn: "", tekst: "", barn: [] };
	const stakk = [rot];
	for (const [, slutt, navn, selvlukkende, tekst] of xml.matchAll(
		/<(\/?)([\w-]+)[^>]*?(\/?)>|([^<]+)/g,
	)) {
		const gjeldende = stakk[stakk.length - 1];
		if (tekst != null) {
			gjeldende.tekst += tekst;
		} else if (slutt) {
			stakk.pop();
		} else {
			const node: XmlNode = { navn, tekst: "", barn: [] };
			gjeldende.barn.push(node);
			if (!selvlukkende) stakk.push(node);
		}
	}
	return rot;
}

function fraXmlTekst(tekst: string): string {
	return tekst
		.trim()
		.replace(/&lt;/g, "<")
		.replace(/&gt;/g, ">")
		.replace(/&quot;/g, '"')
		.replace(/&apos;/g, "'")
		.replace(/&amp;/g, "&");
}

// Skjemaet lagrer datoer som ISO, og tidspunkt som lokal midnatt
function tilSkjemaverdi(verdi: string): string {
	const dato = /^(\d{4}-\d{2}-\d{2})[+-]\d{2}:\d{2}$/.exec(verdi);
	if (dato) return dato[1];
	const midnatt = /^(\d{4}-\d{2}-\d{2})-00\.00\.00\.000000$/.exec(verdi);
	if (midnatt) return `${midnatt[1]}T00:00:00`;
	return verdi;
}

const barn = (node: XmlNode, navn: string) =>
	node.barn.filter((b) => b.navn === navn);

const forste = (node: XmlNode, navn: string): XmlNode | undefined =>
	barn(node, navn)[0];

function verdier<T>(node: XmlNode | undefined): T {
	return Object.fromEntries(
		(node?.barn ?? [])
			.filter((b) => b.barn.length === 0 && !generert.has(b.navn))
			.map((b) => [b.navn, tilSkjemaverdi(fraXmlTekst(b.tekst))]),
	) as T;
}

function liste<T>(node: XmlNode, navn: string): MedNokkel<T>[] {
	return barn(node, navn).map((b) => ({
		...verdier<T>(b),
		nokkel: nyNokkel(),
	}));
}

function linjeFraXml(node: XmlNode): LinjeSkjema {
	const { datoVedtakFom, datoVedtakTom, ...felter } =
		verdier<Record<string, string>>(node);
	return {
		...nyLinje(),
		felter: {
			...felter,
			...verdier(forste(node, "vedtakssats-157")),
			fom: datoVedtakFom,
			tom: datoVedtakTom,
		},
		attestanter: liste(node, "attestant-180"),
		linjeTekster: liste(node, "linje-tekst-158"),
		grader: liste(node, "grad-170"),
		refusjonsinfo: verdier(forste(node, "refusjonsinfo-156")),
	};
}

// Alle linjene havner i én kjede, siden delytelseId og refDelytelseId lages av builderen
export function innsendingFraXml(xml: string): InnsendingSkjema {
	const oppdrag = forste(lesXml(xml), "oppdrag");
	const oppdrag110 = oppdrag && forste(oppdrag, "oppdrag-110");
	if (oppdrag110 == null) return nyInnsending();

	const linjer = barn(oppdrag110, "oppdrags-linje-150").map(linjeFraXml);
	return {
		...nyInnsending(),
		felter: {
			...verdier(oppdrag110),
			...verdier(forste(oppdrag110, "bilagstype-113")),
			...verdier(forste(oppdrag110, "avstemming-115")),
		},
		ompostering: verdier(forste(oppdrag110, "ompostering-116")),
		avventer: liste(oppdrag110, "avvent-118"),
		enheter: liste(oppdrag110, "oppdrags-enhet-120"),
		tekster: liste(oppdrag110, "tekst-140"),
		kjeder: linjer.length > 0 ? [{ ...nyKjede(), linjer }] : [],
	};
}

// Første innsending fylles ut som standardmalen på «Send inn oppdrags-XML»
export function standardInnsending(): InnsendingSkjema {
	return innsendingFraXml(
		oppdragsXmlTypedTemplate.replace(/\{(pnr|dato|number)\}/g, ""),
	);
}
