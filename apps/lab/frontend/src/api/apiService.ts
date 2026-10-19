import { axiosPostFetcher } from "./config/apiConfig";

const BASE_URI = {
	BACKEND_API: "/api",
};

export async function postTolkOppdrag(xml: string, signal?: AbortSignal) {
	return await axiosPostFetcher<string, string>(
		BASE_URI.BACKEND_API,
		"/lab",
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
