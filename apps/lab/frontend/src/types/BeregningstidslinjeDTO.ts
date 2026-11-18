import { z } from "zod";

export const linjetypeSchema = z.enum(["YTELSE", "SKATT", "TREKK"]);

export const linjeSchema = z.object({
	id: z.number(),
	type: linjetypeSchema,
	sats: z.string().nullable(),
	beloep: z.string(),
});

export const delberegningSchema = z.object({
	id: z.number(),
	fom: z.string(),
	tom: z.string(),
	linjer: z.array(linjeSchema),
	fagomraade: z.string(),
	beloep: z.string(),
});

export const beregningSchema = z.object({
	id: z.number(),
	gjelder: z.string(),
	utbetalesTil: z.string(),
	kidnummer: z.string().nullable(),
	status: z.string(),
	delberegninger: z.array(delberegningSchema),
});

export const beregningstidslinjeSchema = z.object({
	beregninger: z.array(beregningSchema),
});

export type LinjetypeDTO = z.infer<typeof linjetypeSchema>;
export type LinjeDTO = z.infer<typeof linjeSchema>;
export type DelberegningDTO = z.infer<typeof delberegningSchema>;
export type BeregningDTO = z.infer<typeof beregningSchema>;
export type BeregningstidslinjeDTO = z.infer<typeof beregningstidslinjeSchema>;
