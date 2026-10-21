import { z } from "zod";

export const beregningRequestSchema = z.object({
	xml: z.array(z.string()),
});

export type BeregningRequest = z.infer<typeof beregningRequestSchema>;
