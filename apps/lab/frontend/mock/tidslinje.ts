import { OppdragstidslinjeDTO } from "../src/types/OppdragstidslinjeDTO";

export const tidslinjeData: OppdragstidslinjeDTO = {
	"kjeder": [
		{
			"id": "11111111-1111-4111-8111-111111111111",
			"fagområde": "AAP",
			"fagsystemId": "TEST-FAGSYSTEM-001",
			"kjedeVersjoner": [
				{
					"kjedePerioder": [
						{
							"fraDato": "2024-01-01",
							"tilDato": "2024-01-14",
							"status": "AKTIV"
						}
					]
				}
			]
		},
		{
			"id": "22222222-2222-4222-8222-222222222222",
			"fagområde": "AAP",
			"fagsystemId": "TEST-FAGSYSTEM-002",
			"kjedeVersjoner": [
				{
					"kjedePerioder": [
						{
							"fraDato": "2024-01-01",
							"tilDato": "2024-01-31",
							"status": "AKTIV"
						},
						{
							"fraDato": "2023-12-01",
							"tilDato": "2023-12-31",
							"status": "AVSLUTTET"
						}
					]
				},
				{
					"kjedePerioder": [
						{
							"fraDato": "2023-12-01",
							"tilDato": "2023-12-31",
							"status": "AVSLUTTET"
						}
					]
				}
			]
		}
	]
}
