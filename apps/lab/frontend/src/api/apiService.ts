import type { BeregningRequest } from "../types/BeregningRequest";
import {
	type BeregningResponse,
	beregningResponseSchema,
} from "../types/BeregningResponse";
import { axiosPostFetcher } from "./config/apiConfig";

const BASE_URI = {
	API_V1: "/api/v1",
};

export async function postTolkOppdrag(
	request: BeregningRequest,
	signal?: AbortSignal,
): Promise<BeregningResponse> {
	const response = await axiosPostFetcher<BeregningRequest, unknown>(
		BASE_URI.API_V1,
		"/tolk-oppdrag",
		request,
		{
			signal,
			responseType: "json",
		},
	);
	return beregningResponseSchema.parse(response);
}
