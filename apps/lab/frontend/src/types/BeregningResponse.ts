import { z } from "zod";
import { beregningstidslinjeSchema } from "./BeregningstidslinjeDTO";
import { oppdragstidslinjeSchema } from "./OppdragstidslinjeDTO";

export const beregningResponseSchema = z.object({
	oppdragstidslinje: oppdragstidslinjeSchema,
	beregningstidslinje: beregningstidslinjeSchema,
});

export type BeregningResponse = z.infer<typeof beregningResponseSchema>;
