import { z } from "zod";

export const opplysningSchema = z.object({
	saksbehandlerId: z.string(),
	opplysning: z.string(),
});

export const kjedePerioderSchema = z
	.object({
		fraDato: z.iso.date(),
		tilDato: z.iso.date().nullable(),
		delytelseId: z.string(),
		status: opplysningSchema,
	})
	.catchall(z.union([opplysningSchema, z.string()]).nullable());

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
