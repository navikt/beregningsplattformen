import { describe, expect, it } from "vitest";
import {
	InnsendingBuilder,
	InnsendingerBuilder,
	LinjeBuilder,
} from "./InnsendingerBuilder";

// Fjerner linjeskift og innrykk mellom tagger, så XML kan sammenlignes uavhengig av formatering
function enLinje(xml: string): string {
	return xml.replace(/>\s+</g, "><").trim();
}

describe("InnsendingerBuilder", () => {
	it("medKjede avviser kjede fra en annen innsending", () => {
		const innsending1 = new InnsendingBuilder({ id: 1 });
		const innsending2 = new InnsendingBuilder({ id: 1 });

		expect(() => innsending2.medKjede(innsending1.nyKjede())).toThrow();
	});

	it("escaper spesialtegn i verdier", () => {
		const xml = new InnsendingBuilder({ id: 1 })
			.med({ kodeFagomraade: "A&B" })
			.xml();

		expect(enLinje(xml)).toBe(
			"<oppdrag><oppdrag-110><kodeFagomraade>A&amp;B</kodeFagomraade><fagsystemId>FAGID-1</fagsystemId></oppdrag-110></oppdrag>",
		);
	});

	it("utelater enheter, avventer og tekster uten verdier", () => {
		const xml = new InnsendingBuilder({ id: 1 })
			.medEnhet({})
			.medAvvent({})
			.medTekst({})
			.xml();

		expect(enLinje(xml)).toBe(
			"<oppdrag><oppdrag-110><fagsystemId>FAGID-1</fagsystemId></oppdrag-110></oppdrag>",
		);
	});

	describe("oppdrag-id-er", () => {
		const innsendingerBuilder = new InnsendingerBuilder();
		const oppdrag1 = innsendingerBuilder.nyttOppdrag();
		innsendingerBuilder.nyInnsending(oppdrag1);
		const oppdrag2 = innsendingerBuilder.nyttOppdrag();
		innsendingerBuilder.nyInnsending(oppdrag2);
		const fortsatt = innsendingerBuilder.fortsettOppdrag(
			oppdrag1.oppdragsidentifikator,
		);
		innsendingerBuilder.nyInnsending(fortsatt);
		const xmler = innsendingerBuilder.build().map(enLinje);

		it("nytt oppdrag får neste ledige id, og fagsystemId er FAGID-id", () => {
			expect(xmler[0]).toContain("<fagsystemId>FAGID-1</fagsystemId>");
			expect(xmler[1]).toContain("<fagsystemId>FAGID-2</fagsystemId>");
		});

		it("fortsatt oppdrag beholder id-en", () => {
			expect(xmler[2]).toContain("<fagsystemId>FAGID-1</fagsystemId>");
		});

		it("avviser oppdrag som ikke finnes fra før", () => {
			expect(() => innsendingerBuilder.fortsettOppdrag({ id: 99 })).toThrow();
		});

		it("nyttOppdrag hopper over id-er som er lagt til manuelt", () => {
			innsendingerBuilder.nyInnsending(new InnsendingBuilder({ id: 7 }));
			expect(innsendingerBuilder.nyttOppdrag().oppdragsidentifikator.id).toBe(
				8,
			);
		});
	});

	describe("fortsette kjede i ny innsending", () => {
		const innsendingerBuilder = new InnsendingerBuilder();
		const innsending1 = innsendingerBuilder.nyttOppdrag();
		innsendingerBuilder.nyInnsending(innsending1);
		innsending1.medKjede(
			innsending1
				.nyKjede()
				.medLinje(new LinjeBuilder("2024-01-01"))
				.medLinje(new LinjeBuilder("2024-02-01")),
		);

		const oppdrag = innsending1.oppdragsidentifikator;
		const innsending2 = innsendingerBuilder.fortsettOppdrag(oppdrag);
		const [kjede1] = innsendingerBuilder.kjeder(oppdrag);
		innsendingerBuilder.nyInnsending(innsending2);
		const nyKjede = innsending2
			.nyKjede()
			.medLinje(new LinjeBuilder("2024-04-01"));
		innsending2
			.medKjede(
				innsending2
					.fortsettKjede(kjede1)
					.medLinje(new LinjeBuilder("2024-03-01")),
			)
			.medKjede(nyKjede);

		const xml = enLinje(innsendingerBuilder.build()[1]);

		it("linjenummer fortsetter og peker på siste linje fra forrige innsending", () => {
			expect(xml).toContain(
				"<delytelseId>kjede-1-linje-3</delytelseId><datoVedtakFom>2024-03-01+01:00</datoVedtakFom><refDelytelseId>kjede-1-linje-2</refDelytelseId>",
			);
		});

		it("ny kjede får id som ikke er brukt for oppdraget", () => {
			expect(nyKjede.kjedeidentifikator.id).toBe(2);
			expect(xml).toContain("<delytelseId>kjede-2-linje-1</delytelseId>");
		});

		it("kjeder gir hver kjede én gang", () => {
			expect(innsendingerBuilder.kjeder(oppdrag).map((k) => k.id)).toEqual([
				2, 1,
			]);
		});

		it("avviser kjede fra et annet oppdrag", () => {
			const annet = new InnsendingBuilder({ id: 2 });
			expect(() => annet.fortsettKjede(kjede1)).toThrow();
		});
	});

	describe("hente kjeder og oppdragsidentifikatorer", () => {
		const innsendingerBuilder = new InnsendingerBuilder();
		const innsending1 = innsendingerBuilder.nyttOppdrag();
		const innsending2 = innsendingerBuilder.nyttOppdrag();
		const kjede1 = innsending1.nyKjede();
		const kjede2 = innsending2.nyKjede();
		const kjede3 = innsending2.nyKjede();

		innsendingerBuilder.nyInnsending(innsending1.medKjede(kjede1));
		innsendingerBuilder.nyInnsending(
			innsending2.medKjede(kjede2).medKjede(kjede3),
		);

		const oppdragsidentifikatorer =
			innsendingerBuilder.oppdragidentifikatorer();
		const aap = { id: 1 };
		const sp = { id: 2 };

		const kjederForAAP = innsendingerBuilder.kjeder(aap);
		const kjederForSP = innsendingerBuilder.kjeder(sp);
		const kjederForNoeSomIkkeFinnes = innsendingerBuilder.kjeder({ id: 99 });

		it("oppdragsidentifikatorer er unike med nyeste først", () => {
			expect(oppdragsidentifikatorer).toEqual([sp, aap]);
		});

		it("kjeder for AAP", () => {
			expect(kjederForAAP).toEqual([{ id: 1, oppdragsidentifikator: aap }]);
		});

		it("kjeder for SP med nyeste først", () => {
			expect(kjederForSP).toEqual([
				{ id: 2, oppdragsidentifikator: sp },
				{ id: 1, oppdragsidentifikator: sp },
			]);
		});

		it("ingen kjeder for ukjent oppdrag", () => {
			expect(kjederForNoeSomIkkeFinnes).toEqual([]);
		});
	});

	describe("lage innsendinger", () => {
		const innsendingerBuilder = new InnsendingerBuilder();
		const innsendingBuilder = innsendingerBuilder
			.nyttOppdrag()
			.med({
				kodeFagomraade: "AAP",
				kodeAksjon: "impl-1",
				kodeEndring: "impl-NY",
				saksbehId: "iml-A1234",
				utbetFrekvens: "iml-MND",
				oppdragGjelderId: "impl-11111111111",
				datoOppdragGjelderFom: "impl-neitakk",
				kodeStatus: "impl-OK",
				datoStatusFom: "impl-jojo",
				datoForfall: "2018-01-02",
				stonadId: "impl-1234",
				bilagsType: "impl-en-billagstype",
			})
			.med({
				kodeKomponent: "iml-komponent",
				nokkelAvstemming: "impl-nokkel",
			})
			.medEnhet({
				typeEnhet: "impl-BOS",
				enhet: "impl-8020",
			})
			.medEnhet({
				datoEnhetFom: "impl-2025-04-22",
			})
			.medAvvent({
				kodeArsak: "impl-AVRK",
				datoAvventFom: "impl-fom",
				datoAvventTom: "impl-tom",
				datoOverfores: "2018-05-05",
				feilreg: "impl-ikkedato",
			})
			.medAvvent({
				kodeArsak: "neitakk",
			})
			.medOmpostering({
				omPostering: "impl-J",
				datoOmposterFom: "2024-03-01",
				feilreg: "impl-N",
				tidspktReg: "impl-ikketidspunkt",
				saksbehId: "impl-O1234",
			})
			.medTekst({
				tekstLnr: "impl-1",
				kodeTekst: "impl-kode",
				tekst: "impl-tekst",
				datoTekstFom: "2024-01-01",
				datoTekstTom: "impl-ikkedato",
				feilreg: "impl-N",
			})
			.medTekst({
				tekst: "impl-tekst-to",
			});

		const linjeEn = new LinjeBuilder("2024-01-01", "2024-01-31")
			.med({
				kodeEndringLinje: "impl-NY",
				vedtakId: "impl-vedtakId",
				sats: "impl-400",
				typeSats: "impl-MDN",
				kodeKlassifik: "impl-AAPAU",
				fradragTillegg: "impl-F",
				brukKjoreplan: "impl-N",
				saksbehId: "impl-S1234",
				utbetalesTilId: "impl-22222222222",
				henvisning: "impl-njet",
				refFagsystemId: "impl-nope",
				vedtakssats: "impl-1337",
				kodeStatusLinje: "impl-OPPH",
				datoStatusFom: "2024-01-15",
				skyldnerId: "impl-skyldner",
				datoSkyldnerFom: "impl-ikkedato",
				kid: "impl-kid",
				datoKidFom: "2024-01-10",
				datoUtbetalesTilIdFom: "impl-ikkedato",
				typeSoknad: "impl-soknad",
			})
			.medRefusjonsinfo({
				maksDato: "2024-12-31",
				refunderesId: "impl-refunderes",
				datoFom: "impl-ikkedato",
			});

		const linjeTo = new LinjeBuilder("2024-02-01", "2024-02-29")
			.med({ sats: "impl-500", typeSats: "impl-DAG" })
			.medAttestant({
				attestantId: "impl-attestant",
			})
			.medAttestant({
				datoUgyldigFom: "impl-neitakk",
			})
			.medLinjeTekst({
				tekstLnr: "impl-1",
				tekstKode: "impl-kode",
				tekst: "impl-linjetekst",
				datoTekstFom: "impl-ikkedato",
				datoTekstTom: "2024-02-29",
				feilreg: "impl-N",
			})
			.medGrad({
				typeGrad: "impl-UFOR",
				grad: "impl-50",
			})
			.medGrad({
				grad: "impl-100",
			});

		const kjedeEn = innsendingBuilder.nyKjede().medLinje(linjeEn);
		const kjedeTo = innsendingBuilder
			.nyKjede()
			.medLinje(linjeTo)
			.medLinje(linjeEn);

		innsendingBuilder.medKjede(kjedeEn).medKjede(kjedeTo);

		innsendingerBuilder.nyInnsending(innsendingBuilder);
		innsendingerBuilder.nyInnsending(innsendingerBuilder.nyttOppdrag());

		const xmler = innsendingerBuilder.build();

		it("bygger forventet XML for første innsending", () => {
			expect(enLinje(xmler[0])).toBe(enLinje(forventet));
		});

		it("andre XML innholder ingen ting", () => {
			expect(enLinje(xmler[1])).toBe(
				"<oppdrag><oppdrag-110><fagsystemId>FAGID-2</fagsystemId></oppdrag-110></oppdrag>",
			);
		});
	});

	const forventet = `
<oppdrag>
    <oppdrag-110>
        <kodeAksjon>impl-1</kodeAksjon>
        <kodeEndring>impl-NY</kodeEndring>
        <kodeStatus>impl-OK</kodeStatus>
        <datoStatusFom>impl-jojo</datoStatusFom>
        <kodeFagomraade>AAP</kodeFagomraade>
        <fagsystemId>FAGID-1</fagsystemId>
        <utbetFrekvens>iml-MND</utbetFrekvens>
        <datoForfall>2018-01-02+01:00</datoForfall>
        <stonadId>impl-1234</stonadId>
        <oppdragGjelderId>impl-11111111111</oppdragGjelderId>
        <datoOppdragGjelderFom>impl-neitakk</datoOppdragGjelderFom>
        <saksbehId>iml-A1234</saksbehId>
        <bilagstype-113>
            <bilagsType>impl-en-billagstype</bilagsType>
        </bilagstype-113>
        <avstemming-115>
            <kodeKomponent>iml-komponent</kodeKomponent>
            <nokkelAvstemming>impl-nokkel</nokkelAvstemming>
        </avstemming-115>
        <ompostering-116>
            <omPostering>impl-J</omPostering>
            <datoOmposterFom>2024-03-01+01:00</datoOmposterFom>
            <feilreg>impl-N</feilreg>
            <tidspktReg>impl-ikketidspunkt</tidspktReg>
            <saksbehId>impl-O1234</saksbehId>
        </ompostering-116>
        <avvent-118>
            <datoAvventFom>impl-fom</datoAvventFom>
            <datoAvventTom>impl-tom</datoAvventTom>
            <kodeArsak>impl-AVRK</kodeArsak>
            <datoOverfores>2018-05-05+01:00</datoOverfores>
            <feilreg>impl-ikkedato</feilreg>
        </avvent-118>
        <avvent-118>
            <kodeArsak>neitakk</kodeArsak>
        </avvent-118>
        <oppdrags-enhet-120>
            <typeEnhet>impl-BOS</typeEnhet>
            <enhet>impl-8020</enhet>
        </oppdrags-enhet-120>
        <oppdrags-enhet-120>
            <datoEnhetFom>2025-04-22+01:00</datoEnhetFom>
        </oppdrags-enhet-120>
        <tekst-140>
            <tekstLnr>impl-1</tekstLnr>
            <kodeTekst>impl-kode</kodeTekst>
            <tekst>impl-tekst</tekst>
            <datoTekstFom>2024-01-01+01:00</datoTekstFom>
            <datoTekstTom>impl-ikkedato</datoTekstTom>
            <feilreg>impl-N</feilreg>
        </tekst-140>
        <tekst-140>
            <tekst>impl-tekst-to</tekst>
        </tekst-140>
        <oppdrags-linje-150>
            <kodeEndringLinje>impl-NY</kodeEndringLinje>
            <kodeStatusLinje>impl-OPPH</kodeStatusLinje>
            <datoStatusFom>2024-01-15+01:00</datoStatusFom>
            <vedtakId>impl-vedtakId</vedtakId>
            <delytelseId>kjede-1-linje-1</delytelseId>
            <kodeKlassifik>impl-AAPAU</kodeKlassifik>
            <datoVedtakFom>2024-01-01+01:00</datoVedtakFom>
            <datoVedtakTom>2024-01-31+01:00</datoVedtakTom>
            <sats>impl-400</sats>
            <fradragTillegg>impl-F</fradragTillegg>
            <typeSats>impl-MDN</typeSats>
            <skyldnerId>impl-skyldner</skyldnerId>
            <datoSkyldnerFom>impl-ikkedato</datoSkyldnerFom>
            <kid>impl-kid</kid>
            <datoKidFom>2024-01-10+01:00</datoKidFom>
            <brukKjoreplan>impl-N</brukKjoreplan>
            <saksbehId>impl-S1234</saksbehId>
            <utbetalesTilId>impl-22222222222</utbetalesTilId>
            <datoUtbetalesTilIdFom>impl-ikkedato</datoUtbetalesTilIdFom>
            <henvisning>impl-njet</henvisning>
            <typeSoknad>impl-soknad</typeSoknad>
            <refFagsystemId>impl-nope</refFagsystemId>
            <refusjonsinfo-156>
                <maksDato>2024-12-31+01:00</maksDato>
                <refunderesId>impl-refunderes</refunderesId>
                <datoFom>impl-ikkedato</datoFom>
            </refusjonsinfo-156>
            <vedtakssats-157>
                <vedtakssats>impl-1337</vedtakssats>
            </vedtakssats-157>
        </oppdrags-linje-150>
        <oppdrags-linje-150>
            <kodeEndringLinje>impl-NY</kodeEndringLinje>
            <kodeStatusLinje>impl-OPPH</kodeStatusLinje>
            <datoStatusFom>2024-01-15+01:00</datoStatusFom>
            <vedtakId>impl-vedtakId</vedtakId>
            <delytelseId>kjede-2-linje-1</delytelseId>
            <kodeKlassifik>impl-AAPAU</kodeKlassifik>
            <datoVedtakFom>2024-01-01+01:00</datoVedtakFom>
            <datoVedtakTom>2024-01-31+01:00</datoVedtakTom>
            <sats>impl-400</sats>
            <fradragTillegg>impl-F</fradragTillegg>
            <typeSats>impl-MDN</typeSats>
            <skyldnerId>impl-skyldner</skyldnerId>
            <datoSkyldnerFom>impl-ikkedato</datoSkyldnerFom>
            <kid>impl-kid</kid>
            <datoKidFom>2024-01-10+01:00</datoKidFom>
            <brukKjoreplan>impl-N</brukKjoreplan>
            <saksbehId>impl-S1234</saksbehId>
            <utbetalesTilId>impl-22222222222</utbetalesTilId>
            <datoUtbetalesTilIdFom>impl-ikkedato</datoUtbetalesTilIdFom>
            <henvisning>impl-njet</henvisning>
            <typeSoknad>impl-soknad</typeSoknad>
            <refFagsystemId>impl-nope</refFagsystemId>
            <refusjonsinfo-156>
                <maksDato>2024-12-31+01:00</maksDato>
                <refunderesId>impl-refunderes</refunderesId>
                <datoFom>impl-ikkedato</datoFom>
            </refusjonsinfo-156>
            <vedtakssats-157>
                <vedtakssats>impl-1337</vedtakssats>
            </vedtakssats-157>
        </oppdrags-linje-150>
        <oppdrags-linje-150>
            <delytelseId>kjede-2-linje-2</delytelseId>
            <datoVedtakFom>2024-02-01+01:00</datoVedtakFom>
            <datoVedtakTom>2024-02-29+01:00</datoVedtakTom>
            <sats>impl-500</sats>
            <typeSats>impl-DAG</typeSats>
            <refDelytelseId>kjede-2-linje-1</refDelytelseId>
            <linje-tekst-158>
                <tekstLnr>impl-1</tekstLnr>
                <tekstKode>impl-kode</tekstKode>
                <tekst>impl-linjetekst</tekst>
                <datoTekstFom>impl-ikkedato</datoTekstFom>
                <datoTekstTom>2024-02-29+01:00</datoTekstTom>
                <feilreg>impl-N</feilreg>
            </linje-tekst-158>
            <grad-170>
                <typeGrad>impl-UFOR</typeGrad>
                <grad>impl-50</grad>
            </grad-170>
            <grad-170>
                <grad>impl-100</grad>
            </grad-170>
            <attestant-180>
                <attestantId>impl-attestant</attestantId>
            </attestant-180>
            <attestant-180>
                <datoUgyldigFom>impl-neitakk</datoUgyldigFom>
            </attestant-180>
        </oppdrags-linje-150>
    </oppdrag-110>
</oppdrag>
    `;
});
