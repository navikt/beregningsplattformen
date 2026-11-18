import { HttpResponse, http } from "msw";
import { beregningstidslinjeData } from "./beregningstidslinje";
import { tidslinjeData } from "./tidslinje";

const API_V1 = "/api/v1";

export const handlers = [
	http.post(`${API_V1}/beregn`, () =>
		HttpResponse.json(
			{
				oppdragstidslinje: tidslinjeData,
				beregningstidslinje: beregningstidslinjeData,
			},
			{ status: 200 },
		),
	),
];
