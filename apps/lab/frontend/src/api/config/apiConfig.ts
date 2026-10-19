import axios, {
	type AxiosInstance,
	type AxiosRequestConfig,
	type CreateAxiosDefaults,
} from "axios";
import { ApiError, HttpStatusCodeError } from "../../types/Error";

const config = (baseUri: string): CreateAxiosDefaults => ({
	baseURL: baseUri,
	timeout: 30000,
	withCredentials: true,
	headers: {
		Pragma: "no-cache",
		"Cache-Control": "no-cache",
		"Content-Type": "application/json",
	},
});

function createApi(baseUri: string): AxiosInstance {
	const instance = axios.create(config(baseUri));

	instance.interceptors.response.use(
		(response) => response,
		(error) => {
			const status: number | undefined = error.response?.status;
			if (status === 400) {
				throw new HttpStatusCodeError(status);
			}
			if (status === 401 || status === 403) {
				return Promise.reject(error);
			}
			throw new ApiError(
				`Issues with connection to backend${status ? ` (HTTP ${status})` : ""}`,
			);
		},
	);
	return instance;
}

const instances = new Map<string, AxiosInstance>();

function api(baseUri: string): AxiosInstance {
	let instance = instances.get(baseUri);
	if (!instance) {
		instance = createApi(baseUri);
		instances.set(baseUri, instance);
	}
	return instance;
}

export async function axiosFetcher<T>(baseUri: string, url: string) {
	const res = await api(baseUri).get<T>(url);
	return res.data;
}

export async function axiosPostFetcher<T, U>(
	baseUri: string,
	url: string,
	body?: T,
	requestConfig?: AxiosRequestConfig<T>,
) {
	const res = await api(baseUri).post<U>(url, body, requestConfig);
	return res.data;
}
