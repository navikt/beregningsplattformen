import { z } from "zod";

export const kjedePerioderSchema = z.object({
	fraDato: z.iso.date(),
	tilDato: z.iso.date().nullable(),
    status: z.string(),
}).catchall(z.string().nullable());

export const kjedeVersjonSchema = z.object({
	innsending: z.number().int(),
	kjedePerioder: z.array(kjedePerioderSchema),
});

export const kjedeSchema = z.object({
	id: z.uuid(),
    fagområde: z.string(),
    fagsystemId: z.string(),
	kjedeVersjoner: z.array(kjedeVersjonSchema),
});

export const oppdragstidslinjeSchema = z.object({
	kjeder: z.array(kjedeSchema),
});

export type KjedePerioderDTO = z.infer<typeof kjedePerioderSchema>;
export type KjedeVersjonDTO = z.infer<typeof kjedeVersjonSchema>;
export type KjedeDTO = z.infer<typeof kjedeSchema>;
export type OppdragstidslinjeDTO = z.infer<typeof oppdragstidslinjeSchema>;
