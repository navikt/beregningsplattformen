import { axiosPostFetcher } from "./config/apiConfig";

const BASE_URI = {
	API_V1: "/api/v1",
};

export async function postTolkOppdrag(xml: string, signal?: AbortSignal) {
	return await axiosPostFetcher<string, string>(
		BASE_URI.API_V1,
		"/tolk-oppdrag",
		xml,
		{
			signal,
			headers: {
				"Content-Type": "application/xml;charset=utf-8",
			},
			responseType: "text",
			transformResponse: [(data: string) => data],
		},
	);
}
