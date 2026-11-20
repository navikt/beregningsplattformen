import { describe, expect, it } from "vitest";
import { innsendingFraXml } from "./fraXml";

describe("innsendingFraXml", () => {
	const innsending = innsendingFraXml(`<oppdrag>
    <oppdrag-110>
        <kodeFagomraade>AAP</kodeFagomraade>
        <fagsystemId>skal-ignoreres</fagsystemId>
        <datoOppdragGjelderFom>2000-01-01+01:00</datoOppdragGjelderFom>
        <avstemming-115>
            <tidspktMelding>2025-04-22-00.00.00.000000</tidspktMelding>
        </avstemming-115>
        <oppdrags-enhet-120>
            <enhet>8020</enhet>
        </oppdrags-enhet-120>
        <oppdrags-linje-150>
            <delytelseId>skal-ignoreres</delytelseId>
            <datoVedtakFom>2026-04-15</datoVedtakFom>
            <henvisning>a&amp;b</henvisning>
            <vedtakssats-157><vedtakssats>500</vedtakssats></vedtakssats-157>
            <attestant-180><attestantId>Kelvin</attestantId></attestant-180>
        </oppdrags-linje-150>
        <oppdrags-linje-150>
            <refDelytelseId>skal-ignoreres</refDelytelseId>
        </oppdrags-linje-150>
    </oppdrag-110>
</oppdrag>`);
	const [linje1, linje2] = innsending.kjeder[0].linjer;

	it("leser oppdragsnivå, avstemming og datoer i skjemaformat", () => {
		expect(innsending.felter).toEqual({
			kodeFagomraade: "AAP",
			datoOppdragGjelderFom: "2000-01-01",
			tidspktMelding: "2025-04-22T00:00:00",
		});
		expect(innsending.enheter.map(({ enhet }) => enhet)).toEqual(["8020"]);
	});

	it("legger alle linjer i én kjede og hopper over genererte id-er", () => {
		expect(innsending.kjeder).toHaveLength(1);
		expect(linje1.felter).toEqual({
			fom: "2026-04-15",
			tom: undefined,
			henvisning: "a&b",
			vedtakssats: "500",
		});
		expect(linje1.attestanter.map((a) => a.attestantId)).toEqual(["Kelvin"]);
		expect(linje2.felter).toEqual({ fom: undefined, tom: undefined });
	});
});
