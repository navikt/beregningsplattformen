import { z } from "zod";
import { oppdragstidslinjeSchema } from "./OppdragstidslinjeDTO";

const ekstraInfoSchema = z.object({
	type: z.literal("TekstEkstraInfo"),
	grunn: z.string(),
	template: z.string(),
	verdier: z
		.record(
			z.string(),
			z.object({
				verdi: z.string(),
				skjult: z.boolean(),
			}),
		)
		.default({}),
	tekst: z.string().nullish(),
});

export type EkstraInfo = z.infer<typeof ekstraInfoSchema>;

const linjeSchema = z.object({
	id: z.number(),
	type: z.enum(["YTELSE", "SKATT", "TREKK", "FRIKORT_BRUK"]),
	sats: z.string().nullable(),
	beloep: z.string(),
	ekstraInfo: z.array(ekstraInfoSchema).nullable(),
});

const delberegningSchema = z.object({
	id: z.number(),
	fom: z.string(),
	tom: z.string(),
	fagomraade: z.string(),
	beloep: z.string(),
	linjer: z.array(linjeSchema),
	ekstraInfo: z.array(ekstraInfoSchema),
});

const beregningSchema = z.object({
	id: z.number(),
	gjelder: z.string(),
	utbetalesTil: z.string(),
	kidnummer: z.string().nullable(),
	status: z.string(),
	beregnet: z.string().nullish(),
	delberegninger: z.array(delberegningSchema),
	ekstraInfo: z.array(ekstraInfoSchema),
});

export const beregningResponseSchema = z.object({
	oppdragstidslinje: oppdragstidslinjeSchema,
	beregningstidslinje: z
		.object({
			beregninger: z.array(beregningSchema),
		})
		.optional(),
});

export type BeregningResponse = z.infer<typeof beregningResponseSchema>;
