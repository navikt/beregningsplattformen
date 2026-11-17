import { z } from "zod";
import { oppdragstidslinjeSchema } from "./OppdragstidslinjeDTO";

export const beregningResponseSchema = z.object({
	oppdragstidslinje: oppdragstidslinjeSchema,
});

export type BeregningResponse = z.infer<typeof beregningResponseSchema>;
