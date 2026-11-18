import type { BeregningstidslinjeDTO } from "../src/types/BeregningstidslinjeDTO";

export const beregningstidslinjeData = {
	beregninger: [
		{
			id: 1,
			gjelder: "11111111111",
			utbetalesTil: "11111111111",
			kidnummer: null,
			status: "BEREGNET",
			delberegninger: [
				{
					id: 1,
					fom: "2024-01-01",
					tom: "2024-01-14",
					linjer: [
						{ id: 1, type: "YTELSE", sats: "500 DAG", beloep: "5000" },
						{ id: 2, type: "SKATT", sats: "50 PROSENT", beloep: "-2500" },
					],
					fagomraade: "AAP",
					beloep: "2500",
				},
				{
					id: 2,
					fom: "2024-01-15",
					tom: "2024-01-31",
					linjer: [
						{ id: 1, type: "YTELSE", sats: "300 DAG", beloep: "3900" },
						{ id: 2, type: "SKATT", sats: "50 PROSENT", beloep: "-1950" },
						{ id: 3, type: "TREKK", sats: "500 LOP", beloep: "-500" },
					],
					fagomraade: "AAP",
					beloep: "1450",
				},
			],
		},
	],
} satisfies BeregningstidslinjeDTO;
