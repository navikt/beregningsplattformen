import { OppdragstidslinjeDTO } from "../src/types/OppdragstidslinjeDTO";

export const tidslinjeData = {
	"kjeder": [
		{
			"id": "11111111-1111-4111-8111-111111111111",
			"fagområde": "AAP",
			"fagsystemId": "TEST-FAGSYSTEM-001",
			"kjedeVersjoner": [
				{
					"innsending": 1,
					"kjedePerioder": [
						{
							"fraDato": "2024-01-01",
							"tilDato": "2024-01-14",
							"delytelseId": "TEST-DELYTELSE-001",
							"status": { "saksbehandlerId": "SBH-001", "opplysning": "Aktiv" },
						},
					],
				},
			],
		},
		{
			"id": "22222222-2222-4222-8222-222222222222",
			"fagområde": "AAP",
			"fagsystemId": "TEST-FAGSYSTEM-002",
			"kjedeVersjoner": [
				{
					"innsending": 2,
					"kjedePerioder": [
						{
							"fraDato": "2024-01-01",
							"tilDato": "2024-01-31",
							"delytelseId": "TEST-DELYTELSE-002",
							"status": { "saksbehandlerId": "SBH-002", "opplysning": "Aktiv" },
						},
						{
							"fraDato": "2023-12-01",
							"tilDato": "2023-12-31",
							"delytelseId": "TEST-DELYTELSE-003",
							"status": { "saksbehandlerId": "SBH-003", "opplysning": "Opphørt" },
						},
					],
				},
				{
					"innsending": 1,
					"kjedePerioder": [
						{
							"fraDato": "2023-12-01",
							"tilDato": "2023-12-31",
							"delytelseId": "TEST-DELYTELSE-004",
							"status": { "saksbehandlerId": "SBH-004", "opplysning": "Opphørt" },
						},
					],
				},
			],
		},
	],
} satisfies OppdragstidslinjeDTO;
