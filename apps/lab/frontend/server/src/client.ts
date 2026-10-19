import type { Request, Response } from "express";
import { logger } from "./logger.ts";
import { getOboToken } from "./token.ts";

export async function sendRequest(
    req: Request,
    res: Response,
    backendUrl: string,
    logMessage: string,
    contentType = "application/json",
) {
    const oboToken = await getOboToken(req);
    const path = new URL(backendUrl).pathname;
    const endpoint = path.split("/").pop();
    logger.info({ method: req.method, path }, logMessage);

    const response = await fetch(backendUrl, {
        method: req.method,
        headers: {
            Authorization: `Bearer ${oboToken}`,
            "Content-Type": contentType,
        },
        ...(req.method === "POST" && {
            body: contentType === "application/json"
                ? JSON.stringify(req.body)
                : req.body,
        }),
    });

    const level =
        response.status >= 500 ? "error" : response.status >= 400 ? "warn" : "info";
    logger[level](
        { method: req.method, path, status: response.status },
        `Svar fra backend: ${endpoint}`,
    );

    const responseData = await response.text();
    res.status(response.status);

    const responseContentType = response.headers.get("content-type");
    if (responseContentType) {
        res.type(responseContentType);
    }

    if (responseData) {
        res.send(responseData);
    } else {
        res.end();
    }
}
