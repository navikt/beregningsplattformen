import type { BeregningRequest } from "../types/BeregningRequest";
import {
	type BeregningResponse,
	beregningResponseSchema,
} from "../types/BeregningResponse";
import { axiosPostFetcher } from "./config/apiConfig";

const BASE_URI = {
	API_V1: "/api/v1",
};

export async function postBeregn(
	request: BeregningRequest,
	signal?: AbortSignal,
): Promise<BeregningResponse> {
	const response = await axiosPostFetcher<BeregningRequest, unknown>(
		BASE_URI.API_V1,
		"/beregn",
		request,
		{
			signal,
			responseType: "json",
		},
	);
	return beregningResponseSchema.parse(response);
}
