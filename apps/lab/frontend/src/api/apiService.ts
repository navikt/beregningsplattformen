import type { BeregningRequest } from "../types/schema/BeregningRequest";
import {
	type OppdragstidslinjeDTO,
	oppdragstidslinjeSchema,
} from "../types/schema/OppdragstidslinjeDTO";
import { axiosPostFetcher } from "./config/apiConfig";

const BASE_URI = {
	API_V1: "/api/v1",
};

export async function postTolkOppdrag(
	request: BeregningRequest,
	signal?: AbortSignal,
): Promise<OppdragstidslinjeDTO> {
	const response = await axiosPostFetcher<BeregningRequest, unknown>(
		BASE_URI.API_V1,
		"/tolk-oppdrag",
		request,
		{
			signal,
			responseType: "json",
		},
	);
	return oppdragstidslinjeSchema.parse(response);
}
