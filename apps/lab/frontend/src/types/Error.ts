export class ApiError extends Error {}

export class HttpStatusCodeError extends Error {
	constructor(
		readonly statusCode: number,
		readonly apiMessage?: string,
	) {
		super(apiMessage ?? `HTTP ${statusCode}`);
	}
}
