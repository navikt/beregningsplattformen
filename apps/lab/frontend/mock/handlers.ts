import { HttpResponse, http } from "msw";
import { tidslinjeData } from "./tidslinje";

const API_V1 = "/api/v1";

export const handlers = [
	http.post(`${API_V1}/tolk-oppdrag`, () =>
		HttpResponse.json(tidslinjeData, { status: 200 }),
	),
];
