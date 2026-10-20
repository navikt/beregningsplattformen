import path from "node:path";
import { collectDefaultMetrics, register } from "@prometheus-io/client";
import express, {
  type NextFunction,
  type Request,
  type Response,
} from "express";
import expressStaticGzip from "express-static-gzip";
import { sendRequest } from "./client.ts";
import { config } from "./config.ts";
import { logger } from "./logger.ts";
import { UnauthorizedError } from "./token.ts";

const BUILD_PATH = path.resolve(import.meta.dirname, "../../dist");
const HASHED_ASSETS_PATH = path.join(BUILD_PATH, "assets");
const ONE_YEAR_IN_SECONDS = 31_536_000;

collectDefaultMetrics();

const server = express();
server.disable("x-powered-by");

server.use(
    expressStaticGzip(BUILD_PATH, {
        enableBrotli: true,
        orderPreference: ["br"],
        serveStatic: {
            setHeaders: (response, filePath) => {
                response.setHeader(
                    "Cache-Control",
                    filePath.startsWith(HASHED_ASSETS_PATH)
                        ? `public, max-age=${ONE_YEAR_IN_SECONDS}, immutable`
                        : "no-cache",
                );
            },
        },
    }),
);

function asyncHandler(
    fn: (req: Request, res: Response, next: NextFunction) => Promise<unknown>,
) {
    return (req: Request, res: Response, next: NextFunction) => {
        Promise.resolve(fn(req, res, next)).catch(next);
    };
}

server.use(express.text({ type: "application/xml" }));

server.post("/api/v1/tolk-oppdrag", asyncHandler(async (request, response) => {
  const backendUrl = new URL(
    "/api/v1/tolk-oppdrag",
    config.backendUrl,
  ).toString();
  await sendRequest(
    request,
    response,
    backendUrl,
    "Videresender XML-forespørsel til backend",
    "application/xml;charset=utf-8",
  );
}));

const internalRouter = express.Router();

internalRouter.get("/isAlive", (_req: Request, res: Response) => {
    res.sendStatus(200);
});

internalRouter.get("/isReady", (_req: Request, res: Response) => {
    res.sendStatus(200);
});

internalRouter.get(
    "/metrics",
    asyncHandler(async (_req: Request, res: Response) => {
        res.set("Content-Type", register.contentType);
        res.send(await register.metrics());
    }),
);

server.use("/internal", internalRouter);

server.use((err: Error, req: Request, res: Response, _next: NextFunction) => {
    const logContext = { err, method: req.method, path: req.path };

    if (err instanceof UnauthorizedError) {
        logger.warn(logContext, "Ikke autentisert");
        res.status(401).json({ message: err.message });
        return;
    }

    logger.error(logContext, "Feil ved behandling av request");
    res.status(500).json({
        message: err.message,
        ...(!config.isProduction && { stack: err.stack }),
    });
});

server.listen(config.port, () =>
    logger.info(`Server listening on port ${config.port}`),
);
