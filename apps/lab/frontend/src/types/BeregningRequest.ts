import { z } from "zod";
import { skattekortSchema } from "./Skattekort";
import { trekkSchema } from "./Trekk";

export const beregningRequestSchema = z.object({
	xml: z.array(z.string()),
	fom: z.iso.date().optional(),
	tom: z.iso.date().optional(),
	skattekort: skattekortSchema.optional(),
	trekk: z.array(trekkSchema).optional(),
});

export type BeregningRequest = z.infer<typeof beregningRequestSchema>;
