import { z } from "zod";
import { oppdragstidslinjeSchema } from "./OppdragstidslinjeDTO";

const linjeSchema = z.object({
	id: z.number(),
	type: z.enum(["YTELSE", "SKATT", "TREKK"]),
	sats: z.string().nullable(),
	beloep: z.string(),
});

const delberegningSchema = z.object({
	id: z.number(),
	fom: z.string(),
	tom: z.string(),
	fagomraade: z.string(),
	beloep: z.string(),
	linjer: z.array(linjeSchema),
});

const beregningSchema = z.object({
	id: z.number(),
	gjelder: z.string(),
	utbetalesTil: z.string(),
	kidnummer: z.string().nullable(),
	status: z.string(),
	delberegninger: z.array(delberegningSchema),
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
