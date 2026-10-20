/** biome-ignore-all lint/suspicious/noConsole: false positive */
import { HttpResponse, http } from "msw";

const API_V1 = "/api/v1";

export const handlers = [
    http.post(
        `${API_V1}/tolk-oppdrag`,
        () => {
            const message =
                "Dette er en test";
            console.log(message);
            return HttpResponse.json(message, { status: 200 });
        },
    ),
];