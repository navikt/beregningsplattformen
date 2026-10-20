/** biome-ignore-all lint/suspicious/noConsole: false positive */
import { HttpResponse, http } from "msw";

export const handlers = [
    http.post(
        "/lab-backend-api/api/v1/tolkOppdrag",
        () => {
            const message =
                "Dette er en test";
            console.log(message);
            return HttpResponse.json(message, { status: 200 });
        },
    ),
];