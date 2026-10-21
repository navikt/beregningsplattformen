import { z } from "zod";

export const kjedePerioderSchema = z.object({
	fraDato: z.iso.date(),
	tilDato: z.iso.date().nullable(),
	utbetalesTil: z.string(),
});

export const kjedeVersjonSchema = z.object({
	kjedePerioder: z.array(kjedePerioderSchema),
});

export const kjedeSchema = z.object({
	id: z.uuid(),
	kjedeVersjoner: z.array(kjedeVersjonSchema),
});

export const oppdragstidslinjeSchema = z.object({
	kjeder: z.array(kjedeSchema),
});

export type KjedePerioderDTO = z.infer<typeof kjedePerioderSchema>;
export type KjedeVersjonDTO = z.infer<typeof kjedeVersjonSchema>;
export type KjedeDTO = z.infer<typeof kjedeSchema>;
export type OppdragstidslinjeDTO = z.infer<typeof oppdragstidslinjeSchema>;
